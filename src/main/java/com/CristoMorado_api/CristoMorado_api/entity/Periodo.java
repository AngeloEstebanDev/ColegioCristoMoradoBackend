package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String nombre;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer bimestre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = false;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public boolean vigenteEn(LocalDate fecha) {
        return fecha != null && fechaInicio != null && fechaFin != null
                && !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }
}
