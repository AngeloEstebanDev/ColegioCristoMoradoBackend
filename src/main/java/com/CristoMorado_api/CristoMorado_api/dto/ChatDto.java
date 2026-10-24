package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mensajería en forma de chat: una conversación por persona.
 * El id de la conversación es el usuarioId del otro; 0 = "Avisos del colegio",
 * -1 = mensajes de cuentas eliminadas.
 */
public final class ChatDto {

    private ChatDto() {}

    public static final long AVISOS = 0L;
    public static final long ELIMINADO = -1L;

    /** Fila de la lista de chats. */
    public record Conversacion(
            Long id,
            String nombre,
            String rol,
            String detalle,
            String ultimoMensaje,     // texto plano, recortado
            Boolean ultimoEsMio,
            LocalDateTime fecha,
            Long noLeidos,
            Boolean puedeEscribir
    ) {}

    /** Burbuja del chat. */
    public record MensajeChat(
            Long id,
            String asunto,            // null en mensajes de chat; título en avisos y mensajes antiguos
            String contenido,         // HTML simple
            LocalDateTime fecha,
            Boolean mio,
            Boolean leido,            // si es mío: si el otro ya lo vio
            Boolean aviso
    ) {}

    public record Chat(Conversacion conversacion, List<MensajeChat> mensajes) {}

    public record EnviarRequest(String contenido) {}
}
