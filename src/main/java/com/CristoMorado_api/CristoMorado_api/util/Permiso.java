package com.CristoMorado_api.CristoMorado_api.util;

import java.util.Arrays;
import java.util.List;

public enum Permiso {
    COMUNICADOS,
    REPORTES,
    ACADEMICO,
    USUARIOS,
    PERMISOS;

    public static List<String> todos() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }

    public static boolean esValido(String p) {
        return p != null && Arrays.stream(values()).anyMatch(v -> v.name().equals(p));
    }
}
