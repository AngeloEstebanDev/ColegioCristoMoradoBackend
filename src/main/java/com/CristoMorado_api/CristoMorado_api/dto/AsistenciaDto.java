package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;
import java.util.List;

public final class AsistenciaDto {

    private AsistenciaDto() {}

    public record Registro(Long estudianteId, String estado, String observacion) {}

    public record GuardarRequest(LocalDate fecha, List<Registro> registros) {}

    public record Fila(Long estudianteId, String nombre, String estado, String observacion, String foto) {}

    public record Dia(Long seccionCursoId, String curso, String grado, String letra, LocalDate fecha,
                      boolean registrada, List<Fila> alumnos, List<LocalDate> fechasRegistradas) {}

    public record Conteo(long presentes, long tardanzas, long faltas, long justificadas, long total,
                         Double porcentaje) {}

    public record ResumenAlumno(Long estudianteId, String nombre, Conteo conteo) {}

    public record ResumenCurso(Long seccionCursoId, String curso, LocalDate desde, LocalDate hasta,
                               long sesiones, List<ResumenAlumno> alumnos) {}

    public record Detalle(LocalDate fecha, String curso, String color, String estado, String observacion) {}

    public record PorCurso(Long seccionCursoId, String curso, String color, Conteo conteo) {}

    public record DelAlumno(Long estudianteId, String nombre, String salon, LocalDate desde, LocalDate hasta,
                            Conteo total, List<PorCurso> cursos, List<Detalle> detalle) {}

    public record Sesion(LocalDate fecha, String horaInicio, String horaFin, String estado, long registrados, boolean editable) {}

    public record Clase(LocalDate fecha, String horaInicio, String horaFin) {}

    public record Sesiones(Long seccionCursoId, String curso, String grado, String letra, boolean puedeGenerar,
                           Clase claseActual, String mensaje, long generadas, long marcadas, long pendientes,
                           long noGeneradas, List<Sesion> sesiones) {}
}
