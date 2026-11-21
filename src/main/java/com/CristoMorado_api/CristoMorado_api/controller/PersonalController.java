package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.PersonalRequest;
import com.CristoMorado_api.CristoMorado_api.dto.PersonalResponse;
import com.CristoMorado_api.CristoMorado_api.services.PersonalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión de Personal Staff (Directiva). El frontend envía la cabecera
 * X-Usuario-Id con el usuario logeado para validar permisos.
 */
@RestController
@RequestMapping("/api/personal")
@CrossOrigin(origins = "${app.cors.origenes}")
public class PersonalController {

    public static final String CABECERA_USUARIO = "X-Usuario-Id";

    private final PersonalService personalService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public PersonalController(PersonalService personalService) {
        this.personalService = personalService;
    }

    /**
     * Endpoint GET /api/personal — Devuelve la lista de la directiva y el personal administrativo. La
     * lógica está en personalService.listar().
     */
    @GetMapping
    public List<PersonalResponse> listar() {
        return personalService.listar();
    }

    /**
     * Endpoint GET /api/personal/{id} — Obtiene el miembro del personal indicado por su id. La lógica
     * está en personalService.obtener().
     */
    @GetMapping("/{id}")
    public PersonalResponse obtener(@PathVariable Long id) {
        return personalService.obtener(id);
    }

    /**
     * Endpoint POST /api/personal — Crea un nuevo registro (el miembro del personal) con los datos
     * recibidos. La lógica está en personalService.crear().
     */
    @PostMapping
    public ResponseEntity<PersonalResponse> crear(@RequestBody PersonalRequest request,
                                                  @RequestHeader(value = CABECERA_USUARIO, required = false) Long actorId) {
        return ResponseEntity.ok(personalService.crear(request, actorId));
    }

    /**
     * Endpoint PUT /api/personal/{id} — Actualiza los datos del miembro del personal indicado por su
     * id. La lógica está en personalService.actualizar().
     */
    @PutMapping("/{id}")
    public PersonalResponse actualizar(@PathVariable Long id, @RequestBody PersonalRequest request,
                                       @RequestHeader(value = CABECERA_USUARIO, required = false) Long actorId) {
        return personalService.actualizar(id, request, actorId);
    }

    /**
     * Endpoint PATCH /api/personal/{id}/estado — Cambia estado. La lógica está en
     * personalService.cambiarEstado().
     */
    @PatchMapping("/{id}/estado")
    public PersonalResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request,
                                          @RequestHeader(value = CABECERA_USUARIO, required = false) Long actorId) {
        return personalService.cambiarEstado(id, Boolean.TRUE.equals(request.estado()), actorId);
    }

    /** Solo el administrador. Body: {"permisos": ["COMUNICADOS", "USUARIOS", ...]} */
    @PutMapping("/{id}/permisos")
    public PersonalResponse actualizarPermisos(@PathVariable Long id, @RequestBody PermisosRequest request,
                                               @RequestHeader(value = CABECERA_USUARIO, required = false) Long actorId) {
        return personalService.actualizarPermisos(id, request.permisos(), actorId);
    }

    /**
     * Estructura de datos PermisosRequest (record): agrupa permisos request en un solo objeto
     * inmutable que viaja como JSON.
     */
    public record PermisosRequest(List<String> permisos) {}
}
