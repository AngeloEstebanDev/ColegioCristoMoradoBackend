package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Comunicado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de la entidad Comunicado: es la "puerta" a la tabla de los comunicados y avisos
 * institucionales en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada
 * método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos
 * heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface ComunicadoRepository extends JpaRepository<Comunicado, Long> {

    /**
     * Busca todos los registros, ordenados por fecha creación (de mayor a menor / lo más reciente
     * primero).
     */
    List<Comunicado> findAllByOrderByFechaCreacionDesc();

    /** Al borrar una cuenta, sus comunicados quedan sin autor (no se pierden). */
    @Modifying
    @Query("update Comunicado c set c.autor = null where c.autor.id = :usuarioId")
    void desvincularAutor(@Param("usuarioId") Long usuarioId);
}
