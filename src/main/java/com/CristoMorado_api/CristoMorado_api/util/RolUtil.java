package com.CristoMorado_api.CristoMorado_api.util;

/**
 * En la tabla `usuarios` el rol puede venir escrito de varias formas
 * ('ADMIN', 'admin', 'Administrador', 'docente', 'PROFESOR', 'alumno'...).
 * Esta clase lo traduce a una categoría fija para no repetir esos
 * "if" en cada servicio.
 */
public final class RolUtil {

    public enum Categoria { ADMINISTRATIVO, DOCENTE, ALUMNO, PADRE, OTRO }

    /** Grupos de destinatarios de un comunicado. */
    public static final String GRUPO_TODOS = "TODOS";
    public static final String GRUPO_ADMINISTRATIVO = "ADMINISTRATIVO";
    public static final String GRUPO_DOCENTES = "DOCENTES";
    public static final String GRUPO_ALUMNOS = "ALUMNOS";
    public static final String GRUPO_PADRES = "PADRES";

    private RolUtil() {}

    public static Categoria categoria(String rol) {
        String r = rol == null ? "" : rol.trim().toLowerCase();
        return switch (r) {
            case "admin", "administrador", "directivo", "director", "secretaria" -> Categoria.ADMINISTRATIVO;
            case "docente", "profesor" -> Categoria.DOCENTE;
            case "alumno", "estudiante" -> Categoria.ALUMNO;
            case "padre", "familiar", "apoderado" -> Categoria.PADRE;
            default -> Categoria.OTRO;
        };
    }

    public static boolean esAdministrativo(String rol) {
        return categoria(rol) == Categoria.ADMINISTRATIVO;
    }

    /** Grupo de comunicados al que pertenece un rol (null si ninguno). */
    public static String grupoComunicado(String rol) {
        return switch (categoria(rol)) {
            case ADMINISTRATIVO -> GRUPO_ADMINISTRATIVO;
            case DOCENTE -> GRUPO_DOCENTES;
            case ALUMNO -> GRUPO_ALUMNOS;
            case PADRE -> GRUPO_PADRES;
            default -> null;
        };
    }

    public static boolean esGrupoValido(String grupo) {
        return GRUPO_TODOS.equals(grupo) || GRUPO_ADMINISTRATIVO.equals(grupo)
                || GRUPO_DOCENTES.equals(grupo) || GRUPO_ALUMNOS.equals(grupo)
                || GRUPO_PADRES.equals(grupo);
    }

    /** Texto amigable del rol para mostrar en pantalla. */
    public static String etiqueta(String rol) {
        return switch (categoria(rol)) {
            case ADMINISTRATIVO -> "Administración";
            case DOCENTE -> "Docente";
            case ALUMNO -> "Estudiante";
            case PADRE -> "Padre";
            default -> rol == null ? "" : rol;
        };
    }
}
