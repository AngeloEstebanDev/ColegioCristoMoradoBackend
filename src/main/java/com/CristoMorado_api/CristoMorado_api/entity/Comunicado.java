package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Comunicado institucional creado por la administración y dirigido a
 * uno o varios grupos (TODOS, ADMINISTRATIVO, DOCENTES, ALUMNOS, PADRES).
 */
@Entity
@Table(name = "comunicados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comunicado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    /** Cuerpo del comunicado (HTML simple generado por el editor). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    /** Imagen opcional (data URL base64 o enlace http). */
    @Column(columnDefinition = "TEXT")
    private String imagen;

    /** true = se muestra como anuncio grande al entrar a Comunicados. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean anuncio = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "comunicado_destinatarios",
            joinColumns = @JoinColumn(name = "comunicado_id"))
    @Column(name = "grupo", nullable = false, length = 30)
    @Builder.Default
    private Set<String> destinatarios = new LinkedHashSet<>();

    /**
     * HU-25: secciones a las que se limita el comunicado (ids de `secciones`).
     * Vacío = todo el colegio (según los grupos). Los comunicados de un docente
     * siempre van a sus secciones: solo los ven los alumnos de esas secciones
     * y/o sus padres.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "comunicado_secciones",
            joinColumns = @JoinColumn(name = "comunicado_id"))
    @Column(name = "seccion_id", nullable = false)
    @Builder.Default
    private Set<Long> secciones = new LinkedHashSet<>();

    /** Quién lo creó (null si su cuenta fue eliminada). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
