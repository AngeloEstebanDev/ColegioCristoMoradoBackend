package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDateTime;

/** Borradores de la mensajería (bandeja "Borradores"). */
public final class BorradorDto {

    private BorradorDto() {}

    /** id = null crea un borrador nuevo; con id actualiza el existente. */
    public record Request(Long id, Long destinatarioId, String asunto, String contenido, Long respuestaAId) {}

    public record Borrador(Long id, Long destinatarioId, String destinatarioNombre, String destinatarioRol,
                           String asunto, String contenido, Long respuestaAId, LocalDateTime fecha) {}
}
