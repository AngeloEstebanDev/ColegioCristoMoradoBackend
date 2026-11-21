package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

/** Vista del alumno: su horario y sus cursos (solo lectura). */
public final class MiHorarioDto {

    /**
     * Constructor de MiHorarioDto.
     */
    private MiHorarioDto() {}

    /**
     * Estructura de datos Curso (record): agrupa curso en un solo objeto inmutable que viaja como
     * JSON.
     */
    public record Curso(Long id, String nombre, String categoria, String categoriaNombre, String color,
                        String descripcion, String docente, int minutosSemana, List<HorarioDto.Bloque> bloques) {}

    /**
     * matriculado = false si el alumno no tiene sección (o la sección no existe / está desactivada):
     * entonces horario y cursos vienen vacíos.
     */
    public record Respuesta(boolean matriculado, String grado, String letra, String turno,
                            String inicio, String fin, String recesoInicio, String recesoFin, int minutosBloque,
                            String tutor, List<HorarioDto.Bloque> bloques, List<Curso> cursos) {}

    // ---------------- Docente ----------------

    /**
     * Estructura de datos BloqueDocente (record): agrupa bloque docente en un solo objeto inmutable
     * que viaja como JSON.
     */
    public record BloqueDocente(Long seccionCursoId, String curso, String color, String categoria, String grado,
                                String letra, Integer dia, String horaInicio, String horaFin) {}

    /** Horario semanal del docente (todas sus secciones). inicio/fin: rango de la grilla. */
    public record Docente(String nombre, long cursos, long secciones, long clasesHoy, String inicio, String fin,
                          List<BloqueDocente> bloques) {}
}
