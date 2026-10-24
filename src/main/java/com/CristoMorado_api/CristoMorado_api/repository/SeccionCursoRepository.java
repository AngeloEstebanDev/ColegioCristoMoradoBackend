package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.SeccionCurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeccionCursoRepository extends JpaRepository<SeccionCurso, Long> {

    List<SeccionCurso> findBySeccionId(Long seccionId);

    boolean existsBySeccionIdAndCursoId(Long seccionId, Long cursoId);

    long countByCursoId(Long cursoId);

    /** Cursos que dicta un docente (por el id de su cuenta de usuario). */
    List<SeccionCurso> findByDocenteUsuarioId(Long usuarioId);

    List<SeccionCurso> findBySeccionIdIn(java.util.Collection<Long> seccionIds);
}
