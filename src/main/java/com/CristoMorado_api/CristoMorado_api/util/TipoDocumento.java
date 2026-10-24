package com.CristoMorado_api.CristoMorado_api.util;

import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;

import java.util.List;

/**
 * Tipos de documento de identidad aceptados para alumnos, docentes, padres y personal.
 *   DNI        -> 8 dígitos
 *   CE         -> carné de extranjería: 9 a 12 números o letras
 *   PASAPORTE  -> 6 a 12 números o letras
 *   OTRO       -> otro documento (PTP, cédula, etc.): 6 a 15 números o letras
 * El usuario se arma igual para todos: PREFIJO + número (ALU, DOC, APO o DIR).
 */
public final class TipoDocumento {

    public static final String DNI = "DNI";
    public static final String CE = "CE";
    public static final String PASAPORTE = "PASAPORTE";
    public static final String OTRO = "OTRO";
    public static final List<String> TODOS = List.of(DNI, CE, PASAPORTE, OTRO);

    private TipoDocumento() {}

    /** Vacío = DNI. Rechaza tipos desconocidos. */
    public static String normalizar(String tipo) {
        if (tipo == null || tipo.isBlank()) return DNI;
        String t = tipo.trim().toUpperCase();
        if (t.equals("PAS")) t = PASAPORTE;
        if (!TODOS.contains(t)) {
            throw new DatosInvalidosException("Tipo de documento no válido. Usa DNI, Carné de Extranjería, Pasaporte u Otro.");
        }
        return t;
    }

    /** Valida el número según el tipo y lo devuelve limpio (sin espacios, en mayúsculas). */
    public static String validarNumero(String tipo, String numero) {
        String t = normalizar(tipo);
        String n = UsuarioFormato.limpiarDni(numero);
        switch (t) {
            case DNI -> {
                if (!n.matches("\\d{8}")) throw new DatosInvalidosException("El DNI debe tener 8 dígitos.");
            }
            case CE -> {
                if (!n.matches("[0-9A-Z]{9,12}"))
                    throw new DatosInvalidosException("El carné de extranjería debe tener entre 9 y 12 caracteres (números o letras).");
            }
            case PASAPORTE -> {
                if (!n.matches("[0-9A-Z]{6,12}"))
                    throw new DatosInvalidosException("El pasaporte debe tener entre 6 y 12 caracteres (números o letras).");
            }
            default -> {
                if (!n.matches("[0-9A-Z]{6,15}"))
                    throw new DatosInvalidosException("El número de documento debe tener entre 6 y 15 caracteres (números o letras).");
            }
        }
        return n;
    }
}
