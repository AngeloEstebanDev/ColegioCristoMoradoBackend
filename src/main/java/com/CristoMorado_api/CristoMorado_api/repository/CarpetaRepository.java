package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Carpeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de la entidad Carpeta: es la "puerta" a la tabla de las carpetas (unidades) donde el
 * docente ordena el contenido de un curso por bimestre en PostgreSQL. Spring Data JPA crea solo las
 * consultas a partir del nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace
 * falta escribir SQL. Los métodos heredados (findAll, findById, save, delete...) vienen de
 * JpaRepository.
 */
public interface CarpetaRepository extends JpaRepository<Carpeta, Long> {

    /**
     * Busca los registros cuyo sección curso id sea igual al valor recibido, ordenados por fecha
     * creación (de menor a mayor). Devuelve una lista (vacía si no hay coincidencias).
     */
    List<Carpeta> findBySeccionCursoIdOrderByFechaCreacionAsc(Long seccionCursoId);
}
