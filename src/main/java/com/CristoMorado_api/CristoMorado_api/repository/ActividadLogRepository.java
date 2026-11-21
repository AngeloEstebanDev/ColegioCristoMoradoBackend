package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.ActividadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de la entidad ActividadLog: es la "puerta" a la tabla del registro de "Actividad
 * reciente" (quién hizo qué y cuándo) en PostgreSQL. Spring Data JPA crea solo las consultas a partir
 * del nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL.
 * Los métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface ActividadLogRepository extends JpaRepository<ActividadLog, Long> {

    /**
     * Busca los 20 primeros registros, ordenados por fecha (de mayor a menor / lo más reciente
     * primero) y luego por id (de mayor a menor / lo más reciente primero).
     */
    List<ActividadLog> findTop20ByOrderByFechaDescIdDesc();

    /**
     * Consulta escrita a mano (@Query): borrar anteriores a. Se usa cuando la consulta es más compleja
     * de lo que permite el nombre del método.
     */
    @Modifying
    @Query("DELETE FROM ActividadLog a WHERE a.fecha < :limite")
    int borrarAnterioresA(@Param("limite") LocalDateTime limite);
}
