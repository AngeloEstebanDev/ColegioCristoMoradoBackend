package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Usuario;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioResponse(
        Long id,
        String nombre,
        String usuario,
        String correo,
        Boolean correoVerificado,
        String rol,
        Boolean estado,
        LocalDateTime fechaRegistro,
        String vinculadoA,
        List<String> permisos
) {
    public static UsuarioResponse fromEntity(Usuario u) {
        return fromEntity(u, null);
    }

    public static UsuarioResponse fromEntity(Usuario u, String vinculadoA) {
        return conPermisos(u, vinculadoA, null);
    }

    public static UsuarioResponse conPermisos(Usuario u, String vinculadoA, List<String> permisos) {
        return new UsuarioResponse(
                u.getId(), u.getNombre(), u.getUsuario(), u.getCorreo(),
                Boolean.TRUE.equals(u.getCorreoVerificado()),
                u.getRol(), u.getEstado(), u.getFechaRegistro(), vinculadoA, permisos
        );
    }
}
