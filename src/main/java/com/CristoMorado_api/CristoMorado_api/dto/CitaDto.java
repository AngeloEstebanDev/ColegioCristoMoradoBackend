package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** HU-22/27/28/29/30: disponibilidad y citas. Horas "HH:mm". */
public final class CitaDto {

    /**
     * Constructor de CitaDto.
     */
    private CitaDto() {}

    /**
     * Estructura de datos DisponibilidadRequest (record): agrupa disponibilidad request en un solo
     * objeto inmutable que viaja como JSON.
     */
    public record DisponibilidadRequest(Integer dia, String horaInicio, String horaFin, Integer duracionMinutos,
                                        String modalidad, String lugar) {}

    /**
     * Estructura de datos Disponibilidad (record): agrupa disponibilidad en un solo objeto inmutable
     * que viaja como JSON.
     */
    public record Disponibilidad(Long id, Integer dia, String horaInicio, String horaFin, Integer duracionMinutos,
                                 String modalidad, String lugar, Boolean activo, int citasPorSemana) {}

    /**
     * Estructura de datos Persona (record): agrupa persona en un solo objeto inmutable que viaja como
     * JSON.
     */
    public record Persona(Long id, String nombre, String rol, String detalle) {}

    /**
     * Estructura de datos Hijo (record): agrupa hijo en un solo objeto inmutable que viaja como JSON.
     */
    public record Hijo(Long id, String nombre, String salon) {}

    /**
     * Estructura de datos Responsable (record): agrupa responsable en un solo objeto inmutable que
     * viaja como JSON.
     */
    public record Responsable(Long usuarioId, String nombre, String rol, String detalle, List<String> relacion,
                              List<Disponibilidad> horarios) {}

    /**
     * Estructura de datos PadreContacto (record): agrupa padre contacto en un solo objeto inmutable
     * que viaja como JSON.
     */
    public record PadreContacto(Long usuarioId, String nombre, String telefono, List<Hijo> hijos) {}

    /**
     * Estructura de datos Horario (record): agrupa horario en un solo objeto inmutable que viaja como
     * JSON.
     */
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

    /**
     * Estructura de datos DisponibilidadSemanaRequest (record): agrupa disponibilidad semana request
     * en un solo objeto inmutable que viaja como JSON.
     */
    public record DisponibilidadSemanaRequest(List<BloqueSemana> bloques) {}

    /**
     * Estructura de datos ReprogramarRequest (record): agrupa reprogramar request en un solo objeto
     * inmutable que viaja como JSON.
     */
    public record ReprogramarRequest(LocalDate fecha, String horaInicio, String horaFin, String motivo) {}

    /**
     * Estructura de datos AccionRequest (record): agrupa acción request en un solo objeto inmutable
     * que viaja como JSON.
     */
    public record AccionRequest(String motivo) {}

    /** tipo: ENVIADA (la creé yo) | RECIBIDA. Acciones según la tabla de condiciones de Mi Agenda. */
    public record Cita(Long id, LocalDate fecha, String horaInicio, String horaFin, String modalidad, String lugar,
                       String motivo, String origen, String estado, String observacion, Persona responsable,
                       Persona padre, Hijo estudiante, String miRol, boolean pasada, int reprogramaciones,
                       LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion,
                       boolean puedeConfirmar, boolean puedeRechazar, boolean puedeReprogramar,
                       boolean puedeCancelar, boolean puedeMarcarRealizada,
                       String tipo, String detalle, boolean puedeEliminar) {}

    /**
     * Estructura de datos Resumen (record): agrupa resumen en un solo objeto inmutable que viaja como
     * JSON.
     */
    public record Resumen(long pendientesPorConfirmar, long proximas, long esperandoRespuesta) {}
}
