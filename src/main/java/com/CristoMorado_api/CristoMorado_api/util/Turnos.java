package com.CristoMorado_api.CristoMorado_api.util;

import java.time.LocalTime;
import java.util.List;

/**
 * Horarios de la I.E. 2092 Cristo Morado (lunes a viernes, iguales en Primaria y Secundaria):
 *   Turno Mañana: 07:30 a 13:15, recreo 10:15 a 10:45
 *   Turno Tarde:  13:30 a 19:45, recreo 16:15 a 16:45
 * Secciones predefinidas (el turno lo fija la letra):
 *   Primaria A (1° a 6°) y Secundaria A: Mañana.  Primaria B (2° a 5°): Tarde.
 * Secciones adicionales ("Otros": C, D, ... o B en 1°, 6° y Secundaria): se elige Mañana o Tarde.
 * Los bloques del horario son de 15 minutos, contados desde el inicio del turno
 * (antes del recreo) y desde el fin del recreo (después).
 */
public final class Turnos {

    public record Turno(String nombre, LocalTime inicio, LocalTime fin, LocalTime recesoInicio, LocalTime recesoFin) {}

    public static final int MINUTOS_BLOQUE = 15;
    public static final String MANANA = "Mañana";
    public static final String TARDE = "Tarde";

    private static final Turno TURNO_MANANA = new Turno(MANANA,
            LocalTime.of(7, 30), LocalTime.of(13, 15), LocalTime.of(10, 15), LocalTime.of(10, 45));
    private static final Turno TURNO_TARDE = new Turno(TARDE,
            LocalTime.of(13, 30), LocalTime.of(19, 45), LocalTime.of(16, 15), LocalTime.of(16, 45));

    private Turnos() {}

    public static boolean esSecundaria(String grado) {
        return grado != null && grado.contains("Secundaria");
    }

    /** Secciones predefinidas de un grado: Primaria 2° a 5° -> A y B; Primaria 1° y 6° -> A; Secundaria -> A. */
    public static List<String> letrasPermitidas(String grado) {
        if (grado == null || esSecundaria(grado)) return List.of("A");
        return grado.startsWith("1°") || grado.startsWith("6°") ? List.of("A") : List.of("A", "B");
    }

    /** true si la letra es una de las secciones predefinidas del grado. */
    public static boolean esPredefinida(String grado, String letra) {
        return letra != null && letrasPermitidas(grado).contains(letra.trim().toUpperCase());
    }

    /** "Mañana" o "Tarde" escrito de cualquier forma -> el nombre oficial; otro valor -> null. */
    public static String normalizarTurno(String turno) {
        if (turno == null) return null;
        String t = turno.trim().toLowerCase().replace('ñ', 'n');
        if (t.equals("manana")) return MANANA;
        if (t.equals("tarde")) return TARDE;
        return null;
    }

    /** Horario de un turno ("Mañana"/"Tarde"). El grado se mantiene por compatibilidad (mismo horario en ambos niveles). */
    public static Turno deTurno(String grado, String turno) {
        String t = normalizarTurno(turno);
        if (t == null) return null;
        return TARDE.equals(t) ? TURNO_TARDE : TURNO_MANANA;
    }

    /** Turno de una sección predefinida (según la letra), o null si la letra no es predefinida. */
    public static Turno de(String grado, String letra) {
        if (!esPredefinida(grado, letra)) return null;
        if (esSecundaria(grado)) return TURNO_MANANA;
        return "B".equals(letra.trim().toUpperCase()) ? TURNO_TARDE : TURNO_MANANA;
    }

    /** Turno de una sección guardada: el predefinido de su letra o, si es adicional, el turno elegido al crearla. */
    public static Turno de(String grado, String letra, String turnoGuardado) {
        Turno t = de(grado, letra);
        return t != null ? t : deTurno(grado, turnoGuardado);
    }

    /** true si la hora cae en la grilla de 15 min del tramo (antes o después del recreo). */
    public static boolean enGrilla(Turno t, LocalTime h) {
        LocalTime base = h.isBefore(t.recesoFin()) ? t.inicio() : t.recesoFin();
        long min = java.time.Duration.between(base, h).toMinutes();
        return min >= 0 && min % MINUTOS_BLOQUE == 0;
    }
}
