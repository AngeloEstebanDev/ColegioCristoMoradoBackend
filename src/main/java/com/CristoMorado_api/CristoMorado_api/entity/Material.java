package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** HU-13: material que el docente publica en uno de sus cursos (texto, archivo o enlace), dentro de una carpeta. */
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

    /** Enlace opcional (video, página web, Drive...). */
    @Column(length = 500)
    private String enlace;

    /** Archivo opcional (tabla archivos). */
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

    /** TEXTO (mensaje con formato e imagen opcional) | ARCHIVO | ENLACE. null en materiales antiguos. */
    @Column(length = 20)
    private String tipo;

    /** false = oculto para alumnos y padres. */
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean visible = true;

    /** Carpeta del curso donde está (null = sin carpeta). */
    @Column(name = "carpeta_id")
    private Long carpetaId;
}
