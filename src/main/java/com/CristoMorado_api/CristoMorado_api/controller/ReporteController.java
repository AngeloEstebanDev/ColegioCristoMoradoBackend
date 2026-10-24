package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.ReporteDto;
import com.CristoMorado_api.CristoMorado_api.services.ReporteService;
import com.CristoMorado_api.CristoMorado_api.util.ExcelXlsx;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;

/**
 * HU-31 Reportes académicos y HU-32 Exportación a Excel.
 * Todo /api/reportes exige el permiso REPORTES (PermisoInterceptor), también para ver.
 */
@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "${app.cors.origenes}", exposedHeaders = HttpHeaders.CONTENT_DISPOSITION)
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/filtros")
    public ReporteDto.Filtros filtros() {
        return reporteService.filtros();
    }

    @GetMapping("/opciones")
    public ReporteDto.Opciones opciones() {
        return reporteService.opciones();
    }

    @GetMapping("/{tipo}")
    public ReporteDto.Tabla generar(@PathVariable String tipo,
                                    @RequestParam(required = false) Long periodoId,
                                    @RequestParam(required = false) Long seccionId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                    @RequestParam(required = false) String grado,
                                    @RequestParam(required = false) Long estudianteId,
                                    @RequestParam(required = false) Long cursoId) {
        return reporteService.generar(tipo, periodoId, seccionId, desde, hasta, grado, estudianteId, cursoId);
    }

    @GetMapping("/{tipo}/excel")
    public ResponseEntity<byte[]> excel(@PathVariable String tipo,
                                        @RequestParam(required = false) Long periodoId,
                                        @RequestParam(required = false) Long seccionId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                        @RequestParam(required = false) String grado,
                                        @RequestParam(required = false) Long estudianteId,
                                        @RequestParam(required = false) Long cursoId) {
        ReporteDto.Tabla tabla = reporteService.generar(tipo, periodoId, seccionId, desde, hasta, grado, estudianteId, cursoId);
        String nombre = archivo(tabla.titulo()) + "_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExcelXlsx.CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre, StandardCharsets.UTF_8).build().toString())
                .body(ExcelXlsx.generar(tabla));
    }

    /** "Rendimiento académico - 3° Primaria A" -> "Rendimiento_academico_3_Primaria_A" */
    private static String archivo(String titulo) {
        String sinTildes = Normalizer.normalize(titulo == null ? "Reporte" : titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = sinTildes.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return limpio.isEmpty() ? "Reporte" : limpio;
    }
}
