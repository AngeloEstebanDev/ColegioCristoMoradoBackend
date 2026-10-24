package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Aula virtual: cursos del docente, materiales (HU-13), actividades (HU-14) y entregas (HU-15). */
public final class AulaDto {

    private AulaDto() {}

    /** Curso (asignación sección-curso) visto desde el docente, alumno o padre. */
    public record CursoAula(Long seccionCursoId, Long seccionId, String grado, String letra, String turno,
                            Long cursoId, String curso, String categoria, String color, String descripcion,
                            String docente, String estado, long totalAlumnos, long materiales,
                            long actividades, long porCalificar, long pendientes) {}

    /** Resumen del inicio del docente. */
    public record ResumenDocente(String nombre, long cursos, long secciones, long alumnos, long porCalificar,
                                 String tutorDe, List<CursoAula> misCursos, List<ActividadResponse> proximas) {}

    // ---------------- Materiales ----------------

    /** tipo: TEXTO (descripcion = HTML, imagen opcional) | ARCHIVO | ENLACE. carpetaId opcional. */
    public record MaterialRequest(String titulo, String descripcion, String enlace,
                                  ArchivoDto.Request archivo, Boolean quitarArchivo, String tipo, Long carpetaId) {}

    public record MaterialResponse(Long id, Long seccionCursoId, String curso, String color, String titulo,
                                   String descripcion, String enlace, ArchivoDto.Info archivo, String autor,
                                   LocalDateTime fechaPublicacion, LocalDateTime fechaActualizacion,
                                   String tipo, Boolean visible, Long carpetaId) {}

    // ---------------- Carpetas ----------------

    public record CarpetaRequest(String nombre) {}

    /** Carpeta del contenido, dentro de un bimestre. */
    public record Carpeta(Long id, Long seccionCursoId, Long periodoId, String periodo, Integer bimestre, Integer anio,
                          LocalDate periodoInicio, LocalDate periodoFin, String nombre, Boolean visible,
                          LocalDateTime fechaCreacion) {}

    public record VisibleRequest(Boolean visible) {}

    public record TardiaRequest(Boolean permiteTardia) {}

    // ---------------- Actividades ----------------

    /** tipo: UNICA (respuestaCorrecta) | MULTIPLE (respuestasCorrectas, al menos una). */
    public record PreguntaRequest(String enunciado, List<String> opciones, Integer respuestaCorrecta, Double puntos,
                                  String tipo, List<Integer> respuestasCorrectas) {}

    public record ActividadRequest(String tipo, String titulo, String instrucciones, LocalDateTime fechaLimite,
                                   Double puntajeMaximo, Boolean permiteTardia, String enlace,
                                   ArchivoDto.Request archivo, Boolean quitarArchivo,
                                   List<PreguntaRequest> preguntas, Integer intentosPermitidos, Long carpetaId,
                                   List<String> formatosPermitidos) {}

    /** respuestaCorrecta es null para el alumno mientras no haya enviado el cuestionario. */
    public record Pregunta(Long id, Integer orden, String enunciado, List<String> opciones,
                           Integer respuestaCorrecta, Double puntos, String tipo, List<Integer> respuestasCorrectas) {}

    /**
     * situacion (para el alumno/padre): PENDIENTE | ENTREGADA | CALIFICADA | VENCIDA.
     * Para el docente viene null y se usan los contadores.
     */
    public record ActividadResponse(Long id, Long seccionCursoId, String curso, String color, String grado,
                                    String letra, String tipo, String titulo, String instrucciones,
                                    LocalDateTime fechaPublicacion, LocalDateTime fechaLimite,
                                    Double puntajeMaximo, Boolean permiteTardia, String enlace,
                                    ArchivoDto.Info archivo, List<Pregunta> preguntas, Long evaluacionId,
                                    String periodo, boolean vencida, long totalAlumnos, long entregadas,
                                    long calificadas, String situacion, EntregaResponse miEntrega,
                                    Boolean visible, Integer intentosPermitidos, Long carpetaId, boolean editable,
                                    List<String> formatosPermitidos) {}

    // ---------------- Entregas ----------------

    /** selecciones: por pregunta, las opciones marcadas (preguntas MULTIPLE). Si falta, se usa respuestas. */
    public record EntregaRequest(String texto, String enlace, ArchivoDto.Request archivo, List<Integer> respuestas,
                                 List<List<Integer>> selecciones) {}

    public record CalificarRequest(Double puntaje, String comentario) {}

    /** nota20 = puntaje llevado a la escala 0-20 (la que va al registro de notas). */
    public record EntregaResponse(Long id, Long actividadId, Long estudianteId, String estudianteNombre,
                                  String texto, String enlace, ArchivoDto.Info archivo, List<Integer> respuestas,
                                  LocalDateTime fechaEntrega, Boolean tardia, String estado, Double puntaje,
                                  Double puntajeMaximo, Double nota20, String comentarioDocente,
                                  LocalDateTime fechaCalificacion, List<List<Integer>> selecciones, Integer intentos,
                                  List<Envio> envios) {}

    /** Un envío del historial ("Entrega 1", "Entrega 2"...). */
    public record Envio(Integer numero, LocalDateTime fecha, Boolean tardia, String texto, String enlace,
                        ArchivoDto.Info archivo, Double puntaje, List<List<Integer>> selecciones) {}

    /** Fila de la lista de entregas del docente: todos los alumnos, hayan entregado o no. */
    public record FilaEntrega(Long estudianteId, String estudianteNombre, EntregaResponse entrega, String foto) {}

    public record Entregas(ActividadResponse actividad, List<FilaEntrega> alumnos) {}

    /** Persona del curso (para "Profesor del curso" y "Participantes"). foto: ruta de avatar o data URL. */
    public record Participante(Long usuarioId, String nombre, String detalle, String foto) {}

    public record Participantes(Participante docente, List<Participante> alumnos) {}
}
