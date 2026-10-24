package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.util.Soporte;
import com.CristoMorado_api.CristoMorado_api.dto.CodigoEnviadoResponse;
import com.CristoMorado_api.CristoMorado_api.dto.UsuarioResponse;
import com.CristoMorado_api.CristoMorado_api.dto.VerificacionRequests.RecuperarRequest;
import com.CristoMorado_api.CristoMorado_api.dto.VerificacionRequests.RestablecerRequest;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.services.PermisoService;
import com.CristoMorado_api.CristoMorado_api.services.VerificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import com.CristoMorado_api.CristoMorado_api.config.ActividadInterceptor;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.cors.origenes}")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final VerificacionService verificacionService;
    private final PermisoService permisoService;

    public AuthController(UsuarioRepository usuarioRepository, VerificacionService verificacionService,
                          PermisoService permisoService) {
        this.usuarioRepository = usuarioRepository;
        this.verificacionService = verificacionService;
        this.permisoService = permisoService;
    }

    /**
     * Inicio de sesión con NOMBRE DE USUARIO (ALU/DOC/APO/DIR + DNI) y contraseña.
     * En caso de error responde {mensaje, campo} para que el login marque el
     * campo correcto ("usuario" o "password").
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String nombre = request.usuario() == null ? "" : request.usuario().trim();
        if (nombre.isEmpty() || request.password() == null || request.password().isEmpty()) {
            return error(400, "Ingresa tu usuario y contraseña.", null);
        }

        Optional<Usuario> encontrado = usuarioRepository.findByUsuarioIgnoreCase(nombre);
        if (encontrado.isEmpty()) {
            marcarActividad(null, nombre, "Intento de inicio de sesión con un usuario que no existe", "Advertencia");
            return error(401, "El usuario no existe.", "usuario");
        }

        Usuario usuario = encontrado.get();
        if (!usuario.getPassword().equals(request.password())) {
            marcarActividad(usuario.getId(), null, "Intento de inicio de sesión con contraseña incorrecta", "Advertencia");
            return error(401, "Contraseña incorrecta.", "password");
        }
        if (Boolean.FALSE.equals(usuario.getEstado())) {
            marcarActividad(usuario.getId(), null, "Intento de inicio de sesión con la cuenta inactiva", "Advertencia");
            return error(403, "Tu cuenta está inactiva. Para activarla, " + Soporte.ESCRIBE_A_SOPORTE + ".", null);
        }
        marcarActividad(usuario.getId(), null, "Inició sesión", "Exitoso");
        // Se devuelve sin la contraseña y con sus permisos del panel.
        return ResponseEntity.ok(UsuarioResponse.conPermisos(usuario, null, permisoService.permisosDe(usuario)));
    }

    /** "¿Olvidaste tu contraseña?" paso 1: envía un código al Gmail verificado. */
    @PostMapping("/recuperar/codigo")
    public CodigoEnviadoResponse solicitarRecuperacion(@RequestBody RecuperarRequest request) {
        return verificacionService.solicitarRecuperacion(request.usuario());
    }

    /** "¿Olvidaste tu contraseña?" paso 2: código + nueva contraseña. */
    @PostMapping("/recuperar/restablecer")
    public ResponseEntity<Void> restablecer(@RequestBody RestablecerRequest request) {
        verificacionService.restablecerPassword(request.usuario(), request.codigo(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }

    /** Deja los datos del inicio de sesión para "Actividad reciente" (los guarda ActividadInterceptor). */
    private static void marcarActividad(Long usuarioId, String nombreEscrito, String accion, String estado) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;   // fuera de una petición web (p. ej. pruebas unitarias)
        if (usuarioId != null) attrs.setAttribute(ActividadInterceptor.ATR_USUARIO_ID, usuarioId, RequestAttributes.SCOPE_REQUEST);
        if (nombreEscrito != null) attrs.setAttribute(ActividadInterceptor.ATR_NOMBRE, nombreEscrito, RequestAttributes.SCOPE_REQUEST);
        attrs.setAttribute(ActividadInterceptor.ATR_ACCION, accion, RequestAttributes.SCOPE_REQUEST);
        attrs.setAttribute(ActividadInterceptor.ATR_ESTADO, estado, RequestAttributes.SCOPE_REQUEST);
    }

    private static ResponseEntity<Map<String, String>> error(int status, String mensaje, String campo) {
        return ResponseEntity.status(status).body(campo == null
                ? Map.of("mensaje", mensaje)
                : Map.of("mensaje", mensaje, "campo", campo));
    }

    public record LoginRequest(String usuario, String password) {}
}
