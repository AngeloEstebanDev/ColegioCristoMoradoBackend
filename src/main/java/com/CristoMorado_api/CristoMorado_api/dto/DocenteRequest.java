package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

/**
 * DTO de entrada DocenteRequest: datos que el frontend envía al backend (en JSON) sobre los docentes.
 * Un DTO (Data Transfer Object) es solo un "paquete" de datos: no tiene lógica ni se guarda
 * directamente.
 */
public record DocenteRequest(
        String nombreCompleto,
        String tipoDocumento,  // DNI | CE | PASAPORTE | OTRO (vacío = DNI)
        String dni,            // número de documento
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
        String password // cuenta DOC + DNI: al crear vacío = el DNI; al editar vacío = no cambiar
) {}
