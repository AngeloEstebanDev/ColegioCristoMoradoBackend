package com.CristoMorado_api.CristoMorado_api.util;

import com.CristoMorado_api.CristoMorado_api.dto.ReporteDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * HU-32: genera un archivo Excel (.xlsx, Office Open XML) a partir de una tabla de reporte.
 * No necesita librerías externas: un .xlsx es un ZIP con archivos XML.
 * Estructura de la hoja:
 *   fila 1: título (negrita) — fila 2: subtítulo — fila 3: indicadores — fila 5: encabezados — luego los datos.
 */
public final class ExcelXlsx {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // Estilos (índices de cellXfs en styles.xml)
    private static final int S_TITULO = 1, S_ENCABEZADO = 2, S_SUBTITULO = 3, S_TEXTO = 4, S_DECIMAL = 5, S_ENTERO = 6;

    private ExcelXlsx() {}

    public static byte[] generar(ReporteDto.Tabla tabla) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                agregar(zip, "[Content_Types].xml", TIPOS);
                agregar(zip, "_rels/.rels", RELS);
                agregar(zip, "xl/workbook.xml", workbook(nombreHoja(tabla.titulo())));
                agregar(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
                agregar(zip, "xl/styles.xml", ESTILOS);
                agregar(zip, "xl/worksheets/sheet1.xml", hoja(tabla));
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String hoja(ReporteDto.Tabla t) {
        List<String> columnas = t.columnas();
        int nCols = Math.max(1, columnas.size());
        String ultima = columna(nCols - 1);
        int filaEncabezado = 5;
        int filaFinal = filaEncabezado + t.filas().size();

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
          .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
          .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
          .append("<dimension ref=\"A1:").append(ultima).append(Math.max(filaFinal, filaEncabezado)).append("\"/>")
          .append("<sheetViews><sheetView workbookViewId=\"0\">")
          .append("<pane ySplit=\"").append(filaEncabezado).append("\" topLeftCell=\"A").append(filaEncabezado + 1)
          .append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
          .append("<sheetFormatPr defaultRowHeight=\"15\"/>");

        // Ancho de columnas según el contenido más largo (máximo 60).
        sb.append("<cols>");
        for (int c = 0; c < nCols; c++) {
            int ancho = c < columnas.size() ? largo(columnas.get(c)) : 8;
            for (List<Object> fila : t.filas()) {
                if (c < fila.size()) ancho = Math.max(ancho, largo(fila.get(c)));
            }
            ancho = Math.min(60, Math.max(8, ancho + 2));
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
              .append("\" width=\"").append(ancho).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");

        fila(sb, 1, List.of(t.titulo() == null ? "Reporte" : t.titulo()), S_TITULO, "20");
        fila(sb, 2, List.of(t.subtitulo() == null ? "" : t.subtitulo()), S_SUBTITULO, null);
        if (t.indicadores() != null && !t.indicadores().isEmpty()) {
            StringBuilder ind = new StringBuilder();
            for (ReporteDto.Indicador i : t.indicadores()) {
                if (ind.length() > 0) ind.append("   |   ");
                ind.append(i.etiqueta()).append(": ").append(i.valor());
            }
            fila(sb, 3, List.of(ind.toString()), S_SUBTITULO, null);
        }
        fila(sb, filaEncabezado, List.copyOf(columnas), S_ENCABEZADO, "20");
        int r = filaEncabezado + 1;
        for (List<Object> datos : t.filas()) fila(sb, r++, datos, -1, null);
        sb.append("</sheetData>");

        if (!t.filas().isEmpty()) {
            sb.append("<autoFilter ref=\"A").append(filaEncabezado).append(':').append(ultima).append(filaFinal).append("\"/>");
        }
        if (nCols > 1) {
            sb.append("<mergeCells count=\"").append(t.indicadores() == null || t.indicadores().isEmpty() ? 2 : 3).append("\">")
              .append("<mergeCell ref=\"A1:").append(ultima).append("1\"/>")
              .append("<mergeCell ref=\"A2:").append(ultima).append("2\"/>");
            if (t.indicadores() != null && !t.indicadores().isEmpty()) {
                sb.append("<mergeCell ref=\"A3:").append(ultima).append("3\"/>");
            }
            sb.append("</mergeCells>");
        }
        sb.append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.75\" bottom=\"0.75\" header=\"0.3\" footer=\"0.3\"/>")
          .append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
          .append("</worksheet>");
        return sb.toString();
    }

    /** estilo -1 = según el tipo de dato (texto / entero / decimal) con borde. */
    private static void fila(StringBuilder sb, int numero, List<?> celdas, int estilo, String alto) {
        sb.append("<row r=\"").append(numero).append('"');
        if (alto != null) sb.append(" ht=\"").append(alto).append("\" customHeight=\"1\"");
        sb.append('>');
        for (int c = 0; c < celdas.size(); c++) {
            Object v = celdas.get(c);
            String ref = columna(c) + numero;
            if (v instanceof Number n && !(Double.isNaN(n.doubleValue()) || Double.isInfinite(n.doubleValue()))) {
                boolean entero = v instanceof Integer || v instanceof Long || n.doubleValue() == Math.rint(n.doubleValue());
                int s = estilo >= 0 ? estilo : (entero ? S_ENTERO : S_DECIMAL);
                sb.append("<c r=\"").append(ref).append("\" s=\"").append(s).append("\"><v>")
                  .append(entero ? String.valueOf(n.longValue()) : String.valueOf(n.doubleValue())).append("</v></c>");
            } else {
                int s = estilo >= 0 ? estilo : S_TEXTO;
                String texto = v == null ? "" : String.valueOf(v);
                sb.append("<c r=\"").append(ref).append("\" s=\"").append(s).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                  .append(xml(texto)).append("</t></is></c>");
            }
        }
        sb.append("</row>");
    }

    /** 0 -> A, 25 -> Z, 26 -> AA ... */
    static String columna(int indice) {
        StringBuilder sb = new StringBuilder();
        int n = indice + 1;
        while (n > 0) {
            int resto = (n - 1) % 26;
            sb.insert(0, (char) ('A' + resto));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    private static int largo(Object v) {
        if (v == null) return 0;
        if (v instanceof Double d) return String.format("%.2f", d).length();
        return String.valueOf(v).length();
    }

    /** Escapa XML y quita caracteres de control no válidos. */
    private static String xml(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> {
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch);
                }
            }
        }
        return sb.toString();
    }

    private static String nombreHoja(String titulo) {
        String n = titulo == null || titulo.isBlank() ? "Reporte" : titulo.replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        return n.length() > 31 ? n.substring(0, 31).trim() : n;
    }

    private static void agregar(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String workbook(String hoja) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"" + xml(hoja) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private static final String TIPOS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static final String WORKBOOK_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    /** Colores del sistema: morado #4A1F52 en los encabezados. */
    private static final String ESTILOS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"0.00\"/></numFmts>"
            + "<fonts count=\"4\">"
            + "<font><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"14\"/><color rgb=\"FF2E1233\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><i/><sz val=\"10\"/><color rgb=\"FF6B5A67\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "</fonts>"
            + "<fills count=\"3\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF4A1F52\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"2\">"
            + "<border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FFE3D5C4\"/></left><right style=\"thin\"><color rgb=\"FFE3D5C4\"/></right>"
            + "<top style=\"thin\"><color rgb=\"FFE3D5C4\"/></top><bottom style=\"thin\"><color rgb=\"FFE3D5C4\"/></bottom><diagonal/></border>"
            + "</borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"7\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">"
            + "<alignment vertical=\"center\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"/>"
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\"/>"
            + "<xf numFmtId=\"1\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
