package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

public record MensajeRequest(
        List<Long> destinatariosIds,  // ids de usuarios (un grupo se envía expandido)
        String asunto,
        String contenido,
        Long respuestaAId             // opcional: mensaje al que se responde
) {}
