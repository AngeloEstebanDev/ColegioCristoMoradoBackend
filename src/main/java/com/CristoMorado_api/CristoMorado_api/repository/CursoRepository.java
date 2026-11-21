package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de la entidad Curso: es la "puerta" a la tabla del catálogo de cursos (Matemática,
 * Comunicación, etc.) en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de
 * cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos
 * heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface CursoRepository extends JpaRepository<Curso, Long> {

    /**
     * Busca los registros cuyo estado sea igual al valor recibido, ordenados por nombre (de menor a
     * mayor). Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Curso> findByEstadoOrderByNombreAsc(String estado);

    /**
     * Busca los registros cuyo nombre (sin importar mayúsculas).
     */
    java.util.Optional<Curso> findByNombreIgnoreCase(String nombre);
}
