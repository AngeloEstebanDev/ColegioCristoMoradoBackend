package com.CristoMorado_api.CristoMorado_api.util;

import java.util.Arrays;
import java.util.List;

/**
 * Permisos que el administrador puede dar o quitar a cada miembro de la
 * Directiva / Personal Staff. El administrador los tiene todos siempre.
 */
public enum Permiso {
    /** Publicar comunicados institucionales. */
    COMUNICADOS,
    /** Ver reportes globales de todos los alumnos. */
    REPORTES,
    /** Grados y Secciones, Catálogo de Cursos, Periodos, Vínculo Padre-Hijo. */
    ACADEMICO,
    /** Crear y editar cuentas de alumnos, padres, docentes y personal. */
    USUARIOS,
    /** Asignar o quitar permisos al personal de la Directiva (p.ej. el Director). */
    PERMISOS;

    public static List<String> todos() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }

    public static boolean esValido(String p) {
        return p != null && Arrays.stream(values()).anyMatch(v -> v.name().equals(p));
    }
}
