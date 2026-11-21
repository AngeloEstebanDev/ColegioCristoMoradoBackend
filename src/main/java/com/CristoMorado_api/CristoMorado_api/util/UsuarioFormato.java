package com.CristoMorado_api.CristoMorado_api.util;

import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;

public final class UsuarioFormato {

    public static final String ALUMNO = "ALU";
    public static final String DOCENTE = "DOC";
    public static final String APODERADO = "APO";
    public static final String DIRECTIVA = "DIR";

    private UsuarioFormato() {}

    public static String generar(String prefijo, String dni) {
        return prefijo + limpiarDni(dni);
    }

    public static String limpiarDni(String dni) {
        return dni == null ? "" : dni.trim().replace(" ", "").toUpperCase();
    }

    public static boolean dniValido(String dni) {
        return limpiarDni(dni).matches("[0-9A-Z]{6,15}");
    }

    public static String prefijoDeRol(String rol) {
        String r = rol == null ? "" : rol.trim().toLowerCase();
        return switch (r) {
            case "alumno", "estudiante" -> ALUMNO;
            case "docente", "profesor" -> DOCENTE;
            case "padre", "apoderado", "familiar" -> APODERADO;
            case "directivo", "directiva", "director" -> DIRECTIVA;
            default -> null;
        };
    }

    public static String nombreCompleto(String nombre) {
        return Campos.nombreCompleto(nombre, "Escribe el nombre completo.");
    }

    public static String correoOpcional(String correo) {
        return Campos.correo(correo, "El correo");
    }
}
