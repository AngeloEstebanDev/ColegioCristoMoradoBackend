package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    boolean existsByGradoAndLetraIgnoreCase(String grado, String letra);

    java.util.Optional<Seccion> findFirstByTutorId(Long docenteId);

    java.util.Optional<Seccion> findByGradoAndLetra(String grado, String letra);

    java.util.List<Seccion> findByTutorUsuarioId(Long usuarioId);
}
