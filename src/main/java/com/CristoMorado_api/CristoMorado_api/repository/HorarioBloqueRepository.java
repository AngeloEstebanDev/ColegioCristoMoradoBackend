package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.HorarioBloque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de la entidad HorarioBloque: es la "puerta" a la tabla de los bloques del horario de
 * clases (día, hora de inicio y fin de cada curso) en PostgreSQL. Spring Data JPA crea solo las
 * consultas a partir del nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace
 * falta escribir SQL. Los métodos heredados (findAll, findById, save, delete...) vienen de
 * JpaRepository.
 */
public interface HorarioBloqueRepository extends JpaRepository<HorarioBloque, Long> {

    /**
     * Busca los registros cuyo sección id sea igual al valor recibido. Devuelve una lista (vacía si no
     * hay coincidencias).
     */
    List<HorarioBloque> findBySeccionId(Long seccionId);

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido. Devuelve una lista (vacía
     * si no hay coincidencias).
     */
    List<HorarioBloque> findBySeccionCursoId(Long seccionCursoId);
}
