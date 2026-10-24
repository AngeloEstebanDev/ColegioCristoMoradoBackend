package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByResponsableIdOrderByFechaDescHoraInicioDesc(Long responsableId);

    List<Cita> findByPadreIdOrderByFechaDescHoraInicioDesc(Long padreId);

    List<Cita> findByResponsableIdAndFechaAndEstadoIn(Long responsableId, LocalDate fecha, Collection<String> estados);

    List<Cita> findByPadreIdAndFechaAndEstadoIn(Long padreId, LocalDate fecha, Collection<String> estados);

    List<Cita> findByResponsableIdAndFechaBetweenAndEstadoIn(Long responsableId, LocalDate desde, LocalDate hasta,
                                                             Collection<String> estados);

    List<Cita> findByEstudianteIdOrderByFechaDescHoraInicioDesc(Long estudianteId);

    long countByResponsableIdAndEstado(Long responsableId, String estado);

    /** Al borrar un alumno, sus citas se conservan sin el alumno. */
    @Modifying
    @Query("update Cita c set c.estudiante = null where c.estudiante.id = :estudianteId")
    void desvincularEstudiante(@Param("estudianteId") Long estudianteId);

    /** Al borrar una cuenta se borran las citas donde participa. */
    @Modifying
    @Query("delete from Cita c where c.responsable.id = :usuarioId or c.padre.id = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
