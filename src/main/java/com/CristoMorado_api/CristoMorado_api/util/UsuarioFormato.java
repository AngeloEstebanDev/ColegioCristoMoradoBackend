package com.CristoMorado_api.CristoMorado_api.util;

import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;

/**
 * Nombre de usuario institucional: PREFIJO + DNI.
 *   ALU + DNI -> alumno
 *   DOC + DNI -> docente
 *   APO + DNI -> apoderado / padre de familia
 *   DIR + DNI -> directiva
 * Ejemplo: alumno con DNI 87654321  ->  ALU87654321
 */
public final class UsuarioFormato {

    public static final String ALUMNO = "ALU";
    public static final String DOCENTE = "DOC";
    public static final String APODERADO = "APO";
    public static final String DIRECTIVA = "DIR";

    private UsuarioFormato() {}

    public static String generar(String prefijo, String dni) {
        return prefijo + limpiarDni(dni);
    }

    /** Quita espacios y pasa a mayúsculas (DNI, carné de extranjería, pasaporte u otro). */
    public static String limpiarDni(String dni) {
        return dni == null ? "" : dni.trim().replace(" ", "").toUpperCase();
    }

    public static boolean dniValido(String dni) {
        return limpiarDni(dni).matches("[0-9A-Z]{6,15}");
    }

    /** Prefijo según el rol (null si el rol no usa DNI, como ADMIN). */
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

    /**
     * Nombre completo obligatorio (alumno, padre o docente). Quita espacios repetidos.
     * Reglas en Campos.nombreCompleto (solo letras, nombre y apellido, 5 a 100 caracteres).
     */
    public static String nombreCompleto(String nombre) {
        return Campos.nombreCompleto(nombre, "Escribe el nombre completo.");
    }

    /**
     * Correo de contacto opcional: vacío -> null; si se escribe, debe tener el formato usuario@dominio.ext
     * (ver Campos.correo).
     */
    public static String correoOpcional(String correo) {
        return Campos.correo(correo, "El correo");
    }
}
