package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Personal Staff / Directiva: director, subdirector, secretaría, auxiliares,
 * etc. Cada uno tiene su cuenta (usuario DIR + N.º de documento, rol DIRECTIVO)
 * y los permisos que el administrador le asigne.
 */
@Entity
@Table(name = "personal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DNI, CE (carné de extranjería), PASAPORTE u OTRO. */
    @Column(name = "tipo_documento", nullable = false, length = 10)
    @Builder.Default
    private String tipoDocumento = "DNI";

    @Column(name = "numero_documento", nullable = false, unique = true, length = 15)
    private String numeroDocumento;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellido_paterno", nullable = false, length = 60)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 60)
    private String apellidoMaterno;

    /** Correo de contacto (el Gmail de inicio de sesión lo vincula cada uno en su perfil). */
    @Column(length = 150)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 200)
    private String direccion;

    /** Cargo o puesto institucional (Director, Secretaria, Auxiliar de Educación...). */
    @Column(length = 100)
    private String cargo;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "Activo";

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    /** Permisos habilitados por el administrador (ver util.Permiso). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "personal_permisos", joinColumns = @JoinColumn(name = "personal_id"))
    @Column(name = "permiso", nullable = false, length = 30)
    @Builder.Default
    private Set<String> permisos = new LinkedHashSet<>();

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /** "Nombres ApellidoPaterno ApellidoMaterno" */
    public String nombreCompleto() {
        return (nombres + " " + apellidoPaterno + " " + (apellidoMaterno == null ? "" : apellidoMaterno)).trim()
                .replaceAll("\\s+", " ");
    }
}
