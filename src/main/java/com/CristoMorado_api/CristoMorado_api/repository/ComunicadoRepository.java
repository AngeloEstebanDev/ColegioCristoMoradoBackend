package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Comunicado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComunicadoRepository extends JpaRepository<Comunicado, Long> {

    List<Comunicado> findAllByOrderByFechaCreacionDesc();

    /** Al borrar una cuenta, sus comunicados quedan sin autor (no se pierden). */
    @Modifying
    @Query("update Comunicado c set c.autor = null where c.autor.id = :usuarioId")
    void desvincularAutor(@Param("usuarioId") Long usuarioId);
}
