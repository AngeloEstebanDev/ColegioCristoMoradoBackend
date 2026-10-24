package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.SesionAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SesionAsistenciaRepository extends JpaRepository<SesionAsistencia, Long> {

    List<SesionAsistencia> findBySeccionCursoId(Long seccionCursoId);

    Optional<SesionAsistencia> findBySeccionCursoIdAndFecha(Long seccionCursoId, LocalDate fecha);
}
