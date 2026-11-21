package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.PadreFamilia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la entidad PadreFamilia: es la "puerta" a la tabla de los padres de familia o
 * apoderados en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada método
 * (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos heredados
 * (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface PadreFamiliaRepository extends JpaRepository<PadreFamilia, Long> {

    /**
     * Indica si existe algún registro cuyo dni sea igual al valor recibido.
     */
    boolean existsByDni(String dni);

    /**
     * Busca los registros cuyo dni sea igual al valor recibido. Devuelve un Optional: vacío si no
     * encuentra ninguno.
     */
    Optional<PadreFamilia> findByDni(String dni);

    /**
     * Busca los 5 primeros registros, ordenados por fecha registro (de mayor a menor / lo más reciente
     * primero).
     */
    List<PadreFamilia> findTop5ByOrderByFechaRegistroDesc();

    /**
     * Busca los registros cuyo usuario id sea igual al valor recibido. Devuelve un Optional: vacío si
     * no encuentra ninguno.
     */
    Optional<PadreFamilia> findByUsuarioId(Long usuarioId);
}
