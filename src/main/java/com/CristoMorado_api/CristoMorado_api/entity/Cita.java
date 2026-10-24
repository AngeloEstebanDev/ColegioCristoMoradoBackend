package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.*;

/**
 * HU-22/28/29/30: reunión entre un responsable (docente, administrador o directivo)
 * y un padre/apoderado.
 * - SOLICITUD_PADRE: la pide el padre en un horario libre -> PENDIENTE hasta que el responsable la confirme.
 * - PROGRAMADA: la programa el responsable -> CONFIRMADA.
 */
@Entity
@Table(name = "citas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cita {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String CANCELADA = "CANCELADA";
    public static final String REALIZADA = "REALIZADA";

    public static final String SOLICITUD_PADRE = "SOLICITUD_PADRE";
    public static final String PROGRAMADA = "PROGRAMADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Usuario responsable;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "padre_id", nullable = false)
    private Usuario padre;

    /** Alumno sobre el que trata la reunión (opcional). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false, length = 15)
    @Builder.Default
    private String modalidad = "PRESENCIAL";

    @Column(length = 300)
    private String lugar;

    @Column(nullable = false, length = 500)
    private String motivo;

    /** SOLICITUD_PADRE | PROGRAMADA */
    @Column(nullable = false, length = 20)
    private String origen;

    /** PENDIENTE | CONFIRMADA | RECHAZADA | CANCELADA | REALIZADA */
    @Column(nullable = false, length = 15)
    private String estado;

    /** Respuesta del responsable, motivo de cancelación/rechazo o de la reprogramación. */
    @Column(length = 500)
    private String observacion;

    /** Quién hizo el último cambio de estado (cancelar, reprogramar...). */
    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(nullable = false)
    @Builder.Default
    private Integer reprogramaciones = 0;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /** Descripción adicional que escribe quien crea la cita (opcional). */
    @Column(columnDefinition = "TEXT")
    private String detalle;
}
