package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String categoria = "OTROS";

    @Column(nullable = false, length = 9)
    @Builder.Default
    private String color = "#4A1F52";

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String estado = "Activo";
}
