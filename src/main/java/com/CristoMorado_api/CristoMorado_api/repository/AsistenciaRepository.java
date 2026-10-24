package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.List;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findBySeccionCursoIdAndFecha(Long seccionCursoId, LocalDate fecha);

    List<Asistencia> findBySeccionCursoIdAndFechaBetween(Long seccionCursoId, LocalDate desde, LocalDate hasta);

    List<Asistencia> findBySeccionCursoId(Long seccionCursoId);

    List<Asistencia> findByEstudianteIdAndFechaBetween(Long estudianteId, LocalDate desde, LocalDate hasta);

    List<Asistencia> findByEstudianteId(Long estudianteId);

    @Modifying
    @Query("delete from Asistencia a where a.estudiante.id = :estudianteId")
    void eliminarPorEstudiante(@Param("estudianteId") Long estudianteId);
}
