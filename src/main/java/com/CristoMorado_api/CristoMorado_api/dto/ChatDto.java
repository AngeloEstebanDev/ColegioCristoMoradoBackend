package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class ChatDto {

    private ChatDto() {}

    public static final long AVISOS = 0L;
    public static final long ELIMINADO = -1L;

    public record Conversacion(
            Long id,
            String nombre,
            String rol,
            String detalle,
            String ultimoMensaje,
            Boolean ultimoEsMio,
            LocalDateTime fecha,
            Long noLeidos,
            Boolean puedeEscribir
    ) {}

    public record MensajeChat(
            Long id,
            String asunto,
            String contenido,
            LocalDateTime fecha,
            Boolean mio,
            Boolean leido,
            Boolean aviso
    ) {}

    public record Chat(Conversacion conversacion, List<MensajeChat> mensajes) {}

    public record EnviarRequest(String contenido) {}
}
