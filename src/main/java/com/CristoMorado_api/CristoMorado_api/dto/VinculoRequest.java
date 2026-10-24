package com.CristoMorado_api.CristoMorado_api.dto;

/** Vincular un padre/apoderado con un alumno (o cambiar el parentesco al editar). */
public record VinculoRequest(Long padreId, Long estudianteId, String parentesco) {}
