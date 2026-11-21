package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de la entidad Periodo: es la "puerta" a la tabla de los periodos académicos (bimestres)
 * con sus fechas en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada
 * método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos
 * heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface PeriodoRepository extends JpaRepository<Periodo, Long> {

    /**
     * Indica si existe algún registro cuyo año sea igual al valor recibido y bimestre sea igual al
     * valor recibido.
     */
    boolean existsByAnioAndBimestre(Integer anio, Integer bimestre);

    /**
     * Busca los registros cuyo activo sea verdadero. Devuelve una lista (vacía si no hay
     * coincidencias).
     */
    List<Periodo> findByActivoTrue();
}
