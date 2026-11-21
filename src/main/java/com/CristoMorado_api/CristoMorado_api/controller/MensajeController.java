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

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    /**
     * Endpoint GET /api/mensajes/contactos — Devuelve contactos. La lógica está en
     * mensajeService.contactos().
     */
    @GetMapping("/contactos")
    public ContactosResponse contactos(@RequestParam Long usuarioId) {
        return mensajeService.contactos(usuarioId);
    }

    // ---------------- Chat (una conversación por persona) ----------------

    /**
     * Endpoint GET /api/mensajes/conversaciones — Devuelve conversaciones (como lista). La lógica está
     * en mensajeService.conversaciones().
     */
    @GetMapping("/conversaciones")
    public List<ChatDto.Conversacion> conversaciones(@RequestParam Long usuarioId) {
        return mensajeService.conversaciones(usuarioId);
    }

    /** otroId = usuario con quien converso; 0 = Avisos del colegio. Marca lo recibido como leído. */
    @GetMapping("/conversaciones/{otroId}")
    public ChatDto.Chat conversacion(@PathVariable Long otroId, @RequestParam Long usuarioId) {
        return mensajeService.conversacion(usuarioId, otroId);
    }

    /**
     * Endpoint POST /api/mensajes/conversaciones/{otroId} — Envía chat. La lógica está en
     * mensajeService.enviarChat().
     */
    @PostMapping("/conversaciones/{otroId}")
    public ResponseEntity<ChatDto.MensajeChat> enviarChat(@PathVariable Long otroId, @RequestParam Long usuarioId,
                                                          @RequestBody ChatDto.EnviarRequest request) {
        return ResponseEntity.ok(mensajeService.enviarChat(usuarioId, otroId, request));
    }

    /**
     * Endpoint DELETE /api/mensajes/conversaciones/{otroId} — Elimina conversacion. La lógica está en
     * mensajeService.eliminarConversacion(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/conversaciones/{otroId}")
    public ResponseEntity<Void> eliminarConversacion(@PathVariable Long otroId, @RequestParam Long usuarioId) {
        mensajeService.eliminarConversacion(usuarioId, otroId);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Borradores ----------------

    /**
     * Endpoint GET /api/mensajes/borradores — Devuelve borradores (como lista). La lógica está en
     * mensajeService.borradores().
     */
    @GetMapping("/borradores")
    public List<BorradorDto.Borrador> borradores(@RequestParam Long usuarioId) {
        return mensajeService.borradores(usuarioId);
    }

    /** Crea (sin id) o actualiza (con id) un borrador. */
    @PostMapping("/borradores")
    public BorradorDto.Borrador guardarBorrador(@RequestParam Long usuarioId, @RequestBody BorradorDto.Request request) {
        return mensajeService.guardarBorrador(usuarioId, request);
    }

    /**
     * Endpoint DELETE /api/mensajes/borradores/{id} — Elimina borrador. La lógica está en
     * mensajeService.eliminarBorrador(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/borradores/{id}")
    public ResponseEntity<Void> eliminarBorrador(@PathVariable Long id, @RequestParam Long usuarioId) {
        mensajeService.eliminarBorrador(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Bandejas (compatibilidad) ----------------

    /**
     * Endpoint GET /api/mensajes/recibidos — Devuelve recibidos (como lista). La lógica está en
     * mensajeService.recibidos().
     */
    @GetMapping("/recibidos")
    public List<MensajeResponse> recibidos(@RequestParam Long usuarioId) {
        return mensajeService.recibidos(usuarioId);
    }

    /** Avisos del colegio sin leer: se muestran en un modal grande al entrar a la cuenta. */
    @GetMapping("/avisos-pendientes")
    public List<MensajeResponse> avisosPendientes(@RequestParam Long usuarioId) {
        return mensajeService.avisosPendientes(usuarioId);
    }

    /**
     * Endpoint POST /api/mensajes/avisos/leidos — Marca avisos leidos. La lógica está en
     * mensajeService.marcarAvisosLeidos(). Si todo sale bien responde 204 (sin contenido).
     */
    @PostMapping("/avisos/leidos")
    public ResponseEntity<Void> marcarAvisosLeidos(@RequestParam Long usuarioId, @RequestBody List<Long> ids) {
        mensajeService.marcarAvisosLeidos(usuarioId, ids);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint GET /api/mensajes/enviados — Devuelve enviados (como lista). La lógica está en
     * mensajeService.enviados().
     */
    @GetMapping("/enviados")
    public List<MensajeResponse> enviados(@RequestParam Long usuarioId) {
        return mensajeService.enviados(usuarioId);
    }

    /**
     * Endpoint POST /api/mensajes — Envía los mensajes internos. La lógica está en
     * mensajeService.enviar().
     */
    @PostMapping
    public ResponseEntity<MensajeResponse> enviar(@RequestParam Long usuarioId,
                                                  @RequestBody MensajeRequest request) {
        return ResponseEntity.ok(mensajeService.enviar(usuarioId, request));
    }

    /**
     * Endpoint POST /api/mensajes/{id}/leido — Marca leido. La lógica está en
     * mensajeService.marcarLeido(). Si todo sale bien responde 204 (sin contenido).
     */
    @PostMapping("/{id}/leido")
    public ResponseEntity<Void> marcarLeido(@PathVariable Long id, @RequestParam Long usuarioId) {
        mensajeService.marcarLeido(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint DELETE /api/mensajes/{id} — Elimina el mensaje indicado por su id. La lógica está en
     * mensajeService.eliminar(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        mensajeService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
