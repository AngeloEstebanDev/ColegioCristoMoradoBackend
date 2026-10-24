package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EstudianteResponse(
        Long id,
        String nombreCompleto,
        String tipoDocumento,
        String dni,
        String fotoUrl,
        LocalDate fechaNacimiento,
        String genero,
        String nacionalidad,
        String direccion,
        String telefono,
        String grado,
        String seccion,
        Integer anioIngreso,
        String estado,
        LocalDateTime fechaRegistro,
        List<PadreResumen> padres,
        // --- Cuenta de acceso (nunca se devuelve la contraseña) ---
        Long usuarioId,
        String correo,
        String usuario,
        Boolean cuentaActiva,
        Boolean correoVerificado
) {
    public static EstudianteResponse fromEntity(Estudiante e) {
        List<PadreResumen> padres = e.getVinculosFamiliares() == null ? List.of() :
                e.getVinculosFamiliares().stream()
                        .map(v -> new PadreResumen(
                                v.getPadreFamilia().getId(),
                                v.getPadreFamilia().getNombreCompleto(),
                                v.getPadreFamilia().getTelefono(),
                                v.getParentesco()))
                        .toList();
        Usuario cuenta = e.getUsuario();
        return new EstudianteResponse(
                e.getId(), e.getNombreCompleto(), e.getTipoDocumento(), e.getDni(), e.getFotoUrl(),
                e.getFechaNacimiento(), e.getGenero(), e.getNacionalidad(), e.getDireccion(),
                e.getTelefono(), e.getGrado(), e.getSeccion(), e.getAnioIngreso(), e.getEstado(),
                e.getFechaRegistro(), padres,
                cuenta == null ? null : cuenta.getId(),
                cuenta == null ? null : cuenta.getCorreo(),
                cuenta == null ? null : cuenta.getUsuario(),
                cuenta == null ? null : cuenta.getEstado(),
                cuenta != null && Boolean.TRUE.equals(cuenta.getCorreoVerificado())
        );
    }
}
