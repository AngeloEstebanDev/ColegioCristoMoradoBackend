package com.CristoMorado_api.CristoMorado_api.util;

import java.time.LocalTime;
import java.util.List;

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

    public static List<String> letrasPermitidas(String grado) {
        if (grado == null || esSecundaria(grado)) return List.of("A");
        return grado.startsWith("1°") || grado.startsWith("6°") ? List.of("A") : List.of("A", "B");
    }

    public static boolean esPredefinida(String grado, String letra) {
        return letra != null && letrasPermitidas(grado).contains(letra.trim().toUpperCase());
    }

    public static String normalizarTurno(String turno) {
        if (turno == null) return null;
        String t = turno.trim().toLowerCase().replace('ñ', 'n');
        if (t.equals("manana")) return MANANA;
        if (t.equals("tarde")) return TARDE;
        return null;
    }

    public static Turno deTurno(String grado, String turno) {
        String t = normalizarTurno(turno);
        if (t == null) return null;
        return TARDE.equals(t) ? TURNO_TARDE : TURNO_MANANA;
    }

    public static Turno de(String grado, String letra) {
        if (!esPredefinida(grado, letra)) return null;
        if (esSecundaria(grado)) return TURNO_MANANA;
        return "B".equals(letra.trim().toUpperCase()) ? TURNO_TARDE : TURNO_MANANA;
    }

    public static Turno de(String grado, String letra, String turnoGuardado) {
        Turno t = de(grado, letra);
        return t != null ? t : deTurno(grado, turnoGuardado);
    }

    public static boolean enGrilla(Turno t, LocalTime h) {
        LocalTime base = h.isBefore(t.recesoFin()) ? t.inicio() : t.recesoFin();
        long min = java.time.Duration.between(base, h).toMinutes();
        return min >= 0 && min % MINUTOS_BLOQUE == 0;
    }
}
