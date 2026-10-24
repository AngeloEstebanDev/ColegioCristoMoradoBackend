package com.CristoMorado_api.CristoMorado_api.dto;

/** Peticiones pequeñas de los flujos de verificación por Gmail. */
public final class VerificacionRequests {

    private VerificacionRequests() {}

    /** Vincular Gmail: primero se envía el correo, luego el código. */
    public record CorreoRequest(String correo) {}

    public record CodigoRequest(String codigo) {}

    /** "¿Olvidaste tu contraseña?" (login): pedir código. */
    public record RecuperarRequest(String usuario) {}

    /** "¿Olvidaste tu contraseña?" (login): nueva contraseña con el código. */
    public record RestablecerRequest(String usuario, String codigo, String passwordNueva) {}
}
