package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.SesionAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la entidad SesionAsistencia: es la "puerta" a la tabla de las clases (sesiones) en
 * las que se toma asistencia en PostgreSQL. Spring Data JPA crea solo las consultas a partir del
 * nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los
 * métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface SesionAsistenciaRepository extends JpaRepository<SesionAsistencia, Long> {

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido. Devuelve una lista (vacía
     * si no hay coincidencias).
     */
    List<SesionAsistencia> findBySeccionCursoId(Long seccionCursoId);

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido y fecha sea igual al valor
     * recibido. Devuelve un Optional: vacío si no encuentra ninguno.
     */
    Optional<SesionAsistencia> findBySeccionCursoIdAndFecha(Long seccionCursoId, LocalDate fecha);
}
