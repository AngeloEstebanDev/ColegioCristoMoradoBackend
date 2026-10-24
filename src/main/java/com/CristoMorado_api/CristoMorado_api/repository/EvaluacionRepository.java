package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    List<Evaluacion> findBySeccionCursoIdAndPeriodoIdOrderByFechaRegistroAsc(Long seccionCursoId, Long periodoId);

    List<Evaluacion> findBySeccionCursoId(Long seccionCursoId);

    List<Evaluacion> findBySeccionCursoIdIn(Collection<Long> seccionCursoIds);

    List<Evaluacion> findByPeriodoId(Long periodoId);

    Optional<Evaluacion> findByActividadId(Long actividadId);
}
