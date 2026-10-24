package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

/** Curso del catálogo (Matemática, Comunicación, ...). */
@Entity
@Table(name = "cursos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    /** MATEMATICA, COMUNICACION, ... (ver CategoriaCurso). */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String categoria = "OTROS";

    /** Color de la categoría (se usa en secciones y horario). */
    @Column(nullable = false, length = 9)
    @Builder.Default
    private String color = "#4A1F52";

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String estado = "Activo";   // Inactivo = eliminado del catálogo (las secciones que ya lo tenían lo conservan)
}
