package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

/**
 * Datos del padre/apoderado. Los hijos NO se vinculan aquí:
 * eso se hace solo en "Vínculo Padre-Hijo" (/api/vinculos).
 */
public record PadreFamiliaRequest(
        String nombreCompleto,
        String tipoDocumento,  // DNI | CE | PASAPORTE | OTRO (vacío = DNI)
        String dni,            // número de documento
        LocalDate fechaNacimiento,
        String telefono,
        String correo,
        String direccion,
        String estado,
        String password // cuenta APO + documento: al crear vacío = el documento; al editar vacío = no cambiar
) {}
