package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mensajes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "remitente_id")
    private Usuario remitente;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "fecha_envio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(name = "respuesta_a_id")
    private Long respuestaAId;

    @Column(name = "eliminado_remitente", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean eliminadoRemitente = false;

    @Column(name = "aviso_sistema", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean avisoSistema = false;

    @OneToMany(mappedBy = "mensaje", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MensajeDestinatario> destinatarios = new ArrayList<>();
}
