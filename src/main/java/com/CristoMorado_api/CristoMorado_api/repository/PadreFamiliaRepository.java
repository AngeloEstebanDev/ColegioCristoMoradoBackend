package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.PadreFamilia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PadreFamiliaRepository extends JpaRepository<PadreFamilia, Long> {

    boolean existsByDni(String dni);

    Optional<PadreFamilia> findByDni(String dni);

    List<PadreFamilia> findTop5ByOrderByFechaRegistroDesc();

    Optional<PadreFamilia> findByUsuarioId(Long usuarioId);
}
