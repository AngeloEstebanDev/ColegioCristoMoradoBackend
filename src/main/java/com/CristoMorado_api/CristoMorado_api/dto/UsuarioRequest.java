package com.CristoMorado_api.CristoMorado_api.dto;

/**
 * Cuentas creadas desde "Administrativo" (Administración y Directiva).
 * - Directiva: el usuario se genera como DIR + DNI (se envía `dni`).
 * - Administración: se escribe el `usuario` a mano.
 * El Gmail no se escribe aquí: lo vincula y verifica cada usuario en su perfil.
 */
public record UsuarioRequest(
        String nombre,
        String usuario,    // solo para Administración
        String dni,        // solo para Directiva
        String password,   // obligatorio al crear; null/vacío = no cambiar al editar
        String rol,
        Boolean estado
) {}
