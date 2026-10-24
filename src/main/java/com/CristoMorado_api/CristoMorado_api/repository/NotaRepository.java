package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    List<Nota> findByEvaluacionIdIn(Collection<Long> evaluacionIds);

    List<Nota> findByEvaluacionId(Long evaluacionId);

    Optional<Nota> findByEvaluacionIdAndEstudianteId(Long evaluacionId, Long estudianteId);

    List<Nota> findByEstudianteId(Long estudianteId);

    long countByEvaluacionId(Long evaluacionId);

    @Modifying
    @Query("delete from Nota n where n.estudiante.id = :estudianteId")
    void eliminarPorEstudiante(@Param("estudianteId") Long estudianteId);
}
