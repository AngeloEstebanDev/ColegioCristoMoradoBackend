package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.AulaDto;
import com.CristoMorado_api.CristoMorado_api.services.AulaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Aula virtual (HU-13, HU-14, HU-15). El usuario logeado llega en la cabecera X-Usuario-Id.
 *  Docente: /docente/**, crear/editar/eliminar materiales y actividades, ver y calificar entregas.
 *  Alumno:  /mis-cursos, /mis-actividades, entregar.
 *  Padre:   /hijos/{id}/cursos, /hijos/{id}/actividades.
 */
@RestController
@RequestMapping("/api/aula")
@CrossOrigin(origins = "${app.cors.origenes}")
public class AulaController {

    private static final String USUARIO = "X-Usuario-Id";

    private final AulaService aulaService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public AulaController(AulaService aulaService) {
        this.aulaService = aulaService;
    }

    // ---------------- Cursos ----------------

    /**
     * Endpoint GET /api/aula/docente/cursos — Devuelve cursos docente (como lista). La lógica está en
     * aulaService.cursosDocente().
     */
    @GetMapping("/docente/cursos")
    public List<AulaDto.CursoAula> cursosDocente(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.cursosDocente(usuarioId);
    }

    /**
     * Endpoint GET /api/aula/docente/resumen — Devuelve resumen docente. La lógica está en
     * aulaService.resumenDocente().
     */
    @GetMapping("/docente/resumen")
    public AulaDto.ResumenDocente resumenDocente(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.resumenDocente(usuarioId);
    }

    /**
     * Endpoint GET /api/aula/mis-cursos — Devuelve mis cursos del usuario que inició sesión. La lógica
     * está en aulaService.cursosAlumno().
     */
    @GetMapping("/mis-cursos")
    public List<AulaDto.CursoAula> misCursos(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.cursosAlumno(usuarioId);
    }

    /**
     * Endpoint GET /api/aula/hijos/{estudianteId}/cursos — Devuelve cursos hijo (como lista). La
     * lógica está en aulaService.cursosHijo().
     */
    @GetMapping("/hijos/{estudianteId}/cursos")
    public List<AulaDto.CursoAula> cursosHijo(@PathVariable Long estudianteId,
                                             @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.cursosHijo(usuarioId, estudianteId);
    }

    /**
     * Endpoint GET /api/aula/cursos/{seccionCursoId} — Devuelve curso. La lógica está en
     * aulaService.curso().
     */
    @GetMapping("/cursos/{seccionCursoId}")
    public AulaDto.CursoAula curso(@PathVariable Long seccionCursoId,
                                   @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.curso(usuarioId, seccionCursoId);
    }

    /**
     * Endpoint GET /api/aula/cursos/{seccionCursoId}/participantes — Devuelve participantes. La lógica
     * está en aulaService.participantes().
     */
    @GetMapping("/cursos/{seccionCursoId}/participantes")
    public AulaDto.Participantes participantes(@PathVariable Long seccionCursoId,
                                               @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.participantes(usuarioId, seccionCursoId);
    }

    // ---------------- Materiales ----------------

    /**
     * Endpoint GET /api/aula/cursos/{seccionCursoId}/materiales — Devuelve materiales (como lista). La
     * lógica está en aulaService.materiales().
     */
    @GetMapping("/cursos/{seccionCursoId}/materiales")
    public List<AulaDto.MaterialResponse> materiales(@PathVariable Long seccionCursoId,
                                                     @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.materiales(usuarioId, seccionCursoId);
    }

    /**
     * Endpoint POST /api/aula/cursos/{seccionCursoId}/materiales — Crea material. La lógica está en
     * aulaService.crearMaterial().
     */
    @PostMapping("/cursos/{seccionCursoId}/materiales")
    public AulaDto.MaterialResponse crearMaterial(@PathVariable Long seccionCursoId, @RequestBody AulaDto.MaterialRequest req,
                                                  @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.crearMaterial(usuarioId, seccionCursoId, req);
    }

