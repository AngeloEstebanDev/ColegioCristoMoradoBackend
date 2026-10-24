package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.BorradorDto;
import com.CristoMorado_api.CristoMorado_api.dto.ChatDto;
import com.CristoMorado_api.CristoMorado_api.dto.ContactosResponse;
import com.CristoMorado_api.CristoMorado_api.dto.MensajeRequest;
import com.CristoMorado_api.CristoMorado_api.dto.MensajeResponse;
import com.CristoMorado_api.CristoMorado_api.services.MensajeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Mensajería entre usuarios. Siempre con ?usuarioId= del usuario logeado. */
@RestController
@RequestMapping("/api/mensajes")
@CrossOrigin(origins = "${app.cors.origenes}")
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @GetMapping("/contactos")
    public ContactosResponse contactos(@RequestParam Long usuarioId) {
        return mensajeService.contactos(usuarioId);
    }

    // ---------------- Chat (una conversación por persona) ----------------

    @GetMapping("/conversaciones")
    public List<ChatDto.Conversacion> conversaciones(@RequestParam Long usuarioId) {
        return mensajeService.conversaciones(usuarioId);
    }

    /** otroId = usuario con quien converso; 0 = Avisos del colegio. Marca lo recibido como leído. */
    @GetMapping("/conversaciones/{otroId}")
    public ChatDto.Chat conversacion(@PathVariable Long otroId, @RequestParam Long usuarioId) {
        return mensajeService.conversacion(usuarioId, otroId);
    }

    @PostMapping("/conversaciones/{otroId}")
    public ResponseEntity<ChatDto.MensajeChat> enviarChat(@PathVariable Long otroId, @RequestParam Long usuarioId,
                                                          @RequestBody ChatDto.EnviarRequest request) {
        return ResponseEntity.ok(mensajeService.enviarChat(usuarioId, otroId, request));
    }

    @DeleteMapping("/conversaciones/{otroId}")
    public ResponseEntity<Void> eliminarConversacion(@PathVariable Long otroId, @RequestParam Long usuarioId) {
        mensajeService.eliminarConversacion(usuarioId, otroId);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Borradores ----------------

    @GetMapping("/borradores")
    public List<BorradorDto.Borrador> borradores(@RequestParam Long usuarioId) {
        return mensajeService.borradores(usuarioId);
    }

    /** Crea (sin id) o actualiza (con id) un borrador. */
    @PostMapping("/borradores")
    public BorradorDto.Borrador guardarBorrador(@RequestParam Long usuarioId, @RequestBody BorradorDto.Request request) {
        return mensajeService.guardarBorrador(usuarioId, request);
    }

    @DeleteMapping("/borradores/{id}")
    public ResponseEntity<Void> eliminarBorrador(@PathVariable Long id, @RequestParam Long usuarioId) {
        mensajeService.eliminarBorrador(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Bandejas (compatibilidad) ----------------

    @GetMapping("/recibidos")
    public List<MensajeResponse> recibidos(@RequestParam Long usuarioId) {
        return mensajeService.recibidos(usuarioId);
    }

    /** Avisos del colegio sin leer: se muestran en un modal grande al entrar a la cuenta. */
    @GetMapping("/avisos-pendientes")
    public List<MensajeResponse> avisosPendientes(@RequestParam Long usuarioId) {
        return mensajeService.avisosPendientes(usuarioId);
    }

    @PostMapping("/avisos/leidos")
    public ResponseEntity<Void> marcarAvisosLeidos(@RequestParam Long usuarioId, @RequestBody List<Long> ids) {
        mensajeService.marcarAvisosLeidos(usuarioId, ids);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/enviados")
    public List<MensajeResponse> enviados(@RequestParam Long usuarioId) {
        return mensajeService.enviados(usuarioId);
    }

    @PostMapping
    public ResponseEntity<MensajeResponse> enviar(@RequestParam Long usuarioId,
                                                  @RequestBody MensajeRequest request) {
        return ResponseEntity.ok(mensajeService.enviar(usuarioId, request));
    }

    @PostMapping("/{id}/leido")
    public ResponseEntity<Void> marcarLeido(@PathVariable Long id, @RequestParam Long usuarioId) {
        mensajeService.marcarLeido(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        mensajeService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
