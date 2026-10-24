package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.PadreFamilia;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PadreFamiliaResponse(
        Long id,
        String nombreCompleto,
        String tipoDocumento,
        String dni,
        LocalDate fechaNacimiento,
        String telefono,
        String correo,
        String direccion,
        String estado,
        LocalDateTime fechaRegistro,
        List<EstudianteResumen> estudiantes,
        // --- Cuenta de acceso (usuario APO + DNI) ---
        Long usuarioId,
        String usuario,
        String correoCuenta,
        Boolean correoVerificado,
        Boolean cuentaActiva
) {
    public static PadreFamiliaResponse fromEntity(PadreFamilia p) {
        List<EstudianteResumen> hijos = p.getVinculosFamiliares() == null ? List.of() :
                p.getVinculosFamiliares().stream()
                        .map(v -> new EstudianteResumen(
                                v.getEstudiante().getId(),
                                v.getEstudiante().getNombreCompleto(),
                                v.getEstudiante().getDni(),
                                v.getEstudiante().getGrado(),
                                v.getEstudiante().getSeccion(),
                                v.getParentesco()))
                        .toList();
        Usuario cuenta = p.getUsuario();
        return new PadreFamiliaResponse(
                p.getId(), p.getNombreCompleto(), p.getTipoDocumento(), p.getDni(), p.getFechaNacimiento(), p.getTelefono(),
                p.getCorreo(), p.getDireccion(), p.getEstado(), p.getFechaRegistro(), hijos,
                cuenta == null ? null : cuenta.getId(),
                cuenta == null ? null : cuenta.getUsuario(),
                cuenta == null ? null : cuenta.getCorreo(),
                cuenta != null && Boolean.TRUE.equals(cuenta.getCorreoVerificado()),
                cuenta == null ? null : cuenta.getEstado()
        );
    }
}
