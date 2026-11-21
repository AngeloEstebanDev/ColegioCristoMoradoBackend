package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class NotaDto {

    private NotaDto() {}

    public record PeriodoCorto(Long id, String nombre, Integer anio, Integer bimestre, Boolean activo) {}

    public record EvaluacionRequest(Long periodoId, String nombre, String tipo, Integer peso, LocalDate fecha) {}

    public record Evaluacion(Long id, String nombre, String tipo, Integer peso, LocalDate fecha, Long actividadId,
                             long notasRegistradas) {}

    public record Cambio(Long evaluacionId, Long estudianteId, Double valor, String comentario) {}

    public record GuardarNotasRequest(List<Cambio> cambios, String motivo) {}

    public record Promedio(Double valor, Integer redondeado, String literal) {}

    public record FilaAlumno(Long estudianteId, String nombre, Map<Long, Double> notas,
                             Map<Long, String> comentarios, Promedio promedio, Double participacion, String foto) {}

    public record ParticipacionDetalle(Long actividadId, String titulo, String tipo, Double nota, String estado,
                                       LocalDate fecha) {}

    public record ParticipacionAlumno(Long estudianteId, String nombre, String foto, Promedio promedio,
                                      List<ParticipacionDetalle> detalle) {}

    public record Participacion(Long seccionCursoId, PeriodoCorto periodo, LocalDate fechaRegistro, int actividades,
                                List<ParticipacionAlumno> alumnos) {}

    public record Registro(Long seccionCursoId, String curso, String color, String grado, String letra,
                           PeriodoCorto periodo, List<PeriodoCorto> periodos, List<Evaluacion> evaluaciones,
                           List<FilaAlumno> alumnos, Promedio promedioCurso, long aprobados, long desaprobados,
                           long sinNotas) {}

    public record CambioHistorial(Long id, String evaluacion, String estudiante, Double valorAnterior,
                                  Double valorNuevo, String motivo, String usuario, LocalDateTime fecha) {}

    public record NotaDetalle(String evaluacion, String tipo, Integer peso, LocalDate fecha, Double valor,
                              String comentario) {}

    public record CursoPeriodo(Long periodoId, Promedio promedio, List<NotaDetalle> notas) {}

    public record CursoBoleta(Long seccionCursoId, String curso, String color, String categoria, String docente,
                              List<CursoPeriodo> periodos, Promedio promedioAnual) {}

    public record PromedioPeriodo(Long periodoId, Promedio promedio) {}

    public record Boleta(Long estudianteId, String nombre, String grado, String letra, Integer anio,
                         List<Integer> anios, List<PeriodoCorto> periodos, List<CursoBoleta> cursos,
                         List<PromedioPeriodo> promediosGenerales, Promedio promedioAnual,
                         long cursosAprobados, long cursosDesaprobados) {}

    public record AnioHistorial(Integer anio, String grado, String letra, boolean actual,
                                List<PromedioPeriodo> periodos, List<PeriodoCorto> nombresPeriodos,
                                Promedio promedioAnual, long cursos, long cursosAprobados,
                                long cursosDesaprobados, Double asistencia, String situacion) {}

    public record Historial(Long estudianteId, String nombre, String gradoActual, String letraActual,
                            Integer anioIngreso, List<AnioHistorial> anios) {}
}
