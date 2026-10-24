package com.CristoMorado_api.CristoMorado_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String usuario;

    /**
     * Gmail personal del usuario. Es opcional: lo vincula el propio
     * usuario desde "Mi perfil" y queda verificado con un código.
     */
    @Column(unique = true, length = 150)
    private String correo;

    /** true cuando el usuario confirmó el correo con el código enviado. */
    @Column(name = "correo_verificado", nullable = false)
    @Builder.Default
    private Boolean correoVerificado = false;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String rol;

    @Column(nullable = false)
    @Builder.Default
    private Boolean estado = true;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /** Foto de perfil (data URL base64). La sube cada usuario en "Mi perfil". */
    @Column(columnDefinition = "TEXT")
    private String foto;

    /** Teléfono y dirección de las cuentas sin registro de persona (p.ej. el administrador). */
    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String direccion;
}
