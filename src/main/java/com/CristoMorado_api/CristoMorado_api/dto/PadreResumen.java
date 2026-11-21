package com.CristoMorado_api.CristoMorado_api.dto;

/**
 * DTO de salida PadreResumen: datos que el backend devuelve al frontend (en JSON) sobre padre. Se arma
 * a partir de la entidad para enviar solo lo necesario (por ejemplo, nunca la contraseña).
 */
public record PadreResumen(
        Long id,
        String nombreCompleto,
        String telefono,
        String parentesco
) {}
