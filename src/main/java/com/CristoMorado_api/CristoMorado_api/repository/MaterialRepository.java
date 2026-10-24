package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findBySeccionCursoIdOrderByFechaPublicacionDesc(Long seccionCursoId);

    long countBySeccionCursoId(Long seccionCursoId);

    java.util.Optional<Material> findFirstByArchivoId(Long archivoId);
}
