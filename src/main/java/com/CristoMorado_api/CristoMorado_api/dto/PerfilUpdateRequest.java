package com.CristoMorado_api.CristoMorado_api.dto;

import java.time.LocalDate;

public record PerfilUpdateRequest(
        String nombre,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        String telefono,
        String direccion,
        LocalDate fechaNacimiento,
        String genero,
        String correoContacto,
        String especialidad,
        String tituloProfesional,
        String foto,
        Boolean quitarFoto,
        String fotoUrl
) {}
