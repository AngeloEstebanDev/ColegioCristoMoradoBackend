package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/** Pregunta de opción múltiple de un cuestionario. */
@Entity
@Table(name = "preguntas_cuestionario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreguntaCuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pregunta_opciones", joinColumns = @JoinColumn(name = "pregunta_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "texto", nullable = false, length = 500)
    @Builder.Default
    private List<String> opciones = new ArrayList<>();

    public static final String UNICA = "UNICA";
    public static final String MULTIPLE = "MULTIPLE";

    /** UNICA (una respuesta correcta) | MULTIPLE (varias). */
    @Column(nullable = false, length = 10, columnDefinition = "varchar(10) default 'UNICA'")
    @Builder.Default
    private String tipo = UNICA;

    /**
     * UNICA: índice (desde 0) de la opción correcta.
     * MULTIPLE: máscara de bits de las opciones correctas (opción i = 1 << i).
     */
    @Column(name = "respuesta_correcta", nullable = false)
    private Integer respuestaCorrecta;

    @Column(nullable = false)
    @Builder.Default
    private Double puntos = 1.0;
}
