package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDateTime;

/**
 * DTOs de actividad reciente: "paquetes" de datos que viajan en JSON entre el frontend y el backend.
 */
public record ActividadReciente(
        LocalDateTime fecha,
        String usuario,
        String accion,
        String modulo,
        String estado
) {}
