package com.CristoMorado_api.CristoMorado_api.dto;

public record EstudianteResumen(
        Long id,
        String nombreCompleto,
        String dni,
        String grado,
        String seccion,
        String parentesco
) {}
