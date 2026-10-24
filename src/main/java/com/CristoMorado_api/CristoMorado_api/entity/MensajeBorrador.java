package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Mensaje que el usuario guardó sin enviar (bandeja "Borradores"). Solo lo ve su autor. */
@Entity
@Table(name = "mensaje_borradores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeBorrador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Puede quedar vacío si aún no eligió a quién escribir. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destinatario_id")
    private Usuario destinatario;

    @Column(nullable = false, length = 200)
    @Builder.Default
    private String asunto = "";

    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String contenido = "";

    /** Id del mensaje al que responde (opcional). */
    @Column(name = "respuesta_a_id")
    private Long respuestaAId;

    @Column(name = "fecha_actualizacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
}
