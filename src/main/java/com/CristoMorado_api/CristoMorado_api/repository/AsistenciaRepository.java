package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de la entidad Asistencia: es la "puerta" a la tabla de la asistencia de los alumnos en
 * cada clase en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada método
 * (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos heredados
 * (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido y fecha sea igual al valor
     * recibido. Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Asistencia> findBySeccionCursoIdAndFecha(Long seccionCursoId, LocalDate fecha);

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido y fecha esté entre dos
     * valores. Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Asistencia> findBySeccionCursoIdAndFechaBetween(Long seccionCursoId, LocalDate desde, LocalDate hasta);

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido. Devuelve una lista (vacía
     * si no hay coincidencias).
     */
    List<Asistencia> findBySeccionCursoId(Long seccionCursoId);

    /**
     * Busca los registros cuyo estudiante id sea igual al valor recibido y fecha esté entre dos
     * valores. Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Asistencia> findByEstudianteIdAndFechaBetween(Long estudianteId, LocalDate desde, LocalDate hasta);

    /**
     * Busca los registros cuyo estudiante id sea igual al valor recibido. Devuelve una lista (vacía si
     * no hay coincidencias).
     */
    List<Asistencia> findByEstudianteId(Long estudianteId);

    /**
     * Consulta escrita a mano (@Query): eliminar por estudiante. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("delete from Asistencia a where a.estudiante.id = :estudianteId")
    void eliminarPorEstudiante(@Param("estudianteId") Long estudianteId);
}
