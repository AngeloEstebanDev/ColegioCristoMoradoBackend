package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Personal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de la entidad Personal: es la "puerta" a la tabla de la directiva y el personal
 * administrativo en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada
 * método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos
 * heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface PersonalRepository extends JpaRepository<Personal, Long> {

    /**
     * Indica si existe algún registro cuyo número documento sea igual al valor recibido.
     */
    boolean existsByNumeroDocumento(String numeroDocumento);

    /**
     * Busca los registros cuyo usuario id sea igual al valor recibido. Devuelve un Optional: vacío si
     * no encuentra ninguno.
     */
    Optional<Personal> findByUsuarioId(Long usuarioId);
}
