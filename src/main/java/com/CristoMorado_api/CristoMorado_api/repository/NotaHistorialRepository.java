package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.NotaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotaHistorialRepository extends JpaRepository<NotaHistorial, Long> {

    List<NotaHistorial> findByEvaluacionIdInOrderByFechaDesc(Collection<Long> evaluacionIds);

    List<NotaHistorial> findByEvaluacionIdAndEstudianteIdOrderByFechaDesc(Long evaluacionId, Long estudianteId);
}
