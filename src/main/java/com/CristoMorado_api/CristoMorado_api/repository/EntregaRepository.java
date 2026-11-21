package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la entidad Entrega: es la "puerta" a la tabla de la entrega que hace un alumno de una
 * actividad (y su calificación) en PostgreSQL. Spring Data JPA crea solo las consultas a partir del
 * nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los
 * métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    /**
     * Busca los registros cuyo actividad id sea igual al valor recibido. Devuelve una lista (vacía si
     * no hay coincidencias).
     */
    List<Entrega> findByActividadId(Long actividadId);

    /**
     * Busca los registros cuyo actividad id sea igual al valor recibido y estudiante id sea igual al
     * valor recibido. Devuelve un Optional: vacío si no encuentra ninguno.
     */
    Optional<Entrega> findByActividadIdAndEstudianteId(Long actividadId, Long estudianteId);

    /**
     * Busca los registros cuyo estudiante id sea igual al valor recibido. Devuelve una lista (vacía si
     * no hay coincidencias).
     */
    List<Entrega> findByEstudianteId(Long estudianteId);

    /**
     * Cuenta los registros cuyo actividad id sea igual al valor recibido.
     */
    long countByActividadId(Long actividadId);

    /**
     * Cuenta los registros cuyo actividad id sea igual al valor recibido y estado sea igual al valor
     * recibido.
     */
    long countByActividadIdAndEstado(Long actividadId, String estado);

    /**
     * Busca el primer registro cuyo archivo id sea igual al valor recibido.
     */
    java.util.Optional<Entrega> findFirstByArchivoId(Long archivoId);
}
