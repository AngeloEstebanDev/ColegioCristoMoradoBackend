package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

public final class ReporteDto {

    private ReporteDto() {}

    public record Indicador(String etiqueta, String valor, String detalle) {}

    public record Tabla(String tipo, String titulo, String subtitulo, List<String> columnas,
                        List<List<Object>> filas, List<Indicador> indicadores) {}

    public record Opcion(Long id, String nombre) {}

    public record Filtros(List<Opcion> periodos, Long periodoActivo, List<Opcion> secciones) {}

    public record SeccionOpcion(Long id, String grado, String letra, String nombre) {}

    public record AlumnoOpcion(Long id, String nombre, String grado, String letra, Long seccionId) {}

    public record Opciones(List<Opcion> periodos, Long periodoActivo, List<String> grados, List<SeccionOpcion> secciones,
                           List<Opcion> cursos, List<AlumnoOpcion> alumnos) {}

    public record Filtro(Long periodoId, String grado, Long seccionId, Long estudianteId, Long cursoId) {}
}
