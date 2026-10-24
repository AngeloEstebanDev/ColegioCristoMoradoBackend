package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.repository.CodigoVerificacionRepository;
import com.CristoMorado_api.CristoMorado_api.repository.ComunicadoLecturaRepository;
import com.CristoMorado_api.CristoMorado_api.repository.ComunicadoRepository;
import com.CristoMorado_api.CristoMorado_api.repository.MensajeBorradorRepository;
import com.CristoMorado_api.CristoMorado_api.repository.MensajeDestinatarioRepository;
import com.CristoMorado_api.CristoMorado_api.repository.MensajeRepository;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Antes de borrar una cuenta de `usuarios` hay que soltar las filas que
 * la referencian (comunicados, lecturas, mensajes); si no, PostgreSQL
 * rechaza el borrado por las llaves foráneas.
 */
@Service
public class LimpiezaUsuarioService {

    private final ComunicadoRepository comunicadoRepository;
    private final ComunicadoLecturaRepository lecturaRepository;
    private final MensajeRepository mensajeRepository;
    private final MensajeDestinatarioRepository destinatarioRepository;
    private final MensajeBorradorRepository borradorRepository;
    private final CodigoVerificacionRepository codigoRepository;
    private final CitaRepository citaRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final NotaRepository notaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EntregaRepository entregaRepository;
    private final ArchivoAdjuntoRepository archivoRepository;

    public LimpiezaUsuarioService(ComunicadoRepository comunicadoRepository,
                                  ComunicadoLecturaRepository lecturaRepository,
                                  MensajeRepository mensajeRepository,
                                  MensajeDestinatarioRepository destinatarioRepository,
                                  CodigoVerificacionRepository codigoRepository,
                                  CitaRepository citaRepository,
                                  DisponibilidadRepository disponibilidadRepository,
                                  NotaRepository notaRepository,
                                  AsistenciaRepository asistenciaRepository,
                                  EntregaRepository entregaRepository,
                                  ArchivoAdjuntoRepository archivoRepository,
                                  MensajeBorradorRepository borradorRepository) {
        this.borradorRepository = borradorRepository;
        this.comunicadoRepository = comunicadoRepository;
        this.lecturaRepository = lecturaRepository;
        this.mensajeRepository = mensajeRepository;
        this.destinatarioRepository = destinatarioRepository;
        this.codigoRepository = codigoRepository;
        this.citaRepository = citaRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.notaRepository = notaRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.entregaRepository = entregaRepository;
        this.archivoRepository = archivoRepository;
    }

    @Transactional
    public void liberarReferencias(Long usuarioId) {
        comunicadoRepository.desvincularAutor(usuarioId);       // el comunicado se conserva
        lecturaRepository.eliminarPorUsuario(usuarioId);
        destinatarioRepository.eliminarPorDestinatario(usuarioId);
        mensajeRepository.desvincularRemitente(usuarioId);      // queda como "Usuario eliminado"
        borradorRepository.eliminarPorUsuario(usuarioId);        // sus borradores
        borradorRepository.desvincularDestinatario(usuarioId);   // borradores de otros dirigidos a él
        codigoRepository.eliminarPorUsuario(usuarioId);
        citaRepository.eliminarPorUsuario(usuarioId);              // Mi Agenda
        disponibilidadRepository.eliminarPorUsuario(usuarioId);
    }

    /**
     * Antes de borrar a un alumno: sus notas, entregas (con sus archivos) y asistencias
     * se borran; sus citas se conservan sin el alumno.
     */
    @Transactional
    public void liberarEstudiante(Long estudianteId) {
        notaRepository.eliminarPorEstudiante(estudianteId);
        asistenciaRepository.eliminarPorEstudiante(estudianteId);
        for (var e : entregaRepository.findByEstudianteId(estudianteId)) {
            List<Long> archivos = AulaService.archivosDe(e);
            entregaRepository.delete(e);
            entregaRepository.flush();
            for (Long archivo : archivos) {
                if (archivoRepository.existsById(archivo)) archivoRepository.deleteById(archivo);
            }
        }
        citaRepository.desvincularEstudiante(estudianteId);
    }
}
