package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.util.Soporte;
import com.CristoMorado_api.CristoMorado_api.dto.CodigoEnviadoResponse;
import com.CristoMorado_api.CristoMorado_api.entity.CodigoVerificacion;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.CredencialesInvalidasException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.CodigoVerificacionRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Verificación por Gmail con códigos de 6 dígitos:
 * 1) Vincular el Gmail propio (desde "Mi perfil").
 * 2) Cambiar la contraseña (desde "Configuración").
 * 3) Recuperar la contraseña (desde el login).
 */
@Service
public class VerificacionService {

    static final int MINUTOS_VALIDEZ = 10;
    static final int SEGUNDOS_REENVIO = 60;
    static final int MAX_INTENTOS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CodigoVerificacionRepository codigoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorreoService correoService;
    private final boolean soloGmail;

    public VerificacionService(CodigoVerificacionRepository codigoRepository,
                               UsuarioRepository usuarioRepository,
                               CorreoService correoService,
                               @Value("${app.correo.solo-gmail:true}") boolean soloGmail) {
        this.codigoRepository = codigoRepository;
        this.usuarioRepository = usuarioRepository;
        this.correoService = correoService;
        this.soloGmail = soloGmail;
    }

    // ------------------------------------------------------------------
    // 1) Vincular Gmail
    // ------------------------------------------------------------------

    @Transactional
    public CodigoEnviadoResponse enviarCodigoVinculacion(Long usuarioId, String correo) {
        Usuario u = buscarUsuario(usuarioId);
        String gmail = validarCorreo(correo);
        if (gmail.equalsIgnoreCase(u.getCorreo()) && Boolean.TRUE.equals(u.getCorreoVerificado())) {
            throw new DatosInvalidosException("Ese Gmail ya está vinculado y verificado en tu cuenta.");
        }
        verificarQueNoLoUseOtro(gmail, usuarioId);
        return generarYEnviar(u, CodigoVerificacion.VINCULAR_CORREO, gmail, "verificar tu Gmail");
    }

    /** Si el código es correcto, el Gmail queda guardado y verificado en la cuenta. */
    // noRollbackFor: si el código es incorrecto, el intento fallido sí se guarda.
    @Transactional(noRollbackFor = DatosInvalidosException.class)
    public Usuario confirmarVinculacion(Long usuarioId, String codigo) {
        Usuario u = buscarUsuario(usuarioId);
        CodigoVerificacion c = validarCodigo(usuarioId, CodigoVerificacion.VINCULAR_CORREO, codigo);
        verificarQueNoLoUseOtro(c.getCorreo(), usuarioId);
        u.setCorreo(c.getCorreo());
        u.setCorreoVerificado(true);
        return usuarioRepository.save(u);
    }

    // ------------------------------------------------------------------
    // 2) Cambiar contraseña (usuario con sesión iniciada)
    // ------------------------------------------------------------------

    @Transactional
    public CodigoEnviadoResponse enviarCodigoCambioPassword(Long usuarioId) {
        Usuario u = buscarUsuario(usuarioId);
        exigirGmailVerificado(u, "Primero vincula y verifica tu Gmail en \"Mi perfil\".");
        return generarYEnviar(u, CodigoVerificacion.CAMBIO_PASSWORD, u.getCorreo(), "cambiar tu contraseña");
    }

