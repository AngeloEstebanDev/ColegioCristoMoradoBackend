package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.ComunicadoLectura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de la entidad ComunicadoLectura: es la "puerta" a la tabla de quién leyó cada comunicado
 * en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada método (por
 * ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos heredados
 * (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface ComunicadoLecturaRepository extends JpaRepository<ComunicadoLectura, Long> {

    /**
     * Indica si existe algún registro cuyo comunicado id sea igual al valor recibido y usuario id sea
     * igual al valor recibido.
     */
    boolean existsByComunicadoIdAndUsuarioId(Long comunicadoId, Long usuarioId);

    /**
     * Consulta escrita a mano (@Query): ids leidos por. Se usa cuando la consulta es más compleja de
     * lo que permite el nombre del método.
     */
    @Query("select l.comunicadoId from ComunicadoLectura l where l.usuarioId = :usuarioId")
    List<Long> idsLeidosPor(@Param("usuarioId") Long usuarioId);

    /** Filas [comunicadoId, cantidad de lecturas]. */
    @Query("select l.comunicadoId, count(l) from ComunicadoLectura l group by l.comunicadoId")
    List<Object[]> contarPorComunicado();

    /**
     * Consulta escrita a mano (@Query): eliminar por comunicado. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("delete from ComunicadoLectura l where l.comunicadoId = :comunicadoId")
    void eliminarPorComunicado(@Param("comunicadoId") Long comunicadoId);

    /**
     * Consulta escrita a mano (@Query): eliminar por usuario. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("delete from ComunicadoLectura l where l.usuarioId = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
