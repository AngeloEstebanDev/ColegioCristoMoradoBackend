package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "entregas", uniqueConstraints = @UniqueConstraint(columnNames = {"actividad_id", "estudiante_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrega {

    public static final String ENTREGADA = "ENTREGADA";
    public static final String CALIFICADA = "CALIFICADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "entrega_respuestas", joinColumns = @JoinColumn(name = "entrega_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "opcion", nullable = false)
    @Builder.Default
    private List<Integer> respuestas = new ArrayList<>();

    @Column(name = "fecha_entrega", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEntrega = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private Boolean tardia = false;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = ENTREGADA;

    private Double puntaje;

    @Column(name = "comentario_docente", columnDefinition = "TEXT")
    private String comentarioDocente;

    @Column(name = "fecha_calificacion")
    private LocalDateTime fechaCalificacion;

    @Column(nullable = false, columnDefinition = "integer default 1")
    @Builder.Default
    private Integer intentos = 1;

    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    @Builder.Default
    private List<EntregaEnvio> envios = new ArrayList<>();
}
