package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comunicado_lecturas",
        uniqueConstraints = @UniqueConstraint(
                name = "comunicado_lecturas_unique",
                columnNames = {"comunicado_id", "usuario_id"}))
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
