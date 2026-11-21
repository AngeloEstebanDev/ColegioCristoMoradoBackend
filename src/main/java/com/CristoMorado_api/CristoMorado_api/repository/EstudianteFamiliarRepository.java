package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.EstudianteFamiliar;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de la entidad EstudianteFamiliar: es la "puerta" a la tabla del vínculo entre un padre o
 * madre y su hijo (estudiante) en PostgreSQL. Spring Data JPA crea solo las consultas a partir del
 * nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los
 * métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface EstudianteFamiliarRepository extends JpaRepository<EstudianteFamiliar, Long> {

    /**
     * Indica si existe algún registro cuyo estudiante id sea igual al valor recibido y padre familia
     * id sea igual al valor recibido.
     */
    boolean existsByEstudianteIdAndPadreFamiliaId(Long estudianteId, Long padreFamiliaId);

    /**
     * Indica si existe algún registro cuyo estudiante id sea igual al valor recibido y parentesco (sin
     * importar mayúsculas).
     */
    boolean existsByEstudianteIdAndParentescoIgnoreCase(Long estudianteId, String parentesco);

    /**
     * Indica si existe algún registro cuyo estudiante id sea igual al valor recibido y parentesco (sin
     * importar mayúsculas) y id sea distinto.
     */
    boolean existsByEstudianteIdAndParentescoIgnoreCaseAndIdNot(Long estudianteId, String parentesco, Long id);
}
