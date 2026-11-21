package com.CristoMorado_api.CristoMorado_api.dto;

/**
 * DTO de salida EstudianteResumen: datos que el backend devuelve al frontend (en JSON) sobre los
 * estudiantes. Se arma a partir de la entidad para enviar solo lo necesario (por ejemplo, nunca la
 * contraseña).
 */
public record EstudianteResumen(
        Long id,
        String nombreCompleto,
        String dni,
        String grado,
        String seccion,
        String parentesco
) {}
