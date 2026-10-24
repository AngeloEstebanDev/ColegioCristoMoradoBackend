package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.HorarioBloque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioBloqueRepository extends JpaRepository<HorarioBloque, Long> {

    List<HorarioBloque> findBySeccionId(Long seccionId);

    List<HorarioBloque> findBySeccionCursoId(Long seccionCursoId);
}
