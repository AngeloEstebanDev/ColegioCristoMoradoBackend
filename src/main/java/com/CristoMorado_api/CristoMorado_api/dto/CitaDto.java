package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** HU-22/27/28/29/30: disponibilidad y citas. Horas "HH:mm". */
public final class CitaDto {

    private CitaDto() {}

    public record DisponibilidadRequest(Integer dia, String horaInicio, String horaFin, Integer duracionMinutos,
                                        String modalidad, String lugar) {}

    public record Disponibilidad(Long id, Integer dia, String horaInicio, String horaFin, Integer duracionMinutos,
                                 String modalidad, String lugar, Boolean activo, int citasPorSemana) {}

    public record Persona(Long id, String nombre, String rol, String detalle) {}

    public record Hijo(Long id, String nombre, String salon) {}

    public record Responsable(Long usuarioId, String nombre, String rol, String detalle, List<String> relacion,
                              List<Disponibilidad> horarios) {}

    public record PadreContacto(Long usuarioId, String nombre, String telefono, List<Hijo> hijos) {}

    public record Horario(LocalDate fecha, String horaInicio, String horaFin, String modalidad, String lugar) {}

    /** detalle: descripción del caso (opcional). */
    public record SolicitarRequest(Long responsableId, Long estudianteId, LocalDate fecha, String horaInicio,
                                   String motivo, String detalle) {}

    /**
     * padreId: a quién se cita (docente: un padre de sus alumnos; administración: cualquier usuario).
     * horaFin opcional si se indica duracionMinutos. detalle: descripción adicional opcional.
     */
    public record ProgramarRequest(Long padreId, Long estudianteId, LocalDate fecha, String horaInicio,
                                   String horaFin, String modalidad, String lugar, String motivo,
                                   Integer duracionMinutos, String detalle) {}

    /** Disponibilidad semanal en grilla de 15 minutos: reemplaza todos los rangos del usuario. */
    public record BloqueSemana(Integer dia, String horaInicio, String horaFin) {}

    public record DisponibilidadSemanaRequest(List<BloqueSemana> bloques) {}

    public record ReprogramarRequest(LocalDate fecha, String horaInicio, String horaFin, String motivo) {}

    public record AccionRequest(String motivo) {}

    /** tipo: ENVIADA (la creé yo) | RECIBIDA. Acciones según la tabla de condiciones de Mi Agenda. */
    public record Cita(Long id, LocalDate fecha, String horaInicio, String horaFin, String modalidad, String lugar,
                       String motivo, String origen, String estado, String observacion, Persona responsable,
                       Persona padre, Hijo estudiante, String miRol, boolean pasada, int reprogramaciones,
                       LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion,
                       boolean puedeConfirmar, boolean puedeRechazar, boolean puedeReprogramar,
                       boolean puedeCancelar, boolean puedeMarcarRealizada,
                       String tipo, String detalle, boolean puedeEliminar) {}

    public record Resumen(long pendientesPorConfirmar, long proximas, long esperandoRespuesta) {}
}
