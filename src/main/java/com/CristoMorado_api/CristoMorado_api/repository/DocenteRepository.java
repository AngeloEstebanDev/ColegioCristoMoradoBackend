package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocenteRepository extends JpaRepository<Docente, Long> {

    boolean existsByDni(String dni);

    Optional<Docente> findByDni(String dni);

    Optional<Docente> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);
}
