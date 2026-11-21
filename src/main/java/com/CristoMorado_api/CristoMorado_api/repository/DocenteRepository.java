package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de la entidad Docente: es la "puerta" a la tabla de los docentes del colegio en
 * PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada método (por ejemplo
 * findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos heredados (findAll,
 * findById, save, delete...) vienen de JpaRepository.
 */
public interface DocenteRepository extends JpaRepository<Docente, Long> {

    /**
     * Indica si existe algún registro cuyo dni sea igual al valor recibido.
     */
    boolean existsByDni(String dni);

    /**
     * Busca los registros cuyo dni sea igual al valor recibido. Devuelve un Optional: vacío si no
     * encuentra ninguno.
     */
    Optional<Docente> findByDni(String dni);

    /**
     * Busca los registros cuyo usuario id sea igual al valor recibido. Devuelve un Optional: vacío si
     * no encuentra ninguno.
     */
    Optional<Docente> findByUsuarioId(Long usuarioId);

    /**
     * Indica si existe algún registro cuyo usuario id sea igual al valor recibido.
     */
    boolean existsByUsuarioId(Long usuarioId);
}
