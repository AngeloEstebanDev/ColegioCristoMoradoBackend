package com.CristoMorado_api.CristoMorado_api.util;

import java.util.List;

public final class Grados {

    public static final List<String> TODOS = List.of(
            "1° Primaria", "2° Primaria", "3° Primaria", "4° Primaria", "5° Primaria", "6° Primaria",
            "1° Secundaria", "2° Secundaria", "3° Secundaria", "4° Secundaria", "5° Secundaria");

    private Grados() {}

    public static boolean valido(String grado) {
        return grado != null && TODOS.contains(grado.trim());
    }

    public static int orden(String grado) {
        int i = grado == null ? -1 : TODOS.indexOf(grado.trim());
        return i < 0 ? TODOS.size() : i;
    }
}
