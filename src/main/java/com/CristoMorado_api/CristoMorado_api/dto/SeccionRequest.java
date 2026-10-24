package com.CristoMorado_api.CristoMorado_api.dto;

/**
 * Crear o editar una sección. En las secciones predefinidas la letra fija el turno
 * (Primaria A = Mañana, B = Tarde; Secundaria A = Mañana). En las adicionales ("Otros")
 * se usa "turno" (Mañana o Tarde). Al editar solo se cambia la capacidad.
 */
public record SeccionRequest(String grado, String letra, String turno, Integer capacidad, Long tutorId) {}
