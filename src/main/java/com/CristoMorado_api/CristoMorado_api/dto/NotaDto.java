package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** HU-16/17/19/20/21: registro de notas, promedios, boleta e historial. Escala vigesimal 0-20. */
public final class NotaDto {

    /**
     * Constructor de NotaDto.
     */
    private NotaDto() {}

    /**
     * Estructura de datos PeriodoCorto (record): agrupa periodo corto en un solo objeto inmutable que
     * viaja como JSON.
     */
    public record PeriodoCorto(Long id, String nombre, Integer anio, Integer bimestre, Boolean activo) {}

    /**
     * Estructura de datos EvaluacionRequest (record): agrupa evaluación request en un solo objeto
     * inmutable que viaja como JSON.
     */
    public record EvaluacionRequest(Long periodoId, String nombre, String tipo, Integer peso, LocalDate fecha) {}

    /**
     * Estructura de datos Evaluacion (record): agrupa evaluación en un solo objeto inmutable que viaja
     * como JSON.
     */
    public record Evaluacion(Long id, String nombre, String tipo, Integer peso, LocalDate fecha, Long actividadId,
                             long notasRegistradas) {}

    /** Una celda modificada. valor null = borrar la nota. */
    public record Cambio(Long evaluacionId, Long estudianteId, Double valor, String comentario) {}

    /**
     * Estructura de datos GuardarNotasRequest (record): agrupa guardar notas request en un solo objeto
     * inmutable que viaja como JSON.
     */
    public record GuardarNotasRequest(List<Cambio> cambios, String motivo) {}

    /** Promedio: valor con 2 decimales; redondeado = entero (10.5 -> 11); literal = AD, A, B o C. */
    public record Promedio(Double valor, Integer redondeado, String literal) {}

    /** notas: solo de los exámenes/registros del docente; participacion: promedio de sus actividades. */
    public record FilaAlumno(Long estudianteId, String nombre, Map<Long, Double> notas,
                             Map<Long, String> comentarios, Promedio promedio, Double participacion, String foto) {}

    // ---------------- Participación (promedio de tareas, cuestionarios y actividades) ----------------

    /** estado: CALIFICADA | NO_ENTREGADA (cuenta 0) | POR_CALIFICAR | PENDIENTE (estos dos aún no cuentan). */
    public record ParticipacionDetalle(Long actividadId, String titulo, String tipo, Double nota, String estado,
                                       LocalDate fecha) {}

    /**
     * Estructura de datos ParticipacionAlumno (record): agrupa participación alumno en un solo objeto
     * inmutable que viaja como JSON.
     */
    public record ParticipacionAlumno(Long estudianteId, String nombre, String foto, Promedio promedio,
                                      List<ParticipacionDetalle> detalle) {}

    /**
     * Estructura de datos Participacion (record): agrupa participación en un solo objeto inmutable que
     * viaja como JSON.
     */
    public record Participacion(Long seccionCursoId, PeriodoCorto periodo, LocalDate fechaRegistro, int actividades,
                                List<ParticipacionAlumno> alumnos) {}

    /**
     * Estructura de datos Registro (record): agrupa registro en un solo objeto inmutable que viaja
     * como JSON.
     */
    public record Registro(Long seccionCursoId, String curso, String color, String grado, String letra,
                           PeriodoCorto periodo, List<PeriodoCorto> periodos, List<Evaluacion> evaluaciones,
                           List<FilaAlumno> alumnos, Promedio promedioCurso, long aprobados, long desaprobados,
                           long sinNotas) {}

    /**
     * Estructura de datos CambioHistorial (record): agrupa cambio historial en un solo objeto
     * inmutable que viaja como JSON.
     */
    public record CambioHistorial(Long id, String evaluacion, String estudiante, Double valorAnterior,
                                  Double valorNuevo, String motivo, String usuario, LocalDateTime fecha) {}

    // ---------------- Boleta del alumno (HU-20) ----------------

    /**
     * Estructura de datos NotaDetalle (record): agrupa nota detalle en un solo objeto inmutable que
     * viaja como JSON.
     */
    public record NotaDetalle(String evaluacion, String tipo, Integer peso, LocalDate fecha, Double valor,
                              String comentario) {}

    /**
     * Estructura de datos CursoPeriodo (record): agrupa curso periodo en un solo objeto inmutable que
     * viaja como JSON.
     */
    public record CursoPeriodo(Long periodoId, Promedio promedio, List<NotaDetalle> notas) {}

    /**
     * Estructura de datos CursoBoleta (record): agrupa curso boleta en un solo objeto inmutable que
     * viaja como JSON.
     */
    public record CursoBoleta(Long seccionCursoId, String curso, String color, String categoria, String docente,
                              List<CursoPeriodo> periodos, Promedio promedioAnual) {}

    /**
     * Estructura de datos PromedioPeriodo (record): agrupa promedio periodo en un solo objeto
     * inmutable que viaja como JSON.
     */
    public record PromedioPeriodo(Long periodoId, Promedio promedio) {}

    /**
     * Estructura de datos Boleta (record): agrupa boleta en un solo objeto inmutable que viaja como
     * JSON.
     */
    public record Boleta(Long estudianteId, String nombre, String grado, String letra, Integer anio,
                         List<Integer> anios, List<PeriodoCorto> periodos, List<CursoBoleta> cursos,
                         List<PromedioPeriodo> promediosGenerales, Promedio promedioAnual,
                         long cursosAprobados, long cursosDesaprobados) {}

    // ---------------- Historial académico (HU-21) ----------------

    /**
     * Estructura de datos AnioHistorial (record): agrupa año historial en un solo objeto inmutable que
     * viaja como JSON.
     */
    public record AnioHistorial(Integer anio, String grado, String letra, boolean actual,
                                List<PromedioPeriodo> periodos, List<PeriodoCorto> nombresPeriodos,
                                Promedio promedioAnual, long cursos, long cursosAprobados,
                                long cursosDesaprobados, Double asistencia, String situacion) {}

    /**
     * Estructura de datos Historial (record): agrupa historial en un solo objeto inmutable que viaja
     * como JSON.
     */
    public record Historial(Long estudianteId, String nombre, String gradoActual, String letraActual,
                            Integer anioIngreso, List<AnioHistorial> anios) {}
}
