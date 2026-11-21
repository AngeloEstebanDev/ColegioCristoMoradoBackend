package com.CristoMorado_api.CristoMorado_api.util;

import com.CristoMorado_api.CristoMorado_api.dto.NotaDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public final class Calificacion {

    public static final double MINIMA = 0;
    public static final double MAXIMA = 20;
    public static final int APROBATORIA = 11;

    public static final Set<String> TIPOS = Set.of(
            "EXAMEN", "PRACTICA", "TAREA", "CUESTIONARIO", "ACTIVIDAD", "PARTICIPACION", "PROYECTO", "OTRO");

    private Calificacion() {}

    public record Ponderada(double valor, int peso) {}

    public static Double ponderado(Collection<Ponderada> notas) {
        double suma = 0;
        int pesos = 0;
        for (Ponderada n : notas) {
            int p = Math.max(1, n.peso());
            suma += n.valor() * p;
            pesos += p;
        }
        return pesos == 0 ? null : dosDecimales(suma / pesos);
    }

    public static Double simple(Collection<Double> valores) {
        double suma = 0;
        int n = 0;
        for (Double v : valores) {
            if (v != null) { suma += v; n++; }
        }
        return n == 0 ? null : dosDecimales(suma / n);
    }

    public static Double dosDecimales(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static Integer redondear(Double v) {
        return v == null ? null : BigDecimal.valueOf(v).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public static String literal(Double v) {
        Integer r = redondear(v);
        if (r == null) return null;
        if (r >= 18) return "AD";
        if (r >= 14) return "A";
        if (r >= APROBATORIA) return "B";
        return "C";
    }

    public static boolean aprobado(Double v) {
        Integer r = redondear(v);
        return r != null && r >= APROBATORIA;
    }

    public static NotaDto.Promedio promedio(Double v) {
        return new NotaDto.Promedio(v, redondear(v), literal(v));
    }

    public static Double validar(Double v) {
        Objects.requireNonNull(v);
        if (v.isNaN() || v < MINIMA || v > MAXIMA) {
            throw new com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException(
                    "Las notas van de 0 a 20.");
        }
        return dosDecimales(v);
    }
}
