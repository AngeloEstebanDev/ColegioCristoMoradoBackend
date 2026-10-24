package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.CitaDto;
import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.services.AgendaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Mi Agenda: disponibilidad (HU-27) y citas (HU-22, 28, 29, 30). */
@RestController
@RequestMapping("/api/agenda")
@CrossOrigin(origins = "${app.cors.origenes}")
public class AgendaController {

    private static final String USUARIO = "X-Usuario-Id";

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    // ---------------- Disponibilidad ----------------

    @GetMapping("/disponibilidad")
    public List<CitaDto.Disponibilidad> disponibilidad(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.misDisponibilidades(usuarioId);
    }

    @PostMapping("/disponibilidad")
    public CitaDto.Disponibilidad crearDisponibilidad(@RequestBody CitaDto.DisponibilidadRequest req,
                                                      @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.crearDisponibilidad(usuarioId, req);
    }

    @PutMapping("/disponibilidad/{id}")
    public CitaDto.Disponibilidad actualizarDisponibilidad(@PathVariable Long id, @RequestBody CitaDto.DisponibilidadRequest req,
                                                           @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.actualizarDisponibilidad(usuarioId, id, req);
    }

    @PatchMapping("/disponibilidad/{id}/estado")
    public CitaDto.Disponibilidad estadoDisponibilidad(@PathVariable Long id, @RequestBody EstadoRequest req,
                                                       @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        if (req == null || req.estado() == null) throw new DatosInvalidosException("Indica el nuevo estado.");
        return agendaService.cambiarEstadoDisponibilidad(usuarioId, id, req.estado());
    }

    @DeleteMapping("/disponibilidad/{id}")
    public ResponseEntity<Void> eliminarDisponibilidad(@PathVariable Long id,
                                                       @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        agendaService.eliminarDisponibilidad(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Padre: solicitar ----------------

    @GetMapping("/responsables")
    public List<CitaDto.Responsable> responsables(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.responsables(usuarioId);
    }

    @GetMapping("/responsables/{responsableId}/horarios")
    public List<CitaDto.Horario> horarios(@PathVariable Long responsableId,
                                          @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.horariosLibres(usuarioId, responsableId);
    }

    @PostMapping("/citas/solicitar")
    public CitaDto.Cita solicitar(@RequestBody CitaDto.SolicitarRequest req,
                                  @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.solicitar(usuarioId, req);
    }

    // ---------------- Responsable: programar ----------------

    @GetMapping("/padres")
    public List<CitaDto.PadreContacto> padres(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.padresParaProgramar(usuarioId);
    }

    @PostMapping("/citas")
    public CitaDto.Cita programar(@RequestBody CitaDto.ProgramarRequest req,
                                  @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.programar(usuarioId, req);
    }

    /** Administración: padres, docentes y alumnos a los que puede citar. */
    @GetMapping("/personas")
    public List<CitaDto.Persona> personas(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.personasParaCitar(usuarioId);
    }

    /** Horas libres propias en una fecha (para "Nueva cita"). */
    @GetMapping("/mis-horas-libres")
    public List<String> misHorasLibres(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                       @RequestParam(required = false) Integer duracion,
                                       @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.misHorasLibres(usuarioId, fecha, duracion);
    }

    @PutMapping("/disponibilidad/semana")
    public List<CitaDto.Disponibilidad> guardarSemana(@RequestBody CitaDto.DisponibilidadSemanaRequest req,
                                                      @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.guardarSemana(usuarioId, req);
    }

    @DeleteMapping("/citas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        agendaService.eliminar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Mis citas ----------------

    @GetMapping("/citas")
    public List<CitaDto.Cita> misCitas(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.misCitas(usuarioId);
    }

    @GetMapping("/resumen")
    public CitaDto.Resumen resumen(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.resumen(usuarioId);
    }

    @PatchMapping("/citas/{id}/confirmar")
    public CitaDto.Cita confirmar(@PathVariable Long id, @RequestBody(required = false) CitaDto.AccionRequest req,
                                  @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.confirmar(usuarioId, id, req == null ? null : req.motivo());
    }

    @PatchMapping("/citas/{id}/rechazar")
    public CitaDto.Cita rechazar(@PathVariable Long id, @RequestBody(required = false) CitaDto.AccionRequest req,
                                 @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.rechazar(usuarioId, id, req == null ? null : req.motivo());
    }

    @PatchMapping("/citas/{id}/cancelar")
    public CitaDto.Cita cancelar(@PathVariable Long id, @RequestBody(required = false) CitaDto.AccionRequest req,
                                 @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.cancelar(usuarioId, id, req == null ? null : req.motivo());
    }

    @PatchMapping("/citas/{id}/reprogramar")
    public CitaDto.Cita reprogramar(@PathVariable Long id, @RequestBody CitaDto.ReprogramarRequest req,
                                    @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.reprogramar(usuarioId, id, req);
    }

    @PatchMapping("/citas/{id}/realizada")
    public CitaDto.Cita realizada(@PathVariable Long id, @RequestBody(required = false) CitaDto.AccionRequest req,
                                  @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return agendaService.marcarRealizada(usuarioId, id, req == null ? null : req.motivo());
    }
}
