package com.CristoMorado_api.CristoMorado_api.dto;

/** Cuerpo genérico de las acciones de la sección (matricular, agregar curso, asignar docente, tutor). */
public record SeccionAccionRequest(Long estudianteId, Long cursoId, Long docenteId, String descripcion) {}
