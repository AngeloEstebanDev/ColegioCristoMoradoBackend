package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

public record PersonalRequest(
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String correo,
        String telefono,
        LocalDate fechaNacimiento,
        String cargo,
        String password
) {}
