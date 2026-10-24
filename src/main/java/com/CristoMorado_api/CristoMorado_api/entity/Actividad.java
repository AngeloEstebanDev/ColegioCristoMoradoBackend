package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * HU-14: tarea, cuestionario o actividad que el docente crea en uno de sus cursos.
 * Al crearla se genera una evaluación en el registro de notas del periodo activo;
 * cuando el docente califica una entrega, la nota pasa sola a ese registro (escala 0-20).
 */
@Entity
@Table(name = "actividades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actividad {

    public static final String TAREA = "TAREA";
    public static final String CUESTIONARIO = "CUESTIONARIO";
    public static final String ACTIVIDAD = "ACTIVIDAD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_curso_id", nullable = false)
    private SeccionCurso seccionCurso;

    /** TAREA | CUESTIONARIO | ACTIVIDAD */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String instrucciones;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    /** Puntaje máximo (para tareas y actividades). En cuestionarios es la suma de los puntos de las preguntas. */
    @Column(name = "puntaje_maximo", nullable = false)
    @Builder.Default
    private Double puntajeMaximo = 20.0;

    /** true = se aceptan entregas después de la fecha límite (quedan marcadas como tardías). */
    @Column(name = "permite_tardia", nullable = false)
    @Builder.Default
    private Boolean permiteTardia = false;

    @Column(name = "archivo_id")
    private Long archivoId;

    @Column(name = "archivo_nombre", length = 255)
    private String archivoNombre;

    @Column(name = "archivo_tipo", length = 150)
    private String archivoTipo;

    @Column(name = "archivo_tamanio")
    private Long archivoTamanio;

    @Column(length = 500)
    private String enlace;

    /** Evaluación del registro de notas vinculada (null si no había periodo activo). */
    @Column(name = "evaluacion_id")
    private Long evaluacionId;

    @Column(name = "autor_id")
    private Long autorId;

    @Column(name = "fecha_publicacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaPublicacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /** false = oculta para alumnos y padres (y no cuenta en Participación). */
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean visible = true;

    /** Cuántas veces puede enviar el alumno (1 por defecto). */
    @Column(name = "intentos_permitidos", nullable = false, columnDefinition = "integer default 1")
    @Builder.Default
    private Integer intentosPermitidos = 1;

    /**
     * Formatos de archivo que acepta la entrega, separados por coma: PDF, WORD, EXCEL, PPT, IMAGEN.
     * null o vacío = todos esos formatos.
     */
    @Column(name = "formatos_permitidos", length = 100)
    private String formatosPermitidos;

    /** Carpeta del curso donde está (null = sin carpeta). */
    @Column(name = "carpeta_id")
    private Long carpetaId;

    /** Solo cuestionarios: preguntas de opción múltiple (se califican solas). */
    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<PreguntaCuestionario> preguntas = new ArrayList<>();
}
