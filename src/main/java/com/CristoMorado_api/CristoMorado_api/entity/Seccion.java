package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "secciones", uniqueConstraints = @UniqueConstraint(columnNames = {"grado", "letra"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String grado;

    @Column(nullable = false, length = 5)
    private String letra;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String turno = "Mañana";

    @Column(nullable = false)
    @Builder.Default
    private Integer capacidad = 35;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String estado = "Activa";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private Docente tutor;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
