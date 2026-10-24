package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sección de un grado (p.ej. "1° Secundaria" - "A").
 * Sus alumnos son los estudiantes con el mismo grado y sección.
 */
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
    private String grado;          // "1° Primaria" ... "5° Secundaria"

    @Column(nullable = false, length = 5)
    private String letra;          // "A", "B", ...

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String turno = "Mañana";   // A = Mañana, B = Tarde (lo fija la letra)

    @Column(nullable = false)
    @Builder.Default
    private Integer capacidad = 35;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String estado = "Activa";  // Activa | Inactiva

    /** Docente tutor (opcional). Un docente es tutor de una sola sección a la vez. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private Docente tutor;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
