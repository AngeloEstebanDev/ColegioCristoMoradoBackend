package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    /** Último código pendiente (no usado) de ese tipo para el usuario. */
    Optional<CodigoVerificacion> findTopByUsuarioIdAndTipoAndUsadoFalseOrderByFechaCreacionDesc(Long usuarioId, String tipo);

    /** Último código enviado (usado o no), para limitar los reenvíos. */
    Optional<CodigoVerificacion> findTopByUsuarioIdAndTipoOrderByFechaCreacionDesc(Long usuarioId, String tipo);

    @Modifying
    @Query("update CodigoVerificacion c set c.usado = true "
            + "where c.usuarioId = :usuarioId and c.tipo = :tipo and c.usado = false")
    void invalidarPendientes(@Param("usuarioId") Long usuarioId, @Param("tipo") String tipo);

    @Modifying
    @Query("delete from CodigoVerificacion c where c.usuarioId = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
