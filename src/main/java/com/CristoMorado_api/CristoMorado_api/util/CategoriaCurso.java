package com.CristoMorado_api.CristoMorado_api.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Categorías del Catálogo de Cursos. Cada curso toma el color de su categoría
 * (se usa en las secciones y en el horario).
 */
public final class CategoriaCurso {

    public record Info(String clave, String nombre, String color) {}

    public static final Map<String, Info> TODAS = new LinkedHashMap<>();
    static {
        agregar("MATEMATICA", "Matemática", "#2563EB");
        agregar("COMUNICACION", "Comunicación y Literatura", "#D97706");
        agregar("CIENCIA", "Ciencia y Tecnología", "#059669");
        agregar("SOCIALES", "Ciencias Sociales y Ciudadanía", "#DC2626");
        agregar("IDIOMAS", "Idiomas Extranjeros", "#7C3AED");
        agregar("ARTE", "Arte y Expresión Corporal", "#DB2777");
        agregar("FORMACION", "Formación Personal, Tecnológica y Espiritual", "#0D9488");
        agregar("COMPUTACION", "Computación e Informática", "#475569");
        agregar("OTROS", "Otros", "#6B7280");
    }

    private static void agregar(String clave, String nombre, String color) {
        TODAS.put(clave, new Info(clave, nombre, color));
    }

    private CategoriaCurso() {}

    public static Info de(String clave) {
        return clave == null ? null : TODAS.get(clave.trim().toUpperCase());
    }

    /** Posición para ordenar el catálogo por categoría. */
    public static int orden(String clave) {
        int i = 0;
        for (String k : TODAS.keySet()) {
            if (k.equals(clave)) return i;
            i++;
        }
        return i;
    }
}
