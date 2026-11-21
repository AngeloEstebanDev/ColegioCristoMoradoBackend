package com.CristoMorado_api.CristoMorado_api.dto;

/**
 * DTO de salida NotificacionesResponse: datos que el backend devuelve al frontend (en JSON) sobre
 * notificaciones. Se arma a partir de la entidad para enviar solo lo necesario (por ejemplo, nunca la
 * contraseña).
 */
public record NotificacionesResponse(
        long comunicadosNoLeidos,
        long mensajesNoLeidos,
        long total
) {}
