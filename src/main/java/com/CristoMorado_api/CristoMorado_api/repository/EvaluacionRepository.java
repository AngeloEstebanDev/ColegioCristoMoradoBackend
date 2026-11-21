package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la entidad Evaluacion: es la "puerta" a la tabla de las columnas del registro de
 * notas (tareas, exámenes, comportamiento) de cada bimestre en PostgreSQL. Spring Data JPA crea solo
 * las consultas a partir del nombre de cada método (por ejemplo findByDni busca por DNI), así que no
 * hace falta escribir SQL. Los métodos heredados (findAll, findById, save, delete...) vienen de
 * JpaRepository.
 */
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido y periodo id sea igual al
     * valor recibido, ordenados por fecha registro (de menor a mayor). Devuelve una lista (vacía si no
     * hay coincidencias).
     */
    List<Evaluacion> findBySeccionCursoIdAndPeriodoIdOrderByFechaRegistroAsc(Long seccionCursoId, Long periodoId);

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido. Devuelve una lista (vacía
     * si no hay coincidencias).
     */
    List<Evaluacion> findBySeccionCursoId(Long seccionCursoId);

    /**
     * Busca los registros cuyo sección curso id esté en la lista recibida. Devuelve una lista (vacía
     * si no hay coincidencias).
     */
    List<Evaluacion> findBySeccionCursoIdIn(Collection<Long> seccionCursoIds);

    /**
     * Busca los registros cuyo periodo id sea igual al valor recibido. Devuelve una lista (vacía si no
     * hay coincidencias).
     */
    List<Evaluacion> findByPeriodoId(Long periodoId);

    /**
     * Busca los registros cuyo actividad id sea igual al valor recibido. Devuelve un Optional: vacío
     * si no encuentra ninguno.
     */
    Optional<Evaluacion> findByActividadId(Long actividadId);
}
