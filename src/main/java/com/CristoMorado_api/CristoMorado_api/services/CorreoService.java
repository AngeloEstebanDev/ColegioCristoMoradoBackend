package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.util.Soporte;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Envía los códigos de verificación al Gmail del usuario.
 *
 * Tres formas, en este orden (application.properties):
 *  1. MAIL_RELAY_URL definida: Google Apps Script que envía desde tu Gmail por HTTPS
 *     (sirve en Render gratis, que bloquea SMTP). Ver GUIA_DESPLIEGUE.md.
 *  2. BREVO_API_KEY definida: API HTTPS de Brevo.
 *  3. Si no: Gmail por SMTP (spring.mail.*, variables MAIL_USERNAME y MAIL_PASSWORD).
 * El código solo se envía al correo: no se muestra en pantalla.
 *
 * Modo consola (solo para tu computadora): si en local.properties pones MAIL_MODO_CONSOLA=true y el
 * correo no está configurado o no se pudo enviar, el código se escribe en la consola del backend para
 * poder seguir con la demostración. En Render nunca se define, así que allí no se usa.
 *
 * Si algo falla, la consola (o los "Logs" de Render) muestra una línea que empieza con [CORREO]
 * explicando la causa exacta (secreto distinto, URL equivocada, script sin permiso, sin internet...).
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;
    private final String brevoApiKey;
    private final String relayUrl;
    private final String relaySecreto;
    private final boolean modoConsola;
    // Apps Script responde con una redirección (302): se sigue automáticamente.
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public CorreoService(ObjectProvider<JavaMailSender> mailSender,
                         @Value("${app.correo.remitente:${spring.mail.username:}}") String remitente,
                         @Value("${app.correo.brevo-api-key:}") String brevoApiKey,
                         @Value("${app.correo.relay-url:}") String relayUrl,
                         @Value("${app.correo.relay-secreto:}") String relaySecreto,
                         @Value("${app.correo.modo-consola:false}") boolean modoConsola) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.relayUrl = relayUrl == null ? "" : relayUrl.trim();
        this.relaySecreto = relaySecreto == null ? "" : relaySecreto.trim();
        this.modoConsola = modoConsola;
        log.info("[CORREO] Método de envío: {}", descripcion());
        if (!this.relayUrl.isEmpty() && this.relaySecreto.isEmpty()) {
            log.warn("[CORREO] MAIL_RELAY_URL está definida pero MAIL_RELAY_SECRET está vacía: el Apps Script responderá \"no autorizado\".");
        }
        if (!this.relayUrl.isEmpty() && !this.relayUrl.endsWith("/exec")) {
            log.warn("[CORREO] MAIL_RELAY_URL debería terminar en /exec (la URL de la implementación, no la del editor).");
        }
    }

    /** Qué forma de envío se usará: APPS_SCRIPT, BREVO, SMTP, CONSOLA o NINGUNO (sin mostrar claves). */
    public String metodo() {
        if (!relayUrl.isEmpty()) return "APPS_SCRIPT";
        if (!brevoApiKey.isEmpty()) return "BREVO";
        if (mailSender.getIfAvailable() != null && remitente != null && !remitente.isBlank()) return "SMTP";
        return modoConsola ? "CONSOLA" : "NINGUNO";
    }

    /** Si está activo el modo consola (solo local). */
    public boolean isModoConsola() {
        return modoConsola;
    }

    /** Texto para la consola al arrancar: dice qué método se usará y qué falta. */
    private String descripcion() {
        return switch (metodo()) {
            case "APPS_SCRIPT" -> "Apps Script (MAIL_RELAY_URL)" + (modoConsola ? " + modo consola si falla" : "");
            case "BREVO" -> "Brevo (BREVO_API_KEY)" + (modoConsola ? " + modo consola si falla" : "");
            case "SMTP" -> "Gmail SMTP (" + remitente + ")" + (modoConsola ? " + modo consola si falla" : "");
            case "CONSOLA" -> "MODO CONSOLA: los códigos se mostrarán aquí, no se envían correos (solo para pruebas locales)";
            default -> "NINGUNO. Vincular Gmail y cambiar contraseña darán error. En local agrega MAIL_RELAY_URL y "
                    + "MAIL_RELAY_SECRET (los mismos de Render) a local.properties, o MAIL_MODO_CONSOLA=true.";
        };
    }

    /**
     * Envía el código al Gmail. El código nunca se muestra en pantalla; solo se escribe en la consola
     * si el modo consola (local) está activo y el correo no se pudo enviar.
     */
    public void enviarCodigo(String para, String nombre, String codigo, String motivo, int minutosValidez) {
        try {
            enviarCodigoPorCorreo(para, nombre, codigo, motivo, minutosValidez);
        } catch (DatosInvalidosException e) {
            if (!modoConsola) throw e;
            log.warn("""

                    ============================================================
                    [CORREO] MODO CONSOLA (solo local): el correo NO se envió.
                      Para: {}   Motivo: {}
                      CÓDIGO: {}   (vence en {} minutos)
                    ============================================================""", para, motivo, codigo, minutosValidez);
        }
    }

    /** Intenta el envío real con el método configurado (Apps Script, Brevo o SMTP). */
    private void enviarCodigoPorCorreo(String para, String nombre, String codigo, String motivo, int minutosValidez) {
        if (!relayUrl.isEmpty()) {
            enviarPorAppsScript(para, "Tu código de verificación: " + codigo, html(nombre, codigo, motivo, minutosValidez));
            return;
        }
        if (!brevoApiKey.isEmpty()) {
            enviarPorBrevo(para, nombre, "Tu código de verificación: " + codigo, html(nombre, codigo, motivo, minutosValidez));
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || remitente == null || remitente.isBlank()) {
            log.error("[CORREO] No hay ningún método de envío configurado (MAIL_RELAY_URL, BREVO_API_KEY o MAIL_USERNAME/MAIL_PASSWORD).");
            throw new DatosInvalidosException("El envío de correos no está configurado. Por favor, " + Soporte.ESCRIBE_A_SOPORTE + ".");
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente, "I.E. 2092 Cristo Morado");
            helper.setTo(para);
            helper.setSubject("Tu código de verificación: " + codigo);
            helper.setText(html(nombre, codigo, motivo, minutosValidez), true);
            sender.send(mensaje);
        } catch (Exception e) {
            log.error("[CORREO] Gmail SMTP falló al enviar a {}: {} (revisa MAIL_USERNAME y que MAIL_PASSWORD sea una "
                    + "contraseña de aplicación de 16 letras; en Render el SMTP está bloqueado, usa MAIL_RELAY_URL)", para, e.getMessage());
            throw new DatosInvalidosException("No se pudo enviar el correo. Revisa el Gmail o intenta más tarde.");
        }
    }

    /** Envío desde el Gmail del colegio con un Google Apps Script publicado como aplicación web. */
    private void enviarPorAppsScript(String para, String asunto, String html) {
        String json = "{\"secreto\":" + jsonTexto(relaySecreto) + ",\"para\":" + jsonTexto(para)
                + ",\"asunto\":" + jsonTexto(asunto) + ",\"html\":" + jsonTexto(html)
                + ",\"nombre\":\"I.E. 2092 Cristo Morado\"}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(relayUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            String body = res.body() == null ? "" : res.body();
            if (res.statusCode() / 100 != 2 || !body.replace(" ", "").contains("\"ok\":true")) {
                log.error("[CORREO] Apps Script respondió {} al enviar a {}. Causa probable: {}. Respuesta: {}",
                        res.statusCode(), para, causaAppsScript(res.statusCode(), body),
                        body.length() > 300 ? body.substring(0, 300) : body);
                throw new DatosInvalidosException("No se pudo enviar el correo. Revisa el Gmail o intenta más tarde.");
            }
        } catch (DatosInvalidosException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("[CORREO] No se pudo conectar con el Apps Script para enviar a {}: {} (¿hay internet? ¿la "
                    + "MAIL_RELAY_URL está bien copiada?)", para, e.toString());
            throw new DatosInvalidosException("No se pudo enviar el correo. Revisa el Gmail o intenta más tarde.");
        }
    }

    /** Traduce la respuesta del Apps Script a una causa entendible para la consola / logs de Render. */
    static String causaAppsScript(int status, String body) {
        String b = body == null ? "" : body;
        if (b.contains("no autorizado")) {
            return "el SECRETO del Apps Script no es igual a MAIL_RELAY_SECRET (o cambiaste el SECRETO y no "
                    + "publicaste una versión nueva: Implementar > Gestionar implementaciones > Editar > Nueva versión)";
        }
        if (b.contains("correo inválido")) return "la dirección de destino no es válida";
        if (b.contains("too many times") || b.contains("demasiadas veces")) {
            return "se alcanzó el límite diario de correos de Gmail (MailApp); intenta mañana";
        }
        if (b.contains("\"ok\":false") || b.contains("\"ok\": false")) {
            return "el script falló al enviar (¿falta autorizarlo? En el editor ejecuta la función probar() y acepta los permisos)";
        }
        if (status == 404) return "la URL no existe: copia otra vez la URL /exec de la implementación activa";
        if (status == 401 || status == 403 || b.contains("<html") || b.contains("<!DOCTYPE")) {
            return "el script no es público o pide iniciar sesión: en la implementación pon 'Ejecutar como: Yo' y "
                    + "'Quién tiene acceso: Cualquier usuario', y usa la URL que termina en /exec";
        }
        return "respuesta inesperada del script";
    }

    /** Envío por la API HTTPS de Brevo (https://developers.brevo.com/reference/sendtransacemail). */
    private void enviarPorBrevo(String para, String nombre, String asunto, String html) {
        if (remitente == null || remitente.isBlank()) {
            throw new DatosInvalidosException("El envío de correos no está configurado. Por favor, " + Soporte.ESCRIBE_A_SOPORTE + ".");
        }
        String json = "{\"sender\":{\"name\":\"I.E. 2092 Cristo Morado\",\"email\":" + jsonTexto(remitente) + "},"
                + "\"to\":[{\"email\":" + jsonTexto(para) + ",\"name\":" + jsonTexto(nombre == null || nombre.isBlank() ? para : nombre) + "}],"
                + "\"subject\":" + jsonTexto(asunto) + ",\"htmlContent\":" + jsonTexto(html) + "}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", brevoApiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.error("[CORREO] Brevo respondió {} al enviar a {}: {}", res.statusCode(), para, res.body());
                throw new DatosInvalidosException("No se pudo enviar el correo. Revisa el Gmail o intenta más tarde.");
            }
        } catch (DatosInvalidosException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("[CORREO] No se pudo enviar el correo a {} por Brevo: {}", para, e.toString());
            throw new DatosInvalidosException("No se pudo enviar el correo. Revisa el Gmail o intenta más tarde.");
        }
    }

    /** Texto como literal JSON (con comillas y caracteres escapados). */
    static String jsonTexto(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : (s == null ? "" : s).toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }

    /**
     * Devuelve html (como texto).
     */
    private static String html(String nombre, String codigo, String motivo, int minutos) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;border:1px solid #E3D5C4;border-radius:12px;overflow:hidden">
                  <div style="background:#2E1233;color:#E4C766;padding:18px 24px;font-weight:bold;letter-spacing:.05em">
                    I.E. N.º 2092 · CRISTO MORADO
                  </div>
                  <div style="padding:24px;color:#241521">
                    <p>Hola %s,</p>
                    <p>Usa este código para <b>%s</b>:</p>
                    <p style="font-size:32px;font-weight:bold;letter-spacing:8px;color:#4A1F52;text-align:center;margin:24px 0">%s</p>
                    <p style="color:#6B5A67;font-size:13px">El código vence en %d minutos. Si no lo pediste tú, ignora este correo.</p>
                  </div>
                </div>
                """.formatted(escapar(nombre), escapar(motivo), codigo, minutos);
    }

    /**
     * Devuelve escapar (como texto).
     */
    private static String escapar(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
