package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.EntregaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaEnvioRepository extends JpaRepository<EntregaEnvio, Long> {

    List<EntregaEnvio> findByEntregaIdOrderByNumeroAsc(Long entregaId);

    Optional<EntregaEnvio> findFirstByArchivoId(Long archivoId);
}
