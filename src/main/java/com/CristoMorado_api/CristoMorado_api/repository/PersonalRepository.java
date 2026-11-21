package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Personal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    boolean existsByNumeroDocumento(String numeroDocumento);

    Optional<Personal> findByUsuarioId(Long usuarioId);
}
