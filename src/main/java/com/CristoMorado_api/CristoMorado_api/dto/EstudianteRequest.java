package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

public record EstudianteRequest(
        String nombreCompleto,
        String tipoDocumento,
        String dni,
        String fotoUrl,
        LocalDate fechaNacimiento,
        String genero,
        String nacionalidad,
        String direccion,
        String telefono,
        String grado,
        String seccion,
        Integer anioIngreso,
        String estado,
        String password
) {}
