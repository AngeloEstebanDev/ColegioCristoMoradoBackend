package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

/**
 * Repositorio de la entidad Disponibilidad: es la "puerta" a la tabla de los horarios de atención que
 * registran docentes y directiva para recibir a los padres en PostgreSQL. Spring Data JPA crea solo
 * las consultas a partir del nombre de cada método (por ejemplo findByDni busca por DNI), así que no
 * hace falta escribir SQL. Los métodos heredados (findAll, findById, save, delete...) vienen de
 * JpaRepository.
 */
public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

    /**
     * Busca los registros cuyo usuario id sea igual al valor recibido, ordenados por día (de menor a
     * mayor) y luego por hora inicio (de menor a mayor). Devuelve una lista (vacía si no hay
     * coincidencias).
     */
    List<Disponibilidad> findByUsuarioIdOrderByDiaAscHoraInicioAsc(Long usuarioId);

    /**
     * Busca los registros cuyo usuario id sea igual al valor recibido y activo sea verdadero. Devuelve
     * una lista (vacía si no hay coincidencias).
     */
    List<Disponibilidad> findByUsuarioIdAndActivoTrue(Long usuarioId);

    /**
     * Busca los registros cuyo activo sea verdadero. Devuelve una lista (vacía si no hay
     * coincidencias).
     */
    List<Disponibilidad> findByActivoTrue();

    /**
     * Consulta escrita a mano (@Query): eliminar por usuario. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("delete from Disponibilidad d where d.usuario.id = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
