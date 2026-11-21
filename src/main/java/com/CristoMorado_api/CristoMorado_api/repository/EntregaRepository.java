package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    List<Entrega> findByActividadId(Long actividadId);

    Optional<Entrega> findByActividadIdAndEstudianteId(Long actividadId, Long estudianteId);

    List<Entrega> findByEstudianteId(Long estudianteId);

    long countByActividadId(Long actividadId);

    long countByActividadIdAndEstado(Long actividadId, String estado);

    java.util.Optional<Entrega> findFirstByArchivoId(Long archivoId);
}
