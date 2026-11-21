package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.ComunicadoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.ComunicadoResponse;
import com.CristoMorado_api.CristoMorado_api.services.ComunicadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comunicados")
@CrossOrigin(origins = "${app.cors.origenes}")
public class ComunicadoController {

    private final ComunicadoService comunicadoService;

    public ComunicadoController(ComunicadoService comunicadoService) {
        this.comunicadoService = comunicadoService;
    }

    @GetMapping
    public List<ComunicadoResponse> listar(@RequestParam Long usuarioId) {
        return comunicadoService.listar(usuarioId);
    }

    @GetMapping("/secciones")
    public List<java.util.Map<String, Object>> secciones(@RequestParam Long usuarioId) {
        return comunicadoService.seccionesParaPublicar(usuarioId);
    }

    @GetMapping("/{id}")
    public ComunicadoResponse obtener(@PathVariable Long id, @RequestParam Long usuarioId) {
        return comunicadoService.obtener(id, usuarioId);
    }

    @PostMapping
    public ResponseEntity<ComunicadoResponse> crear(@RequestParam Long usuarioId,
                                                    @RequestBody ComunicadoRequest request) {
        return ResponseEntity.ok(comunicadoService.crear(usuarioId, request));
    }

    @PutMapping("/{id}")
    public ComunicadoResponse actualizar(@PathVariable Long id, @RequestParam Long usuarioId,
                                         @RequestBody ComunicadoRequest request) {
        return comunicadoService.actualizar(id, usuarioId, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        comunicadoService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/leido")
    public ResponseEntity<Void> marcarLeido(@PathVariable Long id, @RequestParam Long usuarioId) {
        comunicadoService.marcarLeido(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
