package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Carpeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarpetaRepository extends JpaRepository<Carpeta, Long> {

    List<Carpeta> findBySeccionCursoIdOrderByFechaCreacionAsc(Long seccionCursoId);
}
