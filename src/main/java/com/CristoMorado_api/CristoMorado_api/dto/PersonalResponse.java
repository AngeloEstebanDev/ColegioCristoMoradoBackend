package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Personal;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.util.Permiso;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PersonalResponse(
        Long id,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String correo,
        String telefono,
        LocalDate fechaNacimiento,
        String cargo,
        String estado,
        LocalDateTime fechaRegistro,
        List<String> permisos,
        // Cuenta de acceso (usuario DIR + documento)
        Long usuarioId,
        String usuario,
        String correoCuenta,
        Boolean correoVerificado,
        Boolean cuentaActiva
) {
    public static PersonalResponse fromEntity(Personal p) {
        Usuario u = p.getUsuario();
        // Se devuelven en el orden del enum para que la vista sea estable.
        List<String> permisos = Permiso.todos().stream().filter(p.getPermisos()::contains).toList();
        return new PersonalResponse(
                p.getId(), p.getTipoDocumento(), p.getNumeroDocumento(), p.getNombres(),
                p.getApellidoPaterno(), p.getApellidoMaterno(), p.nombreCompleto(),
                p.getCorreo(), p.getTelefono(), p.getFechaNacimiento(), p.getCargo(), p.getEstado(),
                p.getFechaRegistro(), permisos,
                u == null ? null : u.getId(),
                u == null ? null : u.getUsuario(),
                u == null ? null : u.getCorreo(),
                u != null && Boolean.TRUE.equals(u.getCorreoVerificado()),
                u == null ? null : u.getEstado()
        );
    }
}
