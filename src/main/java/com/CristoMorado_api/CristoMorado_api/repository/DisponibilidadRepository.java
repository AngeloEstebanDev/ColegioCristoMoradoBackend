package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

    List<Disponibilidad> findByUsuarioIdOrderByDiaAscHoraInicioAsc(Long usuarioId);

    List<Disponibilidad> findByUsuarioIdAndActivoTrue(Long usuarioId);

    List<Disponibilidad> findByActivoTrue();

    @Modifying
    @Query("delete from Disponibilidad d where d.usuario.id = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
