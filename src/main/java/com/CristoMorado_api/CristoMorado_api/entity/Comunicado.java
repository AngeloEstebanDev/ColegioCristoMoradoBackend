package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(columnDefinition = "TEXT")
    private String imagen;

    @Column(nullable = false)
    @Builder.Default
    private Boolean anuncio = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "comunicado_destinatarios",
            joinColumns = @JoinColumn(name = "comunicado_id"))
    @Column(name = "grupo", nullable = false, length = 30)
    @Builder.Default
    private Set<String> destinatarios = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "comunicado_secciones",
            joinColumns = @JoinColumn(name = "comunicado_id"))
    @Column(name = "seccion_id", nullable = false)
    @Builder.Default
    private Set<Long> secciones = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
