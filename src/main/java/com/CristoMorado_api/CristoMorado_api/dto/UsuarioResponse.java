package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Usuario;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de salida UsuarioResponse: datos que el backend devuelve al frontend (en JSON) sobre las cuentas
 * de usuario, roles y permisos. Se arma a partir de la entidad para enviar solo lo necesario (por
 * ejemplo, nunca la contraseña).
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String usuario,
        String correo,             // Gmail vinculado (puede ser null)
        Boolean correoVerificado,
        String rol,
        Boolean estado,
        LocalDateTime fechaRegistro,
        String vinculadoA,         // "Alumno" | "Docente" | "Apoderado" | "Directiva" | null
        List<String> permisos      // solo en el login: permisos del panel (admin: todos)
) {
    /**
     * Convierte la entidad en este DTO, copiando solo los datos que necesita el frontend.
     */
    public static UsuarioResponse fromEntity(Usuario u) {
        return fromEntity(u, null);
    }

    /**
     * Convierte la entidad en este DTO, copiando solo los datos que necesita el frontend.
     */
    public static UsuarioResponse fromEntity(Usuario u, String vinculadoA) {
        return conPermisos(u, vinculadoA, null);
    }

    /**
     * Devuelve con permisos.
     */
    public static UsuarioResponse conPermisos(Usuario u, String vinculadoA, List<String> permisos) {
        return new UsuarioResponse(
                u.getId(), u.getNombre(), u.getUsuario(), u.getCorreo(),
                Boolean.TRUE.equals(u.getCorreoVerificado()),
                u.getRol(), u.getEstado(), u.getFechaRegistro(), vinculadoA, permisos
        );
    }
}
