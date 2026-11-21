package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

public record DocenteRequest(
        String nombreCompleto,
        String tipoDocumento,
        String dni,
        String fotoUrl,
        LocalDate fechaNacimiento,
        String genero,
        String telefono,
        String correo,
        String direccion,
        String especialidad,
        String tituloProfesional,
        LocalDate fechaIngreso,
        String estado,
        String password
) {}
