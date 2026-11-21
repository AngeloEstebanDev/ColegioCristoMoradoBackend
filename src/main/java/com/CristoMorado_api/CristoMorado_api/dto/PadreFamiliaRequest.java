package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

public record PadreFamiliaRequest(
        String nombreCompleto,
        String tipoDocumento,
        String dni,
        LocalDate fechaNacimiento,
        String telefono,
        String correo,
        String direccion,
        String estado,
        String password
) {}
