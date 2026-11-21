package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "materiales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_curso_id", nullable = false)
    private SeccionCurso seccionCurso;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 500)
    private String enlace;

    @Column(name = "archivo_id")
    private Long archivoId;

    @Column(name = "archivo_nombre", length = 255)
    private String archivoNombre;

    @Column(name = "archivo_tipo", length = 150)
    private String archivoTipo;

    @Column(name = "archivo_tamanio")
    private Long archivoTamanio;

    @Column(name = "autor_id")
    private Long autorId;

    @Column(name = "fecha_publicacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaPublicacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(length = 20)
    private String tipo;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean visible = true;

    @Column(name = "carpeta_id")
    private Long carpetaId;
}
