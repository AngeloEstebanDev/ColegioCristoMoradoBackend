package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.ComunicadoLectura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComunicadoLecturaRepository extends JpaRepository<ComunicadoLectura, Long> {

    boolean existsByComunicadoIdAndUsuarioId(Long comunicadoId, Long usuarioId);

    @Query("select l.comunicadoId from ComunicadoLectura l where l.usuarioId = :usuarioId")
    List<Long> idsLeidosPor(@Param("usuarioId") Long usuarioId);

    /** Filas [comunicadoId, cantidad de lecturas]. */
    @Query("select l.comunicadoId, count(l) from ComunicadoLectura l group by l.comunicadoId")
    List<Object[]> contarPorComunicado();

    @Modifying
    @Query("delete from ComunicadoLectura l where l.comunicadoId = :comunicadoId")
    void eliminarPorComunicado(@Param("comunicadoId") Long comunicadoId);

    @Modifying
    @Query("delete from ComunicadoLectura l where l.usuarioId = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
