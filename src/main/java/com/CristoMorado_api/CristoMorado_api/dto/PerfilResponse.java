package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDateTime;

public record PerfilResponse(
        Long usuarioId,
        String nombre,
        String usuario,
        String correo,
        Boolean correoVerificado,
        String rol,
        Boolean estado,
        LocalDateTime fechaRegistro,
        Boolean puedeEditar,
        String foto,
        String telefono,
        String direccion,
        DocenteResponse docente,
        EstudianteResponse estudiante,
        PadreFamiliaResponse padre,
        PersonalResponse personal,
        java.util.List<String> permisos
) {}
