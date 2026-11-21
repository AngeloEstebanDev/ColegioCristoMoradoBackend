package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.entity.ArchivoAdjunto;
import com.CristoMorado_api.CristoMorado_api.services.ArchivoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/** Descarga de archivos del aula virtual (materiales, actividades y entregas). */
@RestController
@RequestMapping("/api/archivos")
@CrossOrigin(origins = "${app.cors.origenes}", exposedHeaders = HttpHeaders.CONTENT_DISPOSITION)
public class ArchivoController {

    private final ArchivoService archivoService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public ArchivoController(ArchivoService archivoService) {
        this.archivoService = archivoService;
    }

    /**
     * Endpoint GET /api/archivos/{id} — Descarga la subida y descarga de archivos. La lógica está en
     * archivoService.descargar(). Sabe quién hace la petición por la cabecera X-Usuario-Id que envía
     * el frontend.
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id,
                                           @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        ArchivoAdjunto a = archivoService.descargar(id, usuarioId);
        MediaType tipo;
        try {
            tipo = MediaType.parseMediaType(a.getTipoMime());
        } catch (Exception e) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.getNombre(), StandardCharsets.UTF_8).build().toString())
                .body(a.getDatos());
    }
}
