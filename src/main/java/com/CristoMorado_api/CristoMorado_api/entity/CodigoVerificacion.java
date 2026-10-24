package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Código de 6 dígitos enviado al Gmail del usuario para:
 * - VINCULAR_CORREO: confirmar que el Gmail es suyo.
 * - CAMBIO_PASSWORD: cambiar la contraseña desde Configuración.
 * - RECUPERAR_PASSWORD: restablecerla desde el login ("¿Olvidaste tu contraseña?").
 * Solo se guarda el hash del código, nunca el código en texto plano.
 */
@Entity
@Table(name = "codigos_verificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoVerificacion {

    public static final String VINCULAR_CORREO = "VINCULAR_CORREO";
    public static final String CAMBIO_PASSWORD = "CAMBIO_PASSWORD";
    public static final String RECUPERAR_PASSWORD = "RECUPERAR_PASSWORD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 30)
    private String tipo;

    /** Correo al que se envió el código. */
    @Column(nullable = false, length = 150)
    private String correo;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
