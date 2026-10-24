package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

/**
 * Datos editables de "Mi perfil" (todos los roles menos alumnos).
 * Campos null = no se cambian. El Gmail NO se cambia aquí: se vincula con
 * código en /api/perfil/{id}/correo/codigo y /correo/verificar.
 * El número de documento tampoco (de él sale el nombre de usuario).
 */
public record PerfilUpdateRequest(
        String nombre,               // docente, padre y administrador
        String nombres,              // directiva: nombres y apellidos por separado
        String apellidoPaterno,
        String apellidoMaterno,
        String telefono,
        String direccion,
        LocalDate fechaNacimiento,
        String genero,               // docente
        String correoContacto,       // correo de contacto (no es el Gmail de inicio de sesión)
        String especialidad,         // docente
        String tituloProfesional,    // docente
        String foto,                 // data URL de la imagen (opcional)
        Boolean quitarFoto,
        String fotoUrl               // compatibilidad: ya no se usa
) {}