    /**
     * Endpoint PUT /api/aula/materiales/{id} — Actualiza material. La lógica está en
     * aulaService.actualizarMaterial().
     */
    @PutMapping("/materiales/{id}")
    public AulaDto.MaterialResponse actualizarMaterial(@PathVariable Long id, @RequestBody AulaDto.MaterialRequest req,
                                                       @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actualizarMaterial(usuarioId, id, req);
    }

    /**
     * Endpoint DELETE /api/aula/materiales/{id} — Elimina material. La lógica está en
     * aulaService.eliminarMaterial(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/materiales/{id}")
    public ResponseEntity<Void> eliminarMaterial(@PathVariable Long id,
                                                 @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        aulaService.eliminarMaterial(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint PATCH /api/aula/materiales/{id}/visible — Devuelve visible material. La lógica está en
     * aulaService.visibleMaterial().
     */
    @PatchMapping("/materiales/{id}/visible")
    public AulaDto.MaterialResponse visibleMaterial(@PathVariable Long id, @RequestBody AulaDto.VisibleRequest req,
                                                    @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.visibleMaterial(usuarioId, id, req == null ? null : req.visible());
    }

    // ---------------- Carpetas (se crean en el bimestre en curso) ----------------

    /**
     * Endpoint GET /api/aula/cursos/{seccionCursoId}/carpetas — Devuelve carpetas (como lista). La
     * lógica está en aulaService.carpetas().
     */
    @GetMapping("/cursos/{seccionCursoId}/carpetas")
    public List<AulaDto.Carpeta> carpetas(@PathVariable Long seccionCursoId,
                                          @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.carpetas(usuarioId, seccionCursoId);
    }

    /**
     * Endpoint POST /api/aula/cursos/{seccionCursoId}/carpetas — Crea carpeta. La lógica está en
     * aulaService.crearCarpeta().
     */
    @PostMapping("/cursos/{seccionCursoId}/carpetas")
    public AulaDto.Carpeta crearCarpeta(@PathVariable Long seccionCursoId, @RequestBody AulaDto.CarpetaRequest req,
                                        @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.crearCarpeta(usuarioId, seccionCursoId, req);
    }

    /**
     * Endpoint PUT /api/aula/carpetas/{id} — Devuelve renombrar carpeta. La lógica está en
     * aulaService.renombrarCarpeta().
     */
    @PutMapping("/carpetas/{id}")
    public AulaDto.Carpeta renombrarCarpeta(@PathVariable Long id, @RequestBody AulaDto.CarpetaRequest req,
                                            @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.renombrarCarpeta(usuarioId, id, req);
    }

    /**
     * Endpoint PATCH /api/aula/carpetas/{id}/visible — Devuelve visible carpeta. La lógica está en
     * aulaService.visibleCarpeta().
     */
    @PatchMapping("/carpetas/{id}/visible")
    public AulaDto.Carpeta visibleCarpeta(@PathVariable Long id, @RequestBody AulaDto.VisibleRequest req,
                                          @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.visibleCarpeta(usuarioId, id, req == null ? null : req.visible());
    }

    /**
     * Endpoint DELETE /api/aula/carpetas/{id} — Elimina carpeta. La lógica está en
     * aulaService.eliminarCarpeta(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/carpetas/{id}")
    public ResponseEntity<Void> eliminarCarpeta(@PathVariable Long id,
                                                @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        aulaService.eliminarCarpeta(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Actividades ----------------

    /**
     * Endpoint PATCH /api/aula/actividades/{id}/visible — Devuelve visible actividad. La lógica está
     * en aulaService.visibleActividad().
     */
    @PatchMapping("/actividades/{id}/visible")
    public AulaDto.ActividadResponse visibleActividad(@PathVariable Long id, @RequestBody AulaDto.VisibleRequest req,
                                                      @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.visibleActividad(usuarioId, id, req == null ? null : req.visible());
    }

    /** Activa / desactiva "aceptar entregas tardías" (se puede cambiar aunque ya haya entregas). */
    @PatchMapping("/actividades/{id}/tardia")
    public AulaDto.ActividadResponse tardiaActividad(@PathVariable Long id, @RequestBody AulaDto.TardiaRequest req,
                                                     @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.tardiaActividad(usuarioId, id, req == null ? null : req.permiteTardia());
    }

