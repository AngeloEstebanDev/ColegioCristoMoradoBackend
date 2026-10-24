package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    boolean existsByDni(String dni);

    Optional<Estudiante> findByDni(String dni);

    List<Estudiante> findTop5ByOrderByFechaRegistroDesc();

    Optional<Estudiante> findByUsuarioId(Long usuarioId);

    /** Alumnos matriculados en una sección (mismo grado y letra). */
    List<Estudiante> findByGradoAndSeccionOrderByNombreCompletoAsc(String grado, String seccion);

    long countByGradoAndSeccion(String grado, String seccion);
}
