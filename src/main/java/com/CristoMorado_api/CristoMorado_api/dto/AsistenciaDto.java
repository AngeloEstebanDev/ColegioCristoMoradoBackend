package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;
import java.util.List;

/** HU-18: registro de asistencia por curso y fecha. */
public final class AsistenciaDto {

    private AsistenciaDto() {}

    public record Registro(Long estudianteId, String estado, String observacion) {}

    public record GuardarRequest(LocalDate fecha, List<Registro> registros) {}

    public record Fila(Long estudianteId, String nombre, String estado, String observacion, String foto) {}

    /** Lista del día: un registro por alumno (estado null = aún sin marcar). */
    public record Dia(Long seccionCursoId, String curso, String grado, String letra, LocalDate fecha,
                      boolean registrada, List<Fila> alumnos, List<LocalDate> fechasRegistradas) {}

    public record Conteo(long presentes, long tardanzas, long faltas, long justificadas, long total,
                         Double porcentaje) {}

    public record ResumenAlumno(Long estudianteId, String nombre, Conteo conteo) {}

    public record ResumenCurso(Long seccionCursoId, String curso, LocalDate desde, LocalDate hasta,
                               long sesiones, List<ResumenAlumno> alumnos) {}

    public record Detalle(LocalDate fecha, String curso, String color, String estado, String observacion) {}

    public record PorCurso(Long seccionCursoId, String curso, String color, Conteo conteo) {}

    /** Vista del alumno o del padre. */
    public record DelAlumno(Long estudianteId, String nombre, String salon, LocalDate desde, LocalDate hasta,
                            Conteo total, List<PorCurso> cursos, List<Detalle> detalle) {}

    // ---------------- Sesiones: la asistencia se genera solo en la hora de clase ----------------

    /** estado: MARCADA | SIN_MARCAR (generada sin lista) | NO_GENERADA (pasó la clase sin generarla) | HOY (clase de hoy aún sin generar). */
    /** editable: solo durante la hora de clase de ese día (al terminar la clase la asistencia se cierra). */
    public record Sesion(LocalDate fecha, String horaInicio, String horaFin, String estado, long registrados, boolean editable) {}

    /** claseActual: la clase en curso ahora mismo (null si no hay). */
    public record Clase(LocalDate fecha, String horaInicio, String horaFin) {}

    public record Sesiones(Long seccionCursoId, String curso, String grado, String letra, boolean puedeGenerar,
                           Clase claseActual, String mensaje, long generadas, long marcadas, long pendientes,
                           long noGeneradas, List<Sesion> sesiones) {}
}
