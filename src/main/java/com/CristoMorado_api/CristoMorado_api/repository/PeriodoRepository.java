package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeriodoRepository extends JpaRepository<Periodo, Long> {

    boolean existsByAnioAndBimestre(Integer anio, Integer bimestre);

    List<Periodo> findByActivoTrue();
}
