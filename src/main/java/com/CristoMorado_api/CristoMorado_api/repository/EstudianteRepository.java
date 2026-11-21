package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la entidad Estudiante: es la "puerta" a la tabla de los estudiantes (alumnos) del
 * colegio en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada método
 * (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos heredados
 * (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    /**
     * Indica si existe algún registro cuyo dni sea igual al valor recibido.
     */
    boolean existsByDni(String dni);

    /**
     * Busca los registros cuyo dni sea igual al valor recibido. Devuelve un Optional: vacío si no
     * encuentra ninguno.
     */
    Optional<Estudiante> findByDni(String dni);

    /**
     * Busca los 5 primeros registros, ordenados por fecha registro (de mayor a menor / lo más reciente
     * primero).
     */
    List<Estudiante> findTop5ByOrderByFechaRegistroDesc();

    /**
     * Busca los registros cuyo usuario id sea igual al valor recibido. Devuelve un Optional: vacío si
     * no encuentra ninguno.
     */
    Optional<Estudiante> findByUsuarioId(Long usuarioId);

    /** Alumnos matriculados en una sección (mismo grado y letra). */
    List<Estudiante> findByGradoAndSeccionOrderByNombreCompletoAsc(String grado, String seccion);

    /**
     * Cuenta los registros cuyo grado sea igual al valor recibido y sección sea igual al valor
     * recibido.
     */
    long countByGradoAndSeccion(String grado, String seccion);
}
