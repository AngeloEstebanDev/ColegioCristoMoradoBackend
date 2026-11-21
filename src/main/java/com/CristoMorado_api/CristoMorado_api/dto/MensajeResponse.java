package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de salida MensajeResponse: datos que el backend devuelve al frontend (en JSON) sobre los
 * mensajes internos. Se arma a partir de la entidad para enviar solo lo necesario (por ejemplo, nunca
 * la contraseña).
 */
public record MensajeResponse(
        Long id,
        String asunto,
        String contenido,
        LocalDateTime fechaEnvio,
        Long respuestaAId,
        Participante remitente,
        List<Participante> destinatarios,
        Boolean leido,     // en "Recibidos": si yo ya lo leí
        String bandeja     // RECIBIDO | ENVIADO
) {
    /**
     * Estructura de datos Participante (record): agrupa participante en un solo objeto inmutable que
     * viaja como JSON.
     */
    public record Participante(Long usuarioId, String nombre, String rol, Boolean leido) {}
}
