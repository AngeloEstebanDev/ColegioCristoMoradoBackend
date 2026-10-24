package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estudiantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    /** DNI, CE (carné de extranjería), PASAPORTE u OTRO. El número va en "dni". */
    @Column(name = "tipo_documento", nullable = false, length = 10)
    @Builder.Default
    private String tipoDocumento = "DNI";

    @Column(nullable = false, unique = true, length = 15)
    private String dni;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 20)
    private String genero;

    @Column(length = 60)
    private String nacionalidad;

    @Column(length = 200)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    /** "1° Primaria" ... "5° Secundaria". Vacío = sin matrícula. */
    @Column(length = 20)
    private String grado;

    /** Letra de la sección (A, B, ...). Vacío = sin matrícula. */
    @Column(length = 5)
    private String seccion;

    @Column(name = "anio_ingreso")
    private Integer anioIngreso;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "Activo";

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /**
     * Cuenta de acceso del alumno (tabla `usuarios`, rol ALUMNO).
     * El correo y la contraseña con los que inicia sesión viven en esa
     * cuenta, así que cualquier cambio (desde Alumnos, Administrativo o
     * Configuración) se guarda en un solo lugar de la base de datos.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EstudianteFamiliar> vinculosFamiliares = new ArrayList<>();
}
