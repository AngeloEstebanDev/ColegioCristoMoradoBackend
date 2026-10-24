package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.MiHorarioDto;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.services.MiHorarioService;
import org.springframework.web.bind.annotation.*;

/** Alumno: su horario y sus cursos. Docente: su horario semanal. Se identifican con la cabecera X-Usuario-Id. */
@RestController
@RequestMapping("/api/mi-horario")
@CrossOrigin(origins = "${app.cors.origenes}")
public class MiHorarioController {

    private final MiHorarioService miHorarioService;
    private final UsuarioRepository usuarioRepository;

    public MiHorarioController(MiHorarioService miHorarioService, UsuarioRepository usuarioRepository) {
        this.miHorarioService = miHorarioService;
        this.usuarioRepository = usuarioRepository;
    }

    /** Docente: su horario semanal con todas sus secciones. */
    @GetMapping("/docente")
    public MiHorarioDto.Docente docente(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        String nombre = usuarioId == null ? null : usuarioRepository.findById(usuarioId).map(u -> u.getNombre()).orElse(null);
        return miHorarioService.delDocente(usuarioId, nombre);
    }

    /** Padre: horario y cursos de su hijo. */
    @GetMapping("/hijos/{estudianteId}")
    public MiHorarioDto.Respuesta delHijo(@PathVariable Long estudianteId,
                                          @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return miHorarioService.delHijo(usuarioId, estudianteId);
    }

    @GetMapping
    public MiHorarioDto.Respuesta obtener(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return miHorarioService.delAlumno(usuarioId);
    }
}
