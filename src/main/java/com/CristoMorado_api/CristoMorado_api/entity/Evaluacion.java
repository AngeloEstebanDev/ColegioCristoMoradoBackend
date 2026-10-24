package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * HU-16: columna del registro de notas de un curso en un periodo (examen, práctica...).
 * Las notas son vigesimales (0 a 20). El peso sirve para el promedio ponderado (HU-19).
 */
@Entity
@Table(name = "evaluaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_curso_id", nullable = false)
    private SeccionCurso seccionCurso;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** EXAMEN | PRACTICA | TAREA | CUESTIONARIO | PARTICIPACION | PROYECTO | OTRO */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tipo = "OTRO";

    /** 1 a 10. */
    @Column(nullable = false)
    @Builder.Default
    private Integer peso = 1;

    private LocalDate fecha;

    /** Actividad que la originó (si la nota viene de una tarea/cuestionario). */
    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
