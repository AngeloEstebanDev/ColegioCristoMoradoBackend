package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.SeccionCurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de la entidad SeccionCurso: es la "puerta" a la tabla de los cursos que se dictan en
 * cada sección, con su docente en PostgreSQL. Spring Data JPA crea solo las consultas a partir del
 * nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los
 * métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface SeccionCursoRepository extends JpaRepository<SeccionCurso, Long> {

    /**
     * Busca los registros cuyo sección id sea igual al valor recibido. Devuelve una lista (vacía si no
     * hay coincidencias).
     */
    List<SeccionCurso> findBySeccionId(Long seccionId);

    /**
     * Indica si existe algún registro cuyo sección id sea igual al valor recibido y curso id sea igual
     * al valor recibido.
     */
    boolean existsBySeccionIdAndCursoId(Long seccionId, Long cursoId);

    /**
     * Cuenta los registros cuyo curso id sea igual al valor recibido.
     */
    long countByCursoId(Long cursoId);

    /** Cursos que dicta un docente (por el id de su cuenta de usuario). */
    List<SeccionCurso> findByDocenteUsuarioId(Long usuarioId);

    /**
     * Busca los registros cuyo sección id esté en la lista recibida. Devuelve una lista (vacía si no
     * hay coincidencias).
     */
    List<SeccionCurso> findBySeccionIdIn(java.util.Collection<Long> seccionIds);
}
