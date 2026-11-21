package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MensajeResponse(
        Long id,
        String asunto,
        String contenido,
        LocalDateTime fechaEnvio,
        Long respuestaAId,
        Participante remitente,
        List<Participante> destinatarios,
        Boolean leido,
        String bandeja
) {
    public record Participante(Long usuarioId, String nombre, String rol, Boolean leido) {}
}
