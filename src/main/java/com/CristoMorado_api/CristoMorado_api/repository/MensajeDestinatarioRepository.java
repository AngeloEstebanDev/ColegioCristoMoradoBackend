package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.MensajeDestinatario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la entidad MensajeDestinatario: es la "puerta" a la tabla de los destinatarios de
 * cada mensaje (y si ya lo leyeron) en PostgreSQL. Spring Data JPA crea solo las consultas a partir
 * del nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL.
 * Los métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface MensajeDestinatarioRepository extends JpaRepository<MensajeDestinatario, Long> {

    @Query("select d from MensajeDestinatario d join fetch d.mensaje m "
            + "where d.destinatario.id = :usuarioId and d.eliminado = false "
            + "order by m.fechaEnvio desc")
    /**
     * Consulta «recibidos por» sobre la tabla.
     */
    List<MensajeDestinatario> recibidosPor(@Param("usuarioId") Long usuarioId);

    /**
     * Busca los registros cuyo mensaje id sea igual al valor recibido y destinatario id sea igual al
     * valor recibido. Devuelve un Optional: vacío si no encuentra ninguno.
     */
    Optional<MensajeDestinatario> findByMensajeIdAndDestinatarioId(Long mensajeId, Long destinatarioId);

    /**
     * Cuenta los registros cuyo destinatario id sea igual al valor recibido y leido sea falso y
     * eliminado sea falso.
     */
    long countByDestinatarioIdAndLeidoFalseAndEliminadoFalse(Long destinatarioId);

    /**
     * Consulta escrita a mano (@Query): eliminar por destinatario. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("delete from MensajeDestinatario d where d.destinatario.id = :usuarioId")
    void eliminarPorDestinatario(@Param("usuarioId") Long usuarioId);
}
