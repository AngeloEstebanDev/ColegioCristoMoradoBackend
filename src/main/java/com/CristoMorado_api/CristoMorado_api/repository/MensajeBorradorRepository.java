package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.MensajeBorrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MensajeBorradorRepository extends JpaRepository<MensajeBorrador, Long> {

    List<MensajeBorrador> findByUsuarioIdOrderByFechaActualizacionDesc(Long usuarioId);

    Optional<MensajeBorrador> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Modifying
    @Query("delete from MensajeBorrador b where b.usuario.id = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("update MensajeBorrador b set b.destinatario = null where b.destinatario.id = :usuarioId")
    void desvincularDestinatario(@Param("usuarioId") Long usuarioId);
}
