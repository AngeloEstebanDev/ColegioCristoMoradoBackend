package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Tabla puente entre Estudiante y PadreFamilia (relación muchos a muchos),
 * con el parentesco de cada vínculo (Padre / Madre / Apoderado).
 */
@Entity
@Table(name = "estudiante_familiar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteFamiliar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "padre_familia_id", nullable = false)
    private PadreFamilia padreFamilia;

    @Column(nullable = false, length = 20)
    private String parentesco; // Padre, Madre, Apoderado
}
