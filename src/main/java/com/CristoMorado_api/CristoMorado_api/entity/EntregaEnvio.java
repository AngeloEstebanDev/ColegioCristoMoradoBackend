package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "entrega_envios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false)
    private Entrega entrega;

    @Column(nullable = false)
    private Integer numero;

    @Column(columnDefinition = "TEXT")
    private String texto;

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

    private Double puntaje;

    @Column(columnDefinition = "TEXT")
    private String respuestas;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean tardia = false;
}
