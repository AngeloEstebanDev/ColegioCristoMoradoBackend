package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de la entidad Material: es la "puerta" a la tabla de los materiales que el docente
 * pública en el aula virtual en PostgreSQL. Spring Data JPA crea solo las consultas a partir del
 * nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los
 * métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface MaterialRepository extends JpaRepository<Material, Long> {

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido, ordenados por fecha
     * publicación (de mayor a menor / lo más reciente primero). Devuelve una lista (vacía si no hay
     * coincidencias).
     */
    List<Material> findBySeccionCursoIdOrderByFechaPublicacionDesc(Long seccionCursoId);

    /**
     * Cuenta los registros cuyo sección curso id sea igual al valor recibido.
     */
    long countBySeccionCursoId(Long seccionCursoId);

    /**
     * Busca el primer registro cuyo archivo id sea igual al valor recibido.
     */
    java.util.Optional<Material> findFirstByArchivoId(Long archivoId);
}
