package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

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

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tipo = "OTRO";

    @Column(nullable = false)
    @Builder.Default
    private Integer peso = 1;

    private LocalDate fecha;

    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
