package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio de la entidad Actividad: es la "puerta" a la tabla de las actividades del aula virtual
 * (tareas y cuestionarios) en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre
 * de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los
 * métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido, ordenados por fecha
     * limite (de mayor a menor / lo más reciente primero). Devuelve una lista (vacía si no hay
     * coincidencias).
     */
    List<Actividad> findBySeccionCursoIdOrderByFechaLimiteDesc(Long seccionCursoId);

    /**
     * Busca los registros cuyo sección curso id esté en la lista recibida, ordenados por fecha limite
     * (de menor a mayor). Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Actividad> findBySeccionCursoIdInOrderByFechaLimiteAsc(Collection<Long> seccionCursoIds);

    /**
     * Cuenta los registros cuyo sección curso id sea igual al valor recibido.
     */
    long countBySeccionCursoId(Long seccionCursoId);

    /**
     * Busca el primer registro cuyo archivo id sea igual al valor recibido.
     */
    java.util.Optional<Actividad> findFirstByArchivoId(Long archivoId);
}
