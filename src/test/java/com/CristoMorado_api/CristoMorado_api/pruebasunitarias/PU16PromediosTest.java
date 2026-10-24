package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.util.Calificacion;
import com.CristoMorado_api.CristoMorado_api.util.Calificacion.Ponderada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** PU-16 – HU-19: Cálculo de promedios (Épica 5, Sprint 5). Calificacion.ponderado (pesos de cada evaluación). */
@DisplayName("PU-16 – HU-19: Cálculo de promedios")
class PU16PromediosTest {

    @Test
    @DisplayName("PU-16.1 Notas 15 y 12 con pesos 30 % y 70 %: el promedio es 12,9")
    void c1_promedioPonderado() {
        Double promedio = Calificacion.ponderado(List.of(new Ponderada(15, 30), new Ponderada(12, 70)));

        assertEquals(12.9, promedio, 0.001);    // 15 × 0,30 + 12 × 0,70 = 4,5 + 8,4
    }

    @Test
    @DisplayName("PU-16.2 Notas iguales: el promedio es ese mismo valor")
    void c2_notasIguales() {
        assertEquals(14.0, Calificacion.ponderado(List.of(new Ponderada(14, 30), new Ponderada(14, 70))), 0.001);
        assertEquals(14.0, Calificacion.simple(List.of(14.0, 14.0, 14.0)), 0.001);
    }

    @Test
    @DisplayName("PU-16.3 Lista vacía de notas: no produce un resultado inválido (devuelve vacío / null)")
    void c3_listaVacia() {
        assertNull(Calificacion.ponderado(List.of()));
        assertNull(Calificacion.simple(List.of()));
        assertNull(Calificacion.literal(null), "Sin promedio no hay literal (AD, A, B o C)");
    }
}
