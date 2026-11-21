package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    List<Actividad> findBySeccionCursoIdOrderByFechaLimiteDesc(Long seccionCursoId);

    List<Actividad> findBySeccionCursoIdInOrderByFechaLimiteAsc(Collection<Long> seccionCursoIds);

    long countBySeccionCursoId(Long seccionCursoId);

    java.util.Optional<Actividad> findFirstByArchivoId(Long archivoId);
}
