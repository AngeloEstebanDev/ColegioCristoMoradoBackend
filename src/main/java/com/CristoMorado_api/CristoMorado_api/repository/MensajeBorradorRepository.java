package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.MensajeBorrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la entidad MensajeBorrador: es la "puerta" a la tabla de los borradores de mensajes
 * que aún no se envían en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de
 * cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos
 * heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface MensajeBorradorRepository extends JpaRepository<MensajeBorrador, Long> {

    /**
     * Busca los registros cuyo usuario id sea igual al valor recibido, ordenados por fecha
     * actualización (de mayor a menor / lo más reciente primero). Devuelve una lista (vacía si no hay
     * coincidencias).
     */
    List<MensajeBorrador> findByUsuarioIdOrderByFechaActualizacionDesc(Long usuarioId);

    /**
     * Busca los registros cuyo id sea igual al valor recibido y usuario id sea igual al valor
     * recibido. Devuelve un Optional: vacío si no encuentra ninguno.
     */
    Optional<MensajeBorrador> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Consulta escrita a mano (@Query): eliminar por usuario. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("delete from MensajeBorrador b where b.usuario.id = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Consulta escrita a mano (@Query): desvincular destinatario. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("update MensajeBorrador b set b.destinatario = null where b.destinatario.id = :usuarioId")
    void desvincularDestinatario(@Param("usuarioId") Long usuarioId);
}
