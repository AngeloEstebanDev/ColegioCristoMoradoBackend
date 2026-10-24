package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Seccion;

import java.util.List;

/**
 * Sección con su resumen. En la lista de "Grados y Secciones" alumnos y cursos
 * vienen vacíos; en el detalle de la sección vienen completos.
 */
public record SeccionResponse(
        Long id,
        String grado,
        String letra,
        String turno,
        Integer capacidad,
        String estado,
        long totalAlumnos,
        long totalCursos,
        Long tutorId,
        String tutorNombre,
        List<Alumno> alumnos,
        List<CursoAsignado> cursos,
        Long tutorUsuarioId
) {
    public record Alumno(Long id, String nombreCompleto, String usuario, String tipoDocumento, String dni, String estado,
                         Long usuarioId) {}

    public record CursoAsignado(Long id, Long cursoId, String nombre, String color, String descripcion,
                                Long docenteId, String docenteNombre, String estado, Long docenteUsuarioId) {}

    public static SeccionResponse of(Seccion s, long totalAlumnos, long totalCursos,
                                     List<Alumno> alumnos, List<CursoAsignado> cursos) {
        return new SeccionResponse(s.getId(), s.getGrado(), s.getLetra(), s.getTurno(), s.getCapacidad(),
                s.getEstado(), totalAlumnos, totalCursos,
                s.getTutor() == null ? null : s.getTutor().getId(),
                s.getTutor() == null ? null : s.getTutor().getNombreCompleto(),
                alumnos, cursos,
                s.getTutor() == null ? null : s.getTutor().getUsuarioId());
    }
}
