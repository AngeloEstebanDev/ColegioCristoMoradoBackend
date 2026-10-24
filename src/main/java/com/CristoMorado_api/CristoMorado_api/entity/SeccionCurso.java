package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

/** Curso asignado a una sección, con su docente (opcional). */
@Entity
@Table(name = "seccion_cursos", uniqueConstraints = @UniqueConstraint(columnNames = {"seccion_id", "curso_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeccionCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id")
    private Docente docente;

    /** Descripción opcional (p.ej. "Incluye taller de lectura"). */
    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String estado = "Activo";  // Activo | Inactivo
}
