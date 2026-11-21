package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de la entidad Seccion: es la "puerta" a la tabla de las secciones de cada grado (por
 * ejemplo 5° Primaria A) en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de
 * cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos
 * heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    /**
     * Indica si existe algún registro cuyo grado sea igual al valor recibido y letra (sin importar
     * mayúsculas).
     */
    boolean existsByGradoAndLetraIgnoreCase(String grado, String letra);

    /** Sección de la que el docente ya es tutor (solo puede tener una). */
    java.util.Optional<Seccion> findFirstByTutorId(Long docenteId);

    /**
     * Busca los registros cuyo grado sea igual al valor recibido y letra sea igual al valor recibido.
     */
    java.util.Optional<Seccion> findByGradoAndLetra(String grado, String letra);

    /** Sección de la que es tutor un docente (por el id de su cuenta de usuario). */
    java.util.List<Seccion> findByTutorUsuarioId(Long usuarioId);
}
