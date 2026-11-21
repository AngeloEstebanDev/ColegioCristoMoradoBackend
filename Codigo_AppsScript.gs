/**
 * Envío de códigos de verificación — I.E. 2092 Cristo Morado
 * Pega este código en https://script.google.com con el Gmail que enviará los correos.
 * 1. Cambia SECRETO por una clave larga inventada (la misma va en Render: MAIL_RELAY_SECRET).
 * 2. Implementar > Nueva implementación > Aplicación web
 *    Ejecutar como: Yo  ·  Quién tiene acceso: Cualquier usuario
 * 3. Copia la URL que termina en /exec y ponla en Render: MAIL_RELAY_URL (y en local.properties para usarlo local).
 * 4. Si cambias este código o el SECRETO: Implementar > Gestionar implementaciones > (lápiz) Editar >
 *    Versión: Nueva versión > Implementar. Así la URL /exec sigue siendo la misma y usa el código nuevo.
 * Prueba rápida: abre la URL /exec en el navegador; debe mostrar {"ok":true,"servicio":"relay Cristo Morado"}.
 */
const SECRETO = 'CAMBIA-ESTO-por-una-clave-larga-inventada';

/**
 * Se ejecuta cuando el backend (Render) hace un POST a la URL /exec de este script.
 * Recibe { secreto, para, asunto, html, nombre }, comprueba que el secreto coincida con MAIL_RELAY_SECRET
 * y envía el correo desde este Gmail con MailApp. Responde { ok: true } o { ok: false, error }.
 */
function doPost(e) {
  try {
    const d = JSON.parse(e.postData.contents);
    if (String(d.secreto || '').trim() !== SECRETO.trim()) return salida({ ok: false, error: 'no autorizado' });
    if (!d.para || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.para)) return salida({ ok: false, error: 'correo inválido' });
    MailApp.sendEmail({
      to: d.para,
      subject: String(d.asunto || 'Código de verificación').slice(0, 200),
      htmlBody: String(d.html || ''),
      name: d.nombre || 'I.E. 2092 Cristo Morado',
    });
    return salida({ ok: true });
  } catch (err) {
    return salida({ ok: false, error: String(err) });
  }
}

/**
 * Se ejecuta al abrir la URL /exec en el navegador. Sirve para comprobar que la implementación es pública
 * y está activa (no envía correos ni muestra el secreto).
 */
function doGet() {
  return salida({ ok: true, servicio: 'relay Cristo Morado' });
}

/** Ejecútala una vez desde el editor para dar permiso de enviar correos (y probar). */
function probar() {
  MailApp.sendEmail(Session.getActiveUser().getEmail(), 'Prueba Cristo Morado', 'Si te llegó, el script funciona.');
}

/** Convierte el objeto en texto JSON y lo devuelve como respuesta al backend. */
function salida(o) {
  return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);
}
