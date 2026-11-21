package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.EntregaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la entidad EntregaEnvio: es la "puerta" a la tabla de cada envío (intento) de una
 * entrega en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada método
 * (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos heredados
 * (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface EntregaEnvioRepository extends JpaRepository<EntregaEnvio, Long> {

    /**
     * Busca los registros cuyo entrega id sea igual al valor recibido, ordenados por número (de menor
     * a mayor). Devuelve una lista (vacía si no hay coincidencias).
     */
    List<EntregaEnvio> findByEntregaIdOrderByNumeroAsc(Long entregaId);

    /**
     * Busca el primer registro cuyo archivo id sea igual al valor recibido. Devuelve un Optional:
     * vacío si no encuentra ninguno.
     */
    Optional<EntregaEnvio> findFirstByArchivoId(Long archivoId);
}
