package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.NotificacionesResponse;
import com.CristoMorado_api.CristoMorado_api.services.ComunicadoService;
import com.CristoMorado_api.CristoMorado_api.services.MensajeService;
import org.springframework.web.bind.annotation.*;

/** Contador de la campana del navbar: comunicados y mensajes sin leer. */
@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "${app.cors.origenes}")
public class NotificacionController {

    private final ComunicadoService comunicadoService;
    private final MensajeService mensajeService;

    public NotificacionController(ComunicadoService comunicadoService, MensajeService mensajeService) {
        this.comunicadoService = comunicadoService;
        this.mensajeService = mensajeService;
    }

    @GetMapping("/{usuarioId}")
    public NotificacionesResponse obtener(@PathVariable Long usuarioId) {
        long comunicados = comunicadoService.contarNoLeidos(usuarioId);
        long mensajes = mensajeService.contarNoLeidos(usuarioId);
        return new NotificacionesResponse(comunicados, mensajes, comunicados + mensajes);
    }
}
