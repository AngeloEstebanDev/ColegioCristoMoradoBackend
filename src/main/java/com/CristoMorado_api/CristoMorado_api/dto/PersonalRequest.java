package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

/**
 * DTO de entrada PersonalRequest: datos que el frontend envía al backend (en JSON) sobre la directiva
 * y el personal administrativo. Un DTO (Data Transfer Object) es solo un "paquete" de datos: no tiene
 * lógica ni se guarda directamente.
 */
public record PersonalRequest(
        String tipoDocumento,     // DNI | CE | PASAPORTE | OTRO
        String numeroDocumento,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String correo,            // correo de contacto
        String telefono,
        LocalDate fechaNacimiento,
        String cargo,
        String password           // al crear: vacío = el N.º de documento; al editar: vacío = no cambiar
) {}
