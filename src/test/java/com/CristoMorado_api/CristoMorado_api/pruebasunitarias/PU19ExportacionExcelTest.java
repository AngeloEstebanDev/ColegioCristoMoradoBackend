package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.ReporteDto;
import com.CristoMorado_api.CristoMorado_api.util.ExcelXlsx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** PU-19 – HU-31: Exportación de reportes a Excel (Épica 5, Sprint 5). ExcelXlsx.generar. */
@DisplayName("PU-19 – HU-31: Exportación a Excel")
class PU19ExportacionExcelTest {

    private static ReporteDto.Tabla tabla(List<List<Object>> filas) {
        return new ReporteDto.Tabla("rendimiento", "Rendimiento académico - 5° Primaria A", "I Bimestre 2026",
                List.of("N°", "Alumno", "Promedio"), filas,
                List.of(new ReporteDto.Indicador("Alumnos", String.valueOf(filas.size()), null)));
    }

    /** Abre el .xlsx (es un ZIP) y devuelve cada archivo interno como texto. */
    private static Map<String, String> abrir(byte[] xlsx) throws IOException {
        Map<String, String> partes = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx), StandardCharsets.UTF_8)) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null; ) {
                partes.put(e.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return partes;
    }

    @Test
    @DisplayName("PU-19.1 Datos válidos: se genera un archivo .xlsx válido")
    void c1_archivoValido() throws IOException {
        byte[] xlsx = ExcelXlsx.generar(tabla(List.of(
                List.of(1, "Juan Pérez García", 15.0),
                List.of(2, "María López Ríos", 10.5))));

        assertTrue(xlsx.length > 0);
        assertEquals('P', (char) xlsx[0]);
        assertEquals('K', (char) xlsx[1]);
        Map<String, String> partes = abrir(xlsx);
        for (String parte : List.of("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/styles.xml", "xl/worksheets/sheet1.xml")) {
            assertTrue(partes.containsKey(parte), "Falta la parte " + parte);
        }
        assertTrue(partes.get("[Content_Types].xml").contains("spreadsheetml.sheet.main+xml"));
    }

    @Test
    @DisplayName("PU-19.2 Lista vacía: el archivo se genera con título y encabezados, sin filas de datos")
    void c2_listaVacia() throws IOException {
        byte[] xlsx = assertDoesNotThrow(() -> ExcelXlsx.generar(tabla(List.of())));

        String hoja = abrir(xlsx).get("xl/worksheets/sheet1.xml");
        assertTrue(hoja.contains("Rendimiento académico - 5° Primaria A"));
        assertTrue(hoja.contains("<row r=\"5\""), "Debe tener la fila de encabezados");
        assertFalse(hoja.contains("<row r=\"6\""), "No debe tener filas de datos");
        assertFalse(hoja.contains("<autoFilter"), "Sin datos no se agrega filtro");
    }

    @Test
    @DisplayName("PU-19.3 Encabezados y valores del archivo corresponden a los datos exportados")
    void c3_encabezadosYValores() throws IOException {
        String hoja = abrir(ExcelXlsx.generar(tabla(List.of(
                List.of(1, "Juan Pérez García", 15.0),
                List.of(2, "María López Ríos", 10.5))))).get("xl/worksheets/sheet1.xml");

        assertTrue(hoja.contains("<c r=\"A5\" s=\"2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">N°</t>"));
        assertTrue(hoja.contains("<c r=\"B5\" s=\"2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Alumno</t>"));
        assertTrue(hoja.contains("<c r=\"C5\" s=\"2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Promedio</t>"));
        assertTrue(hoja.contains("<c r=\"B6\" s=\"4\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Juan Pérez García</t>"));
        assertTrue(hoja.contains("<c r=\"C6\" s=\"6\"><v>15</v></c>"), "15,0 se exporta como número");
        assertTrue(hoja.contains("<c r=\"C7\" s=\"5\"><v>10.5</v></c>"), "10,5 se exporta como decimal");
    }
}
