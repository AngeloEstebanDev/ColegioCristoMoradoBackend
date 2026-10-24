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

    public AulaController(AulaService aulaService) {
        this.aulaService = aulaService;
    }

    // ---------------- Cursos ----------------

    @GetMapping("/docente/cursos")
    public List<AulaDto.CursoAula> cursosDocente(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.cursosDocente(usuarioId);
    }

    @GetMapping("/docente/resumen")
    public AulaDto.ResumenDocente resumenDocente(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.resumenDocente(usuarioId);
    }

    @GetMapping("/mis-cursos")
    public List<AulaDto.CursoAula> misCursos(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.cursosAlumno(usuarioId);
    }

    @GetMapping("/hijos/{estudianteId}/cursos")
    public List<AulaDto.CursoAula> cursosHijo(@PathVariable Long estudianteId,
                                             @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.cursosHijo(usuarioId, estudianteId);
    }

    @GetMapping("/cursos/{seccionCursoId}")
    public AulaDto.CursoAula curso(@PathVariable Long seccionCursoId,
                                   @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.curso(usuarioId, seccionCursoId);
    }

    @GetMapping("/cursos/{seccionCursoId}/participantes")
    public AulaDto.Participantes participantes(@PathVariable Long seccionCursoId,
                                               @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.participantes(usuarioId, seccionCursoId);
    }

    // ---------------- Materiales ----------------

    @GetMapping("/cursos/{seccionCursoId}/materiales")
    public List<AulaDto.MaterialResponse> materiales(@PathVariable Long seccionCursoId,
                                                     @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.materiales(usuarioId, seccionCursoId);
    }

    @PostMapping("/cursos/{seccionCursoId}/materiales")
    public AulaDto.MaterialResponse crearMaterial(@PathVariable Long seccionCursoId, @RequestBody AulaDto.MaterialRequest req,
                                                  @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.crearMaterial(usuarioId, seccionCursoId, req);
    }

    @PutMapping("/materiales/{id}")
    public AulaDto.MaterialResponse actualizarMaterial(@PathVariable Long id, @RequestBody AulaDto.MaterialRequest req,
                                                       @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actualizarMaterial(usuarioId, id, req);
    }

    @DeleteMapping("/materiales/{id}")
    public ResponseEntity<Void> eliminarMaterial(@PathVariable Long id,
                                                 @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        aulaService.eliminarMaterial(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/materiales/{id}/visible")
    public AulaDto.MaterialResponse visibleMaterial(@PathVariable Long id, @RequestBody AulaDto.VisibleRequest req,
                                                    @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.visibleMaterial(usuarioId, id, req == null ? null : req.visible());
    }

    // ---------------- Carpetas (se crean en el bimestre en curso) ----------------

    @GetMapping("/cursos/{seccionCursoId}/carpetas")
    public List<AulaDto.Carpeta> carpetas(@PathVariable Long seccionCursoId,
                                          @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.carpetas(usuarioId, seccionCursoId);
    }

    @PostMapping("/cursos/{seccionCursoId}/carpetas")
    public AulaDto.Carpeta crearCarpeta(@PathVariable Long seccionCursoId, @RequestBody AulaDto.CarpetaRequest req,
                                        @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.crearCarpeta(usuarioId, seccionCursoId, req);
    }

    @PutMapping("/carpetas/{id}")
    public AulaDto.Carpeta renombrarCarpeta(@PathVariable Long id, @RequestBody AulaDto.CarpetaRequest req,
                                            @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.renombrarCarpeta(usuarioId, id, req);
    }

    @PatchMapping("/carpetas/{id}/visible")
    public AulaDto.Carpeta visibleCarpeta(@PathVariable Long id, @RequestBody AulaDto.VisibleRequest req,
                                          @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.visibleCarpeta(usuarioId, id, req == null ? null : req.visible());
    }

    @DeleteMapping("/carpetas/{id}")
    public ResponseEntity<Void> eliminarCarpeta(@PathVariable Long id,
                                                @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        aulaService.eliminarCarpeta(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Actividades ----------------

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

    @GetMapping("/cursos/{seccionCursoId}/actividades")
    public List<AulaDto.ActividadResponse> actividades(@PathVariable Long seccionCursoId,
                                                       @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actividades(usuarioId, seccionCursoId);
    }

    @PostMapping("/cursos/{seccionCursoId}/actividades")
    public AulaDto.ActividadResponse crearActividad(@PathVariable Long seccionCursoId, @RequestBody AulaDto.ActividadRequest req,
                                                    @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.crearActividad(usuarioId, seccionCursoId, req);
    }

    @GetMapping("/actividades/{id}")
    public AulaDto.ActividadResponse actividad(@PathVariable Long id,
                                               @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actividad(usuarioId, id);
    }

    @PutMapping("/actividades/{id}")
    public AulaDto.ActividadResponse actualizarActividad(@PathVariable Long id, @RequestBody AulaDto.ActividadRequest req,
                                                         @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actualizarActividad(usuarioId, id, req);
    }

    @DeleteMapping("/actividades/{id}")
    public ResponseEntity<Void> eliminarActividad(@PathVariable Long id,
                                                  @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        aulaService.eliminarActividad(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mis-actividades")
    public List<AulaDto.ActividadResponse> misActividades(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.misActividades(usuarioId);
    }

    @GetMapping("/hijos/{estudianteId}/actividades")
    public List<AulaDto.ActividadResponse> actividadesHijo(@PathVariable Long estudianteId,
                                                           @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.actividadesHijo(usuarioId, estudianteId);
    }

    // ---------------- Entregas ----------------

    @PostMapping("/actividades/{id}/entrega")
    public AulaDto.ActividadResponse entregar(@PathVariable Long id, @RequestBody AulaDto.EntregaRequest req,
                                              @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.entregar(usuarioId, id, req);
    }

    @GetMapping("/actividades/{id}/entregas")
    public AulaDto.Entregas entregas(@PathVariable Long id,
                                     @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.entregas(usuarioId, id);
    }

    @PatchMapping("/entregas/{id}/calificar")
    public AulaDto.Entregas calificar(@PathVariable Long id, @RequestBody AulaDto.CalificarRequest req,
                                      @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return aulaService.calificar(usuarioId, id, req);
    }
}
