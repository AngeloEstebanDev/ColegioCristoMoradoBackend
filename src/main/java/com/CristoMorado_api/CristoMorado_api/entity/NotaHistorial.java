package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** HU-17: registro de cada corrección de una nota (valor anterior, nuevo, quién y por qué). */
@Entity
@Table(name = "notas_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluacion_id", nullable = false)
    private Long evaluacionId;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    /** null = la nota se registró por primera vez. */
    @Column(name = "valor_anterior")
    private Double valorAnterior;

    /** null = la nota se borró. */
    @Column(name = "valor_nuevo")
    private Double valorNuevo;

    @Column(length = 300)
    private String motivo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 100)
    private String usuarioNombre;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
}
