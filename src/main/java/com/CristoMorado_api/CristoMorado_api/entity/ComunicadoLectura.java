package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Registro de que un usuario ya leyó un comunicado. */
@Entity
@Table(name = "comunicado_lecturas",
        uniqueConstraints = @UniqueConstraint(
                name = "comunicado_lecturas_unique",
                columnNames = {"comunicado_id", "usuario_id"}))
/**
 * Entidad ComunicadoLectura: representa la tabla «comunicado_lecturas» de la base de datos (quién leyó
 * cada comunicado). Cada atributo es una columna y cada objeto es una fila. Hibernate (JPA) se encarga
 * de leer y guardar estos datos. Lombok genera los getters, setters y el builder para no escribirlos a
 * mano.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComunicadoLectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comunicado_id", nullable = false)
    private Long comunicadoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "fecha_lectura", nullable = false)
    @Builder.Default
    private LocalDateTime fechaLectura = LocalDateTime.now();
}
