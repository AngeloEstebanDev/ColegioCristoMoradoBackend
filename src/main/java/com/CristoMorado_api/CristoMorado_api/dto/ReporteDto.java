package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

/**
 * HU-31/32: un reporte es una tabla (columnas + filas) con indicadores arriba.
 * La misma tabla se muestra en pantalla y se exporta a Excel.
 */
public final class ReporteDto {

    private ReporteDto() {}

    public record Indicador(String etiqueta, String valor, String detalle) {}

    /** filas: cada celda es texto o número. */
    public record Tabla(String tipo, String titulo, String subtitulo, List<String> columnas,
                        List<List<Object>> filas, List<Indicador> indicadores) {}

    public record Opcion(Long id, String nombre) {}

    public record Filtros(List<Opcion> periodos, Long periodoActivo, List<Opcion> secciones) {}

    /** Sección con su grado y letra (para filtrar secciones por grado). */
    public record SeccionOpcion(Long id, String grado, String letra, String nombre) {}

    /** Alumno matriculado (para filtrar alumnos por grado y sección). */
    public record AlumnoOpcion(Long id, String nombre, String grado, String letra, Long seccionId) {}

    /** Opciones de los filtros del Centro de Control de Reportes. */
    public record Opciones(List<Opcion> periodos, Long periodoActivo, List<String> grados, List<SeccionOpcion> secciones,
                           List<Opcion> cursos, List<AlumnoOpcion> alumnos) {}

    /** Filtros de los reportes por alumno (null = todos). */
    public record Filtro(Long periodoId, String grado, Long seccionId, Long estudianteId, Long cursoId) {}
}
