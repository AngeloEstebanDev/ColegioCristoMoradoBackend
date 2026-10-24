package com.CristoMorado_api.CristoMorado_api.util;

import java.util.List;

/**
 * Grados de la I.E.: 1° a 6° de Primaria y 1° a 5° de Secundaria.
 * El texto se guarda igual en estudiantes.grado y secciones.grado.
 */
public final class Grados {

    public static final List<String> TODOS = List.of(
            "1° Primaria", "2° Primaria", "3° Primaria", "4° Primaria", "5° Primaria", "6° Primaria",
            "1° Secundaria", "2° Secundaria", "3° Secundaria", "4° Secundaria", "5° Secundaria");

    private Grados() {}

    public static boolean valido(String grado) {
        return grado != null && TODOS.contains(grado.trim());
    }

    /** Posición para ordenar (los desconocidos van al final). */
    public static int orden(String grado) {
        int i = grado == null ? -1 : TODOS.indexOf(grado.trim());
        return i < 0 ? TODOS.size() : i;
    }
}
