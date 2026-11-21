package com.CristoMorado_api.CristoMorado_api.dto;

public final class VerificacionRequests {

    private VerificacionRequests() {}

    public record CorreoRequest(String correo) {}

    public record CodigoRequest(String codigo) {}

    public record RecuperarRequest(String usuario) {}

    public record RestablecerRequest(String usuario, String codigo, String passwordNueva) {}
}
