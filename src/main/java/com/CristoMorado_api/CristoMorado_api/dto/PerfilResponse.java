package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDateTime;

/**
 * "Mi perfil": datos de la cuenta + el registro completo de la persona
 * según su rol (solo uno de docente / estudiante / padre viene lleno).
 */
public record PerfilResponse(
        Long usuarioId,
        String nombre,
        String usuario,
        String correo,             // Gmail vinculado (puede ser null)
        Boolean correoVerificado,
        String rol,
        Boolean estado,
        LocalDateTime fechaRegistro,
        Boolean puedeEditar,       // false para alumnos (sus datos los cambia la administración)
        String foto,               // foto de perfil (data URL) o null
        String telefono,           // del registro de la persona (o de la cuenta, si no tiene)
        String direccion,
        DocenteResponse docente,
        EstudianteResponse estudiante,
        PadreFamiliaResponse padre,
        PersonalResponse personal,         // Directiva / Personal Staff
        java.util.List<String> permisos    // permisos del panel (admin: todos)
) {}
