package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Periodo académico (bimestre) del año escolar.
 * Está activo solo mientras la fecha de hoy está dentro de su rango (fecha inicio y fin incluidas).
 */
@Entity
@Table(name = "periodos", uniqueConstraints = @UniqueConstraint(columnNames = {"anio", "bimestre"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Periodo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;          // "I Bimestre 2026"

    @Column(nullable = false)
    private Integer anio;

    /** 1 a 4. */
    @Column(nullable = false)
    private Integer bimestre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /** Copia guardada de vigenteEn(hoy); se actualiza sola (PeriodoService.sincronizarActivo). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = false;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /** true si la fecha está dentro del rango del periodo (inicio y fin incluidos). */
    public boolean vigenteEn(LocalDate fecha) {
        return fecha != null && fechaInicio != null && fechaFin != null
                && !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }
}
