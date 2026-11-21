package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/** HU-18: asistencia de un alumno a un curso en una fecha. */
@Entity
@Table(name = "asistencias",
        uniqueConstraints = @UniqueConstraint(columnNames = {"seccion_curso_id", "estudiante_id", "fecha"}))
/**
 * Entidad Asistencia: representa la tabla «asistencias» de la base de datos (la asistencia de los
 * alumnos en cada clase). Cada atributo es una columna y cada objeto es una fila. Hibernate (JPA) se
 * encarga de leer y guardar estos datos. Lombok genera los getters, setters y el builder para no
 * escribirlos a mano.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asistencia {

    public static final String PRESENTE = "PRESENTE";
    public static final String TARDANZA = "TARDANZA";
    public static final String FALTA = "FALTA";
    public static final String JUSTIFICADA = "JUSTIFICADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_curso_id", nullable = false)
    private SeccionCurso seccionCurso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @Column(nullable = false)
    private LocalDate fecha;

    /** PRESENTE | TARDANZA | FALTA | JUSTIFICADA */
    @Column(nullable = false, length = 15)
    private String estado;

    @Column(length = 300)
    private String observacion;

    @Column(name = "registrado_por")
    private Long registradoPor;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
