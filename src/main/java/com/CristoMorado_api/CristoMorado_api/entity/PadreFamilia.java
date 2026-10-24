package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "padres_familia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PadreFamilia {

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

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 150)
    private String correo;

    @Column(length = 200)
    private String direccion;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "Activo";

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /** Cuenta de acceso del padre/apoderado (usuario APO + DNI). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @OneToMany(mappedBy = "padreFamilia", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EstudianteFamiliar> vinculosFamiliares = new ArrayList<>();
}
