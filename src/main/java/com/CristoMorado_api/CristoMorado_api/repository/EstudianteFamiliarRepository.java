package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.EstudianteFamiliar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstudianteFamiliarRepository extends JpaRepository<EstudianteFamiliar, Long> {

    boolean existsByEstudianteIdAndPadreFamiliaId(Long estudianteId, Long padreFamiliaId);

    boolean existsByEstudianteIdAndParentescoIgnoreCase(Long estudianteId, String parentesco);

    boolean existsByEstudianteIdAndParentescoIgnoreCaseAndIdNot(Long estudianteId, String parentesco, Long id);
}
