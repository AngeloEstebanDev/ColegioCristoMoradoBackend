package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

public record MensajeRequest(
        List<Long> destinatariosIds,
        String asunto,
        String contenido,
        Long respuestaAId
) {}
