package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "docentes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Docente {

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

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String correo;

    @Column(length = 200)
    private String direccion;

    /** Área o materia principal que dicta (ej. "Matemática", "Comunicación"). */
    @Column(length = 100)
    private String especialidad;

    @Column(name = "titulo_profesional", length = 150)
    private String tituloProfesional;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "Activo";

    /**
     * Vínculo (opcional) con la cuenta de acceso en `usuarios` (rol DOCENTE).
     * No es una relación JPA para mantenerlo simple: solo se guarda el id.
     * Se usa para que este docente pueda ver "su" información en las vistas
     * de Perfil, Mi Agenda, etc. cuando inicia sesión.
     */
    @Column(name = "usuario_id", unique = true)
    private Long usuarioId;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
