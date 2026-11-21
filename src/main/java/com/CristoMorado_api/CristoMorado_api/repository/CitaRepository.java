package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Repositorio de la entidad Cita: es la "puerta" a la tabla de las citas o reuniones entre padres de
 * familia y docentes o directiva en PostgreSQL. Spring Data JPA crea solo las consultas a partir del
 * nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los
 * métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface CitaRepository extends JpaRepository<Cita, Long> {

    /**
     * Busca los registros cuyo responsable id sea igual al valor recibido, ordenados por fecha (de
     * mayor a menor / lo más reciente primero) y luego por hora inicio (de mayor a menor / lo más
     * reciente primero). Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Cita> findByResponsableIdOrderByFechaDescHoraInicioDesc(Long responsableId);

    /**
     * Busca los registros cuyo padre id sea igual al valor recibido, ordenados por fecha (de mayor a
     * menor / lo más reciente primero) y luego por hora inicio (de mayor a menor / lo más reciente
     * primero). Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Cita> findByPadreIdOrderByFechaDescHoraInicioDesc(Long padreId);

    /**
     * Busca los registros cuyo responsable id sea igual al valor recibido y fecha sea igual al valor
     * recibido y estado esté en la lista recibida. Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Cita> findByResponsableIdAndFechaAndEstadoIn(Long responsableId, LocalDate fecha, Collection<String> estados);

    /**
     * Busca los registros cuyo padre id sea igual al valor recibido y fecha sea igual al valor
     * recibido y estado esté en la lista recibida. Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Cita> findByPadreIdAndFechaAndEstadoIn(Long padreId, LocalDate fecha, Collection<String> estados);

    /**
     * Busca los registros cuyo responsable id sea igual al valor recibido y fecha esté entre dos
     * valores y estado esté en la lista recibida. Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Cita> findByResponsableIdAndFechaBetweenAndEstadoIn(Long responsableId, LocalDate desde, LocalDate hasta,
                                                             Collection<String> estados);

    /**
     * Busca los registros cuyo estudiante id sea igual al valor recibido, ordenados por fecha (de
     * mayor a menor / lo más reciente primero) y luego por hora inicio (de mayor a menor / lo más
     * reciente primero). Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Cita> findByEstudianteIdOrderByFechaDescHoraInicioDesc(Long estudianteId);

    /**
     * Cuenta los registros cuyo responsable id sea igual al valor recibido y estado sea igual al valor
     * recibido.
     */
    long countByResponsableIdAndEstado(Long responsableId, String estado);

    /** Al borrar un alumno, sus citas se conservan sin el alumno. */
    @Modifying
    @Query("update Cita c set c.estudiante = null where c.estudiante.id = :estudianteId")
    void desvincularEstudiante(@Param("estudianteId") Long estudianteId);

    /** Al borrar una cuenta se borran las citas donde participa. */
    @Modifying
    @Query("delete from Cita c where c.responsable.id = :usuarioId or c.padre.id = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
