package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de la entidad Mensaje: es la "puerta" a la tabla de los mensajes internos entre usuarios
 * en PostgreSQL. Spring Data JPA crea solo las consultas a partir del nombre de cada método (por
 * ejemplo findByDni busca por DNI), así que no hace falta escribir SQL. Los métodos heredados
 * (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    @Query("select m from Mensaje m where m.remitente.id = :usuarioId "
            + "and m.eliminadoRemitente = false order by m.fechaEnvio desc")
    /**
     * Consulta «enviados por» sobre la tabla.
     */
    List<Mensaje> enviadosPor(@Param("usuarioId") Long usuarioId);

    /**
     * Consulta escrita a mano (@Query): desvincular remitente. Se usa cuando la consulta es más
     * compleja de lo que permite el nombre del método.
     */
    @Modifying
    @Query("update Mensaje m set m.remitente = null where m.remitente.id = :usuarioId")
    void desvincularRemitente(@Param("usuarioId") Long usuarioId);
}
