package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.services.CorreoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/correo")
@CrossOrigin(origins = "${app.cors.origenes}")
public class CorreoController {

    private final CorreoService correoService;

    public CorreoController(CorreoService correoService) {
        this.correoService = correoService;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        String metodo = correoService.metodo();
        return Map.of(
                "metodo", metodo,
                "configurado", !"NINGUNO".equals(metodo),
                "modoConsola", correoService.isModoConsola());
    }
}
