package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "archivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(name = "tipo_mime", nullable = false, length = 150)
    private String tipoMime;

    @Column(nullable = false)
    private Long tamanio;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] datos;

    @Column(name = "subido_por")
    private Long subidoPor;

    @Column(name = "fecha_subida", nullable = false)
    @Builder.Default
    private LocalDateTime fechaSubida = LocalDateTime.now();
}
