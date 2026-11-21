package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.PersonalRequest;
import com.CristoMorado_api.CristoMorado_api.dto.PersonalResponse;
import com.CristoMorado_api.CristoMorado_api.services.PersonalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personal")
@CrossOrigin(origins = "${app.cors.origenes}")
public class PersonalController {

    public static final String CABECERA_USUARIO = "X-Usuario-Id";

    private final PersonalService personalService;

    public PersonalController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping
    public List<PersonalResponse> listar() {
        return personalService.listar();
    }

    @GetMapping("/{id}")
    public PersonalResponse obtener(@PathVariable Long id) {
        return personalService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<PersonalResponse> crear(@RequestBody PersonalRequest request,
                                                  @RequestHeader(value = CABECERA_USUARIO, required = false) Long actorId) {
        return ResponseEntity.ok(personalService.crear(request, actorId));
    }

    @PutMapping("/{id}")
    public PersonalResponse actualizar(@PathVariable Long id, @RequestBody PersonalRequest request,
                                       @RequestHeader(value = CABECERA_USUARIO, required = false) Long actorId) {
        return personalService.actualizar(id, request, actorId);
    }

    @PatchMapping("/{id}/estado")
    public PersonalResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request,
                                          @RequestHeader(value = CABECERA_USUARIO, required = false) Long actorId) {
        return personalService.cambiarEstado(id, Boolean.TRUE.equals(request.estado()), actorId);
    }

    @PutMapping("/{id}/permisos")
    public PersonalResponse actualizarPermisos(@PathVariable Long id, @RequestBody PermisosRequest request,
                                               @RequestHeader(value = CABECERA_USUARIO, required = false) Long actorId) {
        return personalService.actualizarPermisos(id, request.permisos(), actorId);
    }

    public record PermisosRequest(List<String> permisos) {}
}
