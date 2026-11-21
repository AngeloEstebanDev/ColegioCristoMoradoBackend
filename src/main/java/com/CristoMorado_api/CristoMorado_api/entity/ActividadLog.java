package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "actividad_log", indexes = @Index(name = "actividad_log_fecha_idx", columnList = "fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActividadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 150)
    private String usuario;

    @Column(length = 30)
    private String rol;

    @Column(nullable = false, length = 160)
    private String accion;

    @Column(nullable = false, length = 60)
    private String modulo;

    @Column(nullable = false, length = 20)
    private String estado;
}
