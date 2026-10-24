package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

public record EstudianteRequest(
        String nombreCompleto,
        String tipoDocumento,  // DNI | CE | PASAPORTE | OTRO (vacío = DNI)
        String dni,            // número de documento
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
        // --- Cuenta de acceso (usuario = ALU + DNI, se genera solo) ---
        String password   // al crear: vacío = el DNI; al editar: vacío = no cambiar
) {}
