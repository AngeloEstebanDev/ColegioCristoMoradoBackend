package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "carpetas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carpeta {

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

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean visible = true;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
