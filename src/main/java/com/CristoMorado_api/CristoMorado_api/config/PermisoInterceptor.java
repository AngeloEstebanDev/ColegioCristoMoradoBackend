package com.CristoMorado_api.CristoMorado_api.config;

import com.CristoMorado_api.CristoMorado_api.services.PermisoService;
import com.CristoMorado_api.CristoMorado_api.util.Permiso;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Revisa en cada petición de gestión que el usuario (cabecera X-Usuario-Id,
 * la agrega el frontend) tenga el permiso del módulo:
 * - Crear/editar/borrar alumnos, padres, docentes, cuentas o personal -> USUARIOS
 * - Grados, secciones, cursos, periodos, vínculos                     -> ACADEMICO
 * - Reportes (también para ver)                                        -> REPORTES
 * Los comunicados se validan en ComunicadoService (permiso COMUNICADOS).
 * El administrador tiene todos los permisos; la Directiva, los que le asigne el admin.
 */
@Component
public class PermisoInterceptor implements HandlerInterceptor {

    /** Prefijo de URL -> permiso requerido para escribir (POST/PUT/PATCH/DELETE). */
    private static final Map<String, Permiso> ESCRITURA = Map.of(
            "/api/estudiantes", Permiso.USUARIOS,
            "/api/padres", Permiso.USUARIOS,
            "/api/docentes", Permiso.USUARIOS,
            "/api/usuarios", Permiso.USUARIOS,
            "/api/personal", Permiso.USUARIOS,
            "/api/grados", Permiso.ACADEMICO,
            "/api/secciones", Permiso.ACADEMICO,
            "/api/cursos", Permiso.ACADEMICO,
            "/api/periodos", Permiso.ACADEMICO,
            "/api/vinculos", Permiso.ACADEMICO
    );

    /** Prefijos que requieren permiso incluso para ver (GET). */
    private static final Map<String, Permiso> LECTURA = Map.of(
            "/api/reportes", Permiso.REPORTES
    );

    private final PermisoService permisoService;

    public PermisoInterceptor(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String metodo = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(metodo)) return true;

        String ruta = request.getRequestURI();
        boolean escritura = !"GET".equalsIgnoreCase(metodo) && !"HEAD".equalsIgnoreCase(metodo);

        Permiso requerido = null;
        // Cambiar permisos del personal: requiere "Gestionar permisos" (admin o p.ej. el Director).
        if (escritura && ruta.matches("/api/personal/\\d+/permisos/?")) {
            requerido = Permiso.PERMISOS;
        }
        for (var e : LECTURA.entrySet()) {
            if (requerido == null && ruta.startsWith(e.getKey())) requerido = e.getValue();
        }
        if (requerido == null && escritura) {
            for (var e : ESCRITURA.entrySet()) {
                if (ruta.startsWith(e.getKey())) requerido = e.getValue();
            }
        }
        if (requerido == null) return true;

        String cabecera = request.getHeader("X-Usuario-Id");
        Long actorId = null;
        try {
            if (cabecera != null && !cabecera.isBlank()) actorId = Long.parseLong(cabecera.trim());
        } catch (NumberFormatException ignored) { }
        permisoService.exigir(actorId, requerido);   // lanza 403 si no tiene permiso
        return true;
    }
}
