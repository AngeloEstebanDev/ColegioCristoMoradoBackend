package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDateTime;

public record ActividadReciente(
        LocalDateTime fecha,
        String usuario,
        String accion,
        String modulo,
        String estado
) {}
