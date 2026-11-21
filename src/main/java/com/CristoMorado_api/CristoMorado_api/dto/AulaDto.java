package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AulaDto {

    private AulaDto() {}

    public record CursoAula(Long seccionCursoId, Long seccionId, String grado, String letra, String turno,
                            Long cursoId, String curso, String categoria, String color, String descripcion,
                            String docente, String estado, long totalAlumnos, long materiales,
                            long actividades, long porCalificar, long pendientes) {}

    public record ResumenDocente(String nombre, long cursos, long secciones, long alumnos, long porCalificar,
                                 String tutorDe, List<CursoAula> misCursos, List<ActividadResponse> proximas) {}

    public record MaterialRequest(String titulo, String descripcion, String enlace,
                                  ArchivoDto.Request archivo, Boolean quitarArchivo, String tipo, Long carpetaId) {}

    public record MaterialResponse(Long id, Long seccionCursoId, String curso, String color, String titulo,
                                   String descripcion, String enlace, ArchivoDto.Info archivo, String autor,
                                   LocalDateTime fechaPublicacion, LocalDateTime fechaActualizacion,
                                   String tipo, Boolean visible, Long carpetaId) {}

    public record CarpetaRequest(String nombre) {}

    public record Carpeta(Long id, Long seccionCursoId, Long periodoId, String periodo, Integer bimestre, Integer anio,
                          LocalDate periodoInicio, LocalDate periodoFin, String nombre, Boolean visible,
                          LocalDateTime fechaCreacion) {}

    public record VisibleRequest(Boolean visible) {}

    public record TardiaRequest(Boolean permiteTardia) {}

    public record PreguntaRequest(String enunciado, List<String> opciones, Integer respuestaCorrecta, Double puntos,
                                  String tipo, List<Integer> respuestasCorrectas) {}

    public record ActividadRequest(String tipo, String titulo, String instrucciones, LocalDateTime fechaLimite,
                                   Double puntajeMaximo, Boolean permiteTardia, String enlace,
                                   ArchivoDto.Request archivo, Boolean quitarArchivo,
                                   List<PreguntaRequest> preguntas, Integer intentosPermitidos, Long carpetaId,
                                   List<String> formatosPermitidos) {}

    public record Pregunta(Long id, Integer orden, String enunciado, List<String> opciones,
                           Integer respuestaCorrecta, Double puntos, String tipo, List<Integer> respuestasCorrectas) {}

    public record ActividadResponse(Long id, Long seccionCursoId, String curso, String color, String grado,
                                    String letra, String tipo, String titulo, String instrucciones,
                                    LocalDateTime fechaPublicacion, LocalDateTime fechaLimite,
                                    Double puntajeMaximo, Boolean permiteTardia, String enlace,
                                    ArchivoDto.Info archivo, List<Pregunta> preguntas, Long evaluacionId,
                                    String periodo, boolean vencida, long totalAlumnos, long entregadas,
                                    long calificadas, String situacion, EntregaResponse miEntrega,
                                    Boolean visible, Integer intentosPermitidos, Long carpetaId, boolean editable,
                                    List<String> formatosPermitidos) {}

    public record EntregaRequest(String texto, String enlace, ArchivoDto.Request archivo, List<Integer> respuestas,
                                 List<List<Integer>> selecciones) {}

    public record CalificarRequest(Double puntaje, String comentario) {}

    public record EntregaResponse(Long id, Long actividadId, Long estudianteId, String estudianteNombre,
                                  String texto, String enlace, ArchivoDto.Info archivo, List<Integer> respuestas,
                                  LocalDateTime fechaEntrega, Boolean tardia, String estado, Double puntaje,
                                  Double puntajeMaximo, Double nota20, String comentarioDocente,
                                  LocalDateTime fechaCalificacion, List<List<Integer>> selecciones, Integer intentos,
                                  List<Envio> envios) {}

    public record Envio(Integer numero, LocalDateTime fecha, Boolean tardia, String texto, String enlace,
                        ArchivoDto.Info archivo, Double puntaje, List<List<Integer>> selecciones) {}

    public record FilaEntrega(Long estudianteId, String estudianteNombre, EntregaResponse entrega, String foto) {}

    public record Entregas(ActividadResponse actividad, List<FilaEntrega> alumnos) {}

    public record Participante(Long usuarioId, String nombre, String detalle, String foto) {}

    public record Participantes(Participante docente, List<Participante> alumnos) {}
}
