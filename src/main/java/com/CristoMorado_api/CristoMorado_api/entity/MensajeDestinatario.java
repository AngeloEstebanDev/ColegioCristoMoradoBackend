package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Un destinatario de un mensaje, con su propio estado leído/eliminado. */
@Entity
@Table(name = "mensaje_destinatarios",
        uniqueConstraints = @UniqueConstraint(
                name = "mensaje_destinatarios_unique",
                columnNames = {"mensaje_id", "destinatario_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeDestinatario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean leido = false;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;

    /** El destinatario lo quitó de su bandeja "Recibidos". */
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean eliminado = false;
}
