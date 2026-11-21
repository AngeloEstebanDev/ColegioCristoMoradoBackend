package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByEstadoOrderByNombreAsc(String estado);

    java.util.Optional<Curso> findByNombreIgnoreCase(String nombre);
}