    /**
     * Endpoint GET /api/aula/cursos/{seccionCursoId}/actividades — Devuelve actividades (como lista).
     * La lógica está en aulaService.actividades().
     */
    @GetMapping("/cursos/{seccionCursoId}/actividades")
    public List<AulaDto.ActividadResponse> actividades(@PathVariable Long seccionCursoId,
                                                       @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actividades(usuarioId, seccionCursoId);
    }

    /**
     * Endpoint POST /api/aula/cursos/{seccionCursoId}/actividades — Crea actividad. La lógica está en
     * aulaService.crearActividad().
     */
    @PostMapping("/cursos/{seccionCursoId}/actividades")
    public AulaDto.ActividadResponse crearActividad(@PathVariable Long seccionCursoId, @RequestBody AulaDto.ActividadRequest req,
                                                    @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.crearActividad(usuarioId, seccionCursoId, req);
    }

    /**
     * Endpoint GET /api/aula/actividades/{id} — Devuelve actividad. La lógica está en
     * aulaService.actividad().
     */
    @GetMapping("/actividades/{id}")
    public AulaDto.ActividadResponse actividad(@PathVariable Long id,
                                               @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actividad(usuarioId, id);
    }

    /**
     * Endpoint PUT /api/aula/actividades/{id} — Actualiza actividad. La lógica está en
     * aulaService.actualizarActividad().
     */
    @PutMapping("/actividades/{id}")
    public AulaDto.ActividadResponse actualizarActividad(@PathVariable Long id, @RequestBody AulaDto.ActividadRequest req,
                                                         @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actualizarActividad(usuarioId, id, req);
    }

    /**
     * Endpoint DELETE /api/aula/actividades/{id} — Elimina actividad. La lógica está en
     * aulaService.eliminarActividad(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/actividades/{id}")
    public ResponseEntity<Void> eliminarActividad(@PathVariable Long id,
                                                  @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        aulaService.eliminarActividad(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint GET /api/aula/mis-actividades — Devuelve mis actividades del usuario que inició sesión.
     * La lógica está en aulaService.misActividades().
     */
    @GetMapping("/mis-actividades")
    public List<AulaDto.ActividadResponse> misActividades(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.misActividades(usuarioId);
    }

    /**
     * Endpoint GET /api/aula/hijos/{estudianteId}/actividades — Devuelve actividades hijo (como
     * lista). La lógica está en aulaService.actividadesHijo().
     */
    @GetMapping("/hijos/{estudianteId}/actividades")
    public List<AulaDto.ActividadResponse> actividadesHijo(@PathVariable Long estudianteId,
                                                           @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actividadesHijo(usuarioId, estudianteId);
    }

    // ---------------- Entregas ----------------

    /**
     * Endpoint POST /api/aula/actividades/{id}/entrega — Devuelve entregar. La lógica está en
     * aulaService.entregar().
     */
    @PostMapping("/actividades/{id}/entrega")
    public AulaDto.ActividadResponse entregar(@PathVariable Long id, @RequestBody AulaDto.EntregaRequest req,
                                              @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.entregar(usuarioId, id, req);
    }

    /**
     * Endpoint GET /api/aula/actividades/{id}/entregas — Devuelve entregas. La lógica está en
     * aulaService.entregas().
     */
    @GetMapping("/actividades/{id}/entregas")
    public AulaDto.Entregas entregas(@PathVariable Long id,
                                     @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.entregas(usuarioId, id);
    }

    /**
     * Endpoint PATCH /api/aula/entregas/{id}/calificar — Devuelve calificar. La lógica está en
     * aulaService.calificar().
     */
    @PatchMapping("/entregas/{id}/calificar")
    public AulaDto.Entregas calificar(@PathVariable Long id, @RequestBody AulaDto.CalificarRequest req,
                                      @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.calificar(usuarioId, id, req);
    }
}
