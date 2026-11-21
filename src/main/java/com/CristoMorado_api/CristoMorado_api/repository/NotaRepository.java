package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la entidad Nota: es la "puerta" a la tabla de las notas (calificaciones) de cada
 * alumno en cada evaluación en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre
 * de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los
 * métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface NotaRepository extends JpaRepository<Nota, Long> {

    /**
     * Busca los registros cuyo evaluación id esté en la lista recibida. Devuelve una lista (vacía si
     * no hay coincidencias).
     */
    List<Nota> findByEvaluacionIdIn(Collection<Long> evaluacionIds);

    /**
     * Busca los registros cuyo evaluación id sea igual al valor recibido. Devuelve una lista (vacía si
     * no hay coincidencias).
     */
    List<Nota> findByEvaluacionId(Long evaluacionId);

    /**
     * Busca los registros cuyo evaluación id sea igual al valor recibido y estudiante id sea igual al
     * valor recibido. Devuelve un Optional: vacío si no encuentra ninguno.
     */
    Optional<Nota> findByEvaluacionIdAndEstudianteId(Long evaluacionId, Long estudianteId);

    /**
     * Busca los registros cuyo estudiante id sea igual al valor recibido. Devuelve una lista (vacía si
     * no hay coincidencias).
     */
    List<Nota> findByEstudianteId(Long estudianteId);

    /**
     * Cuenta los registros cuyo evaluación id sea igual al valor recibido.
     */
    long countByEvaluacionId(Long evaluacionId);

    /**
     * Consulta escrita a mano (@Query): eliminar por estudiante. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("delete from Nota n where n.estudiante.id = :estudianteId")
    void eliminarPorEstudiante(@Param("estudianteId") Long estudianteId);
}
