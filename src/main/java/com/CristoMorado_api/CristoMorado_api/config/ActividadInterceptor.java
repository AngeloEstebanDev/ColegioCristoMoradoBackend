package com.CristoMorado_api.CristoMorado_api.config;

import com.CristoMorado_api.CristoMorado_api.services.ActividadLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Registra en "Actividad reciente" cada acción de guardar, editar o eliminar que termina bien
 * (y los intentos sin permiso). El usuario sale de la cabecera X-Usuario-Id que agrega el frontend.
 * El inicio de sesión lo marca AuthController con atributos de la petición.
 */
@Component
public class ActividadInterceptor implements HandlerInterceptor {

    /** Atributos que deja AuthController al iniciar sesión. */
    public static final String ATR_USUARIO_ID = "actividad.usuarioId";
    public static final String ATR_NOMBRE = "actividad.nombre";
    public static final String ATR_ACCION = "actividad.accion";
    public static final String ATR_ESTADO = "actividad.estado";

    private final ActividadLogService actividad;

    public ActividadInterceptor(ActividadLogService actividad) {
        this.actividad = actividad;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String metodo = request.getMethod();
        if ("GET".equalsIgnoreCase(metodo) || "OPTIONS".equalsIgnoreCase(metodo) || "HEAD".equalsIgnoreCase(metodo)) return;

        // Inicio de sesión (lo marca AuthController).
        Object accionLogin = request.getAttribute(ATR_ACCION);
        if (accionLogin != null) {
            actividad.registrar((Long) request.getAttribute(ATR_USUARIO_ID), (String) request.getAttribute(ATR_NOMBRE),
                    "Acceso", accionLogin.toString(), String.valueOf(request.getAttribute(ATR_ESTADO)));
            return;
        }

        String[] desc = ActividadLogService.describir(metodo, request.getRequestURI());
        if (desc == null) return;
        int status = response.getStatus();
        String estado;
        String accion = desc[1];
        if (status >= 200 && status < 300 && ex == null) estado = "Exitoso";
        else if (status == 403) { estado = "Advertencia"; accion = "Sin permiso: " + accion.substring(0, 1).toLowerCase() + accion.substring(1); }
        else if (status >= 500 || ex != null) estado = "Error";
        else return;   // datos inválidos (400, 404, 409): el usuario ya ve el mensaje y lo corrige

        actividad.registrar(usuarioId(request), null, desc[0], accion, estado);
    }

    private static Long usuarioId(HttpServletRequest request) {
        String v = request.getHeader("X-Usuario-Id");
        try {
            return v == null || v.isBlank() ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
