package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.*;

/**
 * Asistencia "generada" por el docente para una clase (un día del curso).
 * Solo se genera dentro de la hora de clase del horario; luego se marca (toma lista).
 */
@Entity
@Table(name = "sesiones_asistencia",
        uniqueConstraints = @UniqueConstraint(columnNames = {"seccion_curso_id", "fecha"}))
/**
 * Entidad SesionAsistencia: representa la tabla «sesiones_asistencia» de la base de datos (las clases
 * (sesiones) en las que se toma asistencia). Cada atributo es una columna y cada objeto es una fila.
 * Hibernate (JPA) se encarga de leer y guardar estos datos. Lombok genera los getters, setters y el
 * builder para no escribirlos a mano.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_curso_id", nullable = false)
    private SeccionCurso seccionCurso;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Hora de inicio de la clase de ese día. */
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "generada_por")
    private Long generadaPor;

    @Column(name = "fecha_generacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaGeneracion = LocalDateTime.now();
}
