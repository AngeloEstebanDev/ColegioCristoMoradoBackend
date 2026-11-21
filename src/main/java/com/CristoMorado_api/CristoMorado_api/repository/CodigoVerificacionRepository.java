package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repositorio de la entidad CodigoVerificacion: es la "puerta" a la tabla de los códigos de 6 dígitos
 * que se envían al Gmail para verificar o cambiar la contraseña en PostgreSQL. Spring Data JPA crea
 * solo las consultas a partir del nombre de cada método (por ejemplo findByDni busca por DNI), así que
 * no hace falta escribir SQL. Los métodos heredados (findAll, findById, save, delete...) vienen de
 * JpaRepository.
 */
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    /** Último código pendiente (no usado) de ese tipo para el usuario. */
    Optional<CodigoVerificacion> findTopByUsuarioIdAndTipoAndUsadoFalseOrderByFechaCreacionDesc(Long usuarioId, String tipo);

    /** Último código enviado (usado o no), para limitar los reenvíos. */
    Optional<CodigoVerificacion> findTopByUsuarioIdAndTipoOrderByFechaCreacionDesc(Long usuarioId, String tipo);

    @Modifying
    @Query("update CodigoVerificacion c set c.usado = true "
            + "where c.usuarioId = :usuarioId and c.tipo = :tipo and c.usado = false")
    /**
     * Consulta «invalidar pendientes» sobre la tabla.
     */
    void invalidarPendientes(@Param("usuarioId") Long usuarioId, @Param("tipo") String tipo);

    /**
     * Consulta escrita a mano (@Query): eliminar por usuario. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("delete from CodigoVerificacion c where c.usuarioId = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
