package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.MensajeDestinatario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MensajeDestinatarioRepository extends JpaRepository<MensajeDestinatario, Long> {

    @Query("select d from MensajeDestinatario d join fetch d.mensaje m "
            + "where d.destinatario.id = :usuarioId and d.eliminado = false "
            + "order by m.fechaEnvio desc")
    List<MensajeDestinatario> recibidosPor(@Param("usuarioId") Long usuarioId);

    Optional<MensajeDestinatario> findByMensajeIdAndDestinatarioId(Long mensajeId, Long destinatarioId);

    long countByDestinatarioIdAndLeidoFalseAndEliminadoFalse(Long destinatarioId);

    @Modifying
    @Query("delete from MensajeDestinatario d where d.destinatario.id = :usuarioId")
    void eliminarPorDestinatario(@Param("usuarioId") Long usuarioId);
}
