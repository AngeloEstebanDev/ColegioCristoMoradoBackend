package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

/**
 * DTO de entrada MensajeRequest: datos que el frontend envía al backend (en JSON) sobre los mensajes
 * internos. Un DTO (Data Transfer Object) es solo un "paquete" de datos: no tiene lógica ni se guarda
 * directamente.
 */
public record MensajeRequest(
        List<Long> destinatariosIds,  // ids de usuarios (un grupo se envía expandido)
        String asunto,
        String contenido,
        Long respuestaAId             // opcional: mensaje al que se responde
) {}
