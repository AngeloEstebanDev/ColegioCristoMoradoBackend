package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Archivo subido al sistema (materiales del docente, adjuntos de actividades
 * y entregas de los alumnos). El contenido se guarda en PostgreSQL (bytea).
 * Las demás tablas solo guardan el id del archivo, así los listados no cargan
 * los bytes; se descargan con GET /api/archivos/{id}.
 */
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

    /** Tamaño en bytes. */
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
