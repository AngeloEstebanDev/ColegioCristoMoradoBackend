package com.CristoMorado_api.CristoMorado_api.config;

import com.CristoMorado_api.CristoMorado_api.services.ActividadLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActividadInterceptor implements HandlerInterceptor {

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
        else return;

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
