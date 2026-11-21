package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.NotaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio de la entidad NotaHistorial: es la "puerta" a la tabla del historial de cambios de cada
 * nota (quién la cambió, cuándo y por qué) en PostgreSQL. Spring Data JPA crea solo las consultas a
 * partir del nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta
 * escribir SQL. Los métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface NotaHistorialRepository extends JpaRepository<NotaHistorial, Long> {

    /**
     * Busca los registros cuyo evaluación id esté en la lista recibida, ordenados por fecha (de mayor
     * a menor / lo más reciente primero). Devuelve una lista (vacía si no hay coincidencias).
     */
    List<NotaHistorial> findByEvaluacionIdInOrderByFechaDesc(Collection<Long> evaluacionIds);

    /**
     * Busca los registros cuyo evaluación id sea igual al valor recibido y estudiante id sea igual al
     * valor recibido, ordenados por fecha (de mayor a menor / lo más reciente primero). Devuelve una
     * lista (vacía si no hay coincidencias).
     */
    List<NotaHistorial> findByEvaluacionIdAndEstudianteIdOrderByFechaDesc(Long evaluacionId, Long estudianteId);
}
