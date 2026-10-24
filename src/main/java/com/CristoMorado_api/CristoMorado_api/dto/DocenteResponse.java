package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Docente;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocenteResponse(
        Long id,
        String nombreCompleto,
        String tipoDocumento,
        String dni,
        String fotoUrl,
        LocalDate fechaNacimiento,
        String genero,
        String telefono,
        String correo,
        String direccion,
        String especialidad,
        String tituloProfesional,
        LocalDate fechaIngreso,
        String estado,
        Long usuarioId,
        Boolean cuentaVinculada,
        LocalDateTime fechaRegistro,
        // --- Cuenta de acceso (usuario DOC + DNI) ---
        String usuario,
        String correoCuenta,       // Gmail vinculado por el docente
        Boolean correoVerificado,
        Boolean cuentaActiva
) {
    public static DocenteResponse fromEntity(Docente d, Usuario cuenta) {
        return new DocenteResponse(
                d.getId(), d.getNombreCompleto(), d.getTipoDocumento(), d.getDni(), d.getFotoUrl(),
                d.getFechaNacimiento(), d.getGenero(), d.getTelefono(), d.getCorreo(),
                d.getDireccion(), d.getEspecialidad(), d.getTituloProfesional(),
                d.getFechaIngreso(), d.getEstado(), d.getUsuarioId(),
                d.getUsuarioId() != null, d.getFechaRegistro(),
                cuenta == null ? null : cuenta.getUsuario(),
                cuenta == null ? null : cuenta.getCorreo(),
                cuenta != null && Boolean.TRUE.equals(cuenta.getCorreoVerificado()),
                cuenta == null ? null : cuenta.getEstado()
        );
    }
}
