package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    @Query("select m from Mensaje m where m.remitente.id = :usuarioId "
            + "and m.eliminadoRemitente = false order by m.fechaEnvio desc")
    List<Mensaje> enviadosPor(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("update Mensaje m set m.remitente = null where m.remitente.id = :usuarioId")
    void desvincularRemitente(@Param("usuarioId") Long usuarioId);
}
