package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.*;

/**
 * HU-27: bloque semanal en el que un docente, administrador o directivo atiende
 * reuniones (p.ej. martes 15:00 a 17:00, citas de 30 minutos).
 */
@Entity
@Table(name = "disponibilidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** 1 = Lunes ... 6 = Sábado. */
    @Column(nullable = false)
    private Integer dia;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /** Duración de cada cita en minutos (15, 20, 30, 45 o 60). */
    @Column(name = "duracion_minutos", nullable = false)
    @Builder.Default
    private Integer duracionMinutos = 30;

    /** PRESENCIAL | VIRTUAL */
    @Column(nullable = false, length = 15)
    @Builder.Default
    private String modalidad = "PRESENCIAL";

    /** Aula / oficina, o enlace de la reunión virtual. */
    @Column(length = 300)
    private String lugar;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