    // noRollbackFor: si el código es incorrecto, el intento fallido sí se guarda.
    @Transactional(noRollbackFor = DatosInvalidosException.class)
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva, String codigo) {
        Usuario u = buscarUsuario(usuarioId);
        exigirGmailVerificado(u, "Primero vincula y verifica tu Gmail en \"Mi perfil\".");
        if (passwordActual == null || !passwordActual.equals(u.getPassword())) {
            throw new CredencialesInvalidasException("La contraseña actual no es correcta.");
        }
        CuentaService.validarPassword(passwordNueva);
        validarCodigo(usuarioId, CodigoVerificacion.CAMBIO_PASSWORD, codigo);
        u.setPassword(passwordNueva);
        usuarioRepository.save(u);
    }

    // ------------------------------------------------------------------
    // 3) Recuperar contraseña (desde el login, sin sesión)
    // ------------------------------------------------------------------

    @Transactional
    public CodigoEnviadoResponse solicitarRecuperacion(String nombreUsuario) {
        Usuario u = buscarPorNombreUsuario(nombreUsuario);
        if (Boolean.FALSE.equals(u.getEstado())) {
            throw new DatosInvalidosException("Tu cuenta está inactiva. Para activarla, " + Soporte.ESCRIBE_A_SOPORTE + ".");
        }
        exigirGmailVerificado(u, "Tu cuenta no tiene un Gmail verificado. Para restablecer tu contraseña, "
                + Soporte.ESCRIBE_A_SOPORTE + ". Luego vincula tu Gmail en \"Mi perfil\".");
        return generarYEnviar(u, CodigoVerificacion.RECUPERAR_PASSWORD, u.getCorreo(), "restablecer tu contraseña");
    }

    // noRollbackFor: si el código es incorrecto, el intento fallido sí se guarda.
    @Transactional(noRollbackFor = DatosInvalidosException.class)
    public void restablecerPassword(String nombreUsuario, String codigo, String passwordNueva) {
        Usuario u = buscarPorNombreUsuario(nombreUsuario);
        CuentaService.validarPassword(passwordNueva);
        validarCodigo(u.getId(), CodigoVerificacion.RECUPERAR_PASSWORD, codigo);
        u.setPassword(passwordNueva);
        usuarioRepository.save(u);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private CodigoEnviadoResponse generarYEnviar(Usuario u, String tipo, String correo, String motivo) {
        // No más de un código por minuto (evita spam al correo).
        codigoRepository.findTopByUsuarioIdAndTipoOrderByFechaCreacionDesc(u.getId(), tipo).ifPresent(ultimo -> {
            long seg = Duration.between(ultimo.getFechaCreacion(), LocalDateTime.now()).getSeconds();
            if (seg < SEGUNDOS_REENVIO) {
                throw new DatosInvalidosException("Espera " + (SEGUNDOS_REENVIO - seg)
                        + " segundos para pedir un código nuevo.");
            }
        });
        codigoRepository.invalidarPendientes(u.getId(), tipo);

        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        codigoRepository.save(CodigoVerificacion.builder()
                .usuarioId(u.getId())
                .tipo(tipo)
                .correo(correo)
                .codigoHash(hash(codigo, u.getId()))
                .expiraEn(LocalDateTime.now().plusMinutes(MINUTOS_VALIDEZ))
                .fechaCreacion(LocalDateTime.now())
                .build());

        correoService.enviarCodigo(correo, u.getNombre(), codigo, motivo, MINUTOS_VALIDEZ);
        return new CodigoEnviadoResponse(enmascarar(correo), MINUTOS_VALIDEZ, SEGUNDOS_REENVIO);
    }

    private CodigoVerificacion validarCodigo(Long usuarioId, String tipo, String codigo) {
        String limpio = codigo == null ? "" : codigo.replaceAll("\\s", "");
        if (!limpio.matches("\\d{6}")) {
            throw new DatosInvalidosException("Escribe el código de 6 dígitos que te llegó al Gmail.");
        }
        CodigoVerificacion c = codigoRepository
                .findTopByUsuarioIdAndTipoAndUsadoFalseOrderByFechaCreacionDesc(usuarioId, tipo)
                .orElseThrow(() -> new DatosInvalidosException("No hay un código pendiente. Pide uno nuevo."));

        if (c.getExpiraEn().isBefore(LocalDateTime.now())) {
            c.setUsado(true);
            codigoRepository.save(c);
            throw new DatosInvalidosException("El código venció. Pide uno nuevo.");
        }
        if (!hash(limpio, usuarioId).equals(c.getCodigoHash())) {
            c.setIntentos(c.getIntentos() + 1);
            if (c.getIntentos() >= MAX_INTENTOS) {
                c.setUsado(true);
                codigoRepository.save(c);
                throw new DatosInvalidosException("Demasiados intentos fallidos. Pide un código nuevo.");
            }
            codigoRepository.save(c);
            throw new DatosInvalidosException("Código incorrecto. Te quedan "
                    + (MAX_INTENTOS - c.getIntentos()) + " intentos.");
        }
        c.setUsado(true);
        return codigoRepository.save(c);
    }

    private String validarCorreo(String correo) {
        String c = correo == null ? "" : correo.trim().toLowerCase();
        if (!c.matches("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}")) {
            throw new DatosInvalidosException("Escribe un correo válido.");
        }
        if (soloGmail && !c.endsWith("@gmail.com")) {
            throw new DatosInvalidosException("Debe ser una cuenta de Gmail (termina en @gmail.com).");
        }
        return c;
    }

    private void verificarQueNoLoUseOtro(String correo, Long usuarioId) {
        usuarioRepository.findByCorreoIgnoreCase(correo).ifPresent(otro -> {
            if (!otro.getId().equals(usuarioId)) {
                throw new RecursoDuplicadoException("Ese Gmail ya está vinculado a otra cuenta.");
            }
        });
    }

    private static void exigirGmailVerificado(Usuario u, String mensaje) {
        if (u.getCorreo() == null || !Boolean.TRUE.equals(u.getCorreoVerificado())) {
            throw new DatosInvalidosException(mensaje);
        }
    }

    /** "lucas.premium@gmail.com" -> "lu*********um@gmail.com" */
    static String enmascarar(String correo) {
        int arroba = correo.indexOf('@');
        if (arroba <= 0) return correo;
        String nombre = correo.substring(0, arroba);
        String visible = nombre.length() <= 4 ? nombre.substring(0, 1) : nombre.substring(0, 2);
        String fin = nombre.length() <= 4 ? "" : nombre.substring(nombre.length() - 2);
        return visible + "*".repeat(Math.max(3, nombre.length() - visible.length() - fin.length())) + fin
                + correo.substring(arroba);
    }

    private static String hash(String codigo, Long usuarioId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest((codigo + ":" + usuarioId).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Usuario buscarUsuario(Long id) {
        if (id == null) throw new DatosInvalidosException("Falta el usuario.");
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }

    private Usuario buscarPorNombreUsuario(String usuario) {
        String u = usuario == null ? "" : usuario.trim();
        if (u.isEmpty()) throw new DatosInvalidosException("Escribe tu usuario.");
        return usuarioRepository.findByUsuarioIgnoreCase(u)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }
}
