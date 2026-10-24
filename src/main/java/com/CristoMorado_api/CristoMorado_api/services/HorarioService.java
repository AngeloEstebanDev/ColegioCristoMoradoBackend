package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.HorarioDto;
import com.CristoMorado_api.CristoMorado_api.entity.Docente;
import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import com.CristoMorado_api.CristoMorado_api.entity.HorarioBloque;
import com.CristoMorado_api.CristoMorado_api.entity.Seccion;
import com.CristoMorado_api.CristoMorado_api.entity.SeccionCurso;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.HorarioBloqueRepository;
import com.CristoMorado_api.CristoMorado_api.repository.SeccionCursoRepository;
import com.CristoMorado_api.CristoMorado_api.repository.SeccionRepository;
import com.CristoMorado_api.CristoMorado_api.util.Turnos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Editor de horario de una sección (Académico > Grados y Secciones > Horario).
 * El turno sale de la letra: A = mañana, B = tarde (ver {@link Turnos}).
 * Cuando se guarda un cambio, los alumnos de la sección reciben un aviso
 * en su mensajería ("Avisos del colegio") para que revisen su horario.
 */
@Service
public class HorarioService {

    private static final String[] DIAS = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final SeccionRepository seccionRepository;
    private final SeccionCursoRepository seccionCursoRepository;
    private final HorarioBloqueRepository bloqueRepository;
    private final EstudianteRepository estudianteRepository;
    private final MensajeService mensajeService;

    public HorarioService(SeccionRepository seccionRepository, SeccionCursoRepository seccionCursoRepository,
                          HorarioBloqueRepository bloqueRepository, EstudianteRepository estudianteRepository,
                          MensajeService mensajeService) {
        this.seccionRepository = seccionRepository;
        this.seccionCursoRepository = seccionCursoRepository;
        this.bloqueRepository = bloqueRepository;
        this.estudianteRepository = estudianteRepository;
        this.mensajeService = mensajeService;
    }

    @Transactional(readOnly = true)
    public HorarioDto.Respuesta obtener(Long seccionId) {
        Seccion s = buscar(seccionId);
        return respuesta(s, turno(s));
    }

    /** Reemplaza todo el horario de la sección (botón "Guardar" del editor). */
    @Transactional
    public HorarioDto.Respuesta guardar(Long seccionId, HorarioDto.GuardarRequest req) {
        Seccion s = buscar(seccionId);
        Turnos.Turno t = turno(s);
        List<HorarioDto.BloqueRequest> entrada = req == null || req.bloques() == null ? List.of() : req.bloques();

        List<HorarioBloque> nuevos = new ArrayList<>();
        for (HorarioDto.BloqueRequest b : entrada) {
            if (b == null || b.seccionCursoId() == null) throw new DatosInvalidosException("Cada bloque debe tener un curso.");
            SeccionCurso sc = seccionCursoRepository.findById(b.seccionCursoId())
                    .filter(x -> x.getSeccion().getId().equals(s.getId()))
                    .orElseThrow(() -> new DatosInvalidosException("El curso elegido no pertenece a esta sección."));
            if (b.dia() == null || b.dia() < 1 || b.dia() > 5) throw new DatosInvalidosException("El día debe ser de lunes a viernes.");
            LocalTime ini = hora(b.horaInicio()), fin = hora(b.horaFin());
            String donde = sc.getCurso().getNombre() + " (" + DIAS[b.dia()] + " " + texto(ini) + " - " + texto(fin) + ")";
            if (!fin.isAfter(ini)) throw new DatosInvalidosException(donde + ": la hora fin debe ser mayor que la de inicio.");
            if (!Turnos.enGrilla(t, ini) || !Turnos.enGrilla(t, fin)) {
                throw new DatosInvalidosException(donde + ": las horas deben ir en bloques de 15 minutos desde el inicio del turno ("
                        + texto(t.inicio()) + ") o desde el fin del recreo (" + texto(t.recesoFin()) + ").");
            }
            if (ini.isBefore(t.inicio()) || fin.isAfter(t.fin())) {
                throw new DatosInvalidosException(donde + ": está fuera del turno " + t.nombre().toLowerCase()
                        + " (" + texto(t.inicio()) + " a " + texto(t.fin()) + ").");
            }
            if (ini.isBefore(t.recesoFin()) && fin.isAfter(t.recesoInicio())) {
                throw new DatosInvalidosException(donde + ": cruza el receso (" + texto(t.recesoInicio()) + " a " + texto(t.recesoFin()) + ").");
            }
            for (HorarioBloque otro : nuevos) {
                if (otro.getDia().equals(b.dia()) && ini.isBefore(otro.getHoraFin()) && fin.isAfter(otro.getHoraInicio())) {
                    throw new DatosInvalidosException(donde + ": se cruza con " + otro.getSeccionCurso().getCurso().getNombre()
                            + " (" + texto(otro.getHoraInicio()) + " - " + texto(otro.getHoraFin()) + ").");
                }
            }
            nuevos.add(HorarioBloque.builder().seccion(s).seccionCurso(sc).dia(b.dia()).horaInicio(ini).horaFin(fin).build());
        }
        validarCruceDocentes(s, nuevos);
        List<HorarioBloque> anteriores = bloqueRepository.findBySeccionId(s.getId());
        List<String> cambiados = cursosCambiados(anteriores, nuevos);
        bloqueRepository.deleteAll(anteriores);
        bloqueRepository.flush();
        nuevos.forEach(bloqueRepository::save);
        if (!cambiados.isEmpty()) avisarAlumnos(s, cambiados);
        return respuesta(s, t);
    }

    /**
     * Un docente no puede tener dos clases a la misma hora en secciones distintas.
     * (Los cruces dentro de la misma sección se revisan al armar la lista de bloques.)
     */
    private void validarCruceDocentes(Seccion s, List<HorarioBloque> nuevos) {
        Map<Long, List<HorarioBloque>> otrasClases = new HashMap<>();   // usuarioId del docente -> bloques en otras secciones
        for (HorarioBloque b : nuevos) {
            Docente d = b.getSeccionCurso().getDocente();
            if (d == null || d.getUsuarioId() == null) continue;
            List<HorarioBloque> otras = otrasClases.computeIfAbsent(d.getUsuarioId(), uid -> {
                List<HorarioBloque> lista = new ArrayList<>();
                for (SeccionCurso sc : seccionCursoRepository.findByDocenteUsuarioId(uid)) {
                    if (sc.getSeccion() == null || sc.getSeccion().getId().equals(s.getId())) continue;
                    if (!"Activo".equals(sc.getEstado())) continue;
                    lista.addAll(bloqueRepository.findBySeccionCursoId(sc.getId()));
                }
                return lista;
            });
            for (HorarioBloque o : otras) {
                if (o.getDia().equals(b.getDia()) && b.getHoraInicio().isBefore(o.getHoraFin()) && b.getHoraFin().isAfter(o.getHoraInicio())) {
                    throw new DatosInvalidosException(b.getSeccionCurso().getCurso().getNombre() + " (" + DIAS[b.getDia()] + " "
                            + texto(b.getHoraInicio()) + " - " + texto(b.getHoraFin()) + "): " + d.getNombreCompleto()
                            + " ya tiene clase de " + o.getSeccionCurso().getCurso().getNombre() + " en "
                            + AccesoService.salon(o.getSeccionCurso().getSeccion()) + " de " + texto(o.getHoraInicio())
                            + " a " + texto(o.getHoraFin()) + ".");
                }
            }
        }
    }

    /** Nombres de los cursos cuyos bloques (día y horas) cambiaron. */
    private static List<String> cursosCambiados(List<HorarioBloque> antes, List<HorarioBloque> despues) {
        Map<Long, Set<String>> a = huella(antes), d = huella(despues);
        Map<Long, String> nombres = new HashMap<>();
        for (HorarioBloque b : antes) nombres.put(b.getSeccionCurso().getId(), b.getSeccionCurso().getCurso().getNombre());
        for (HorarioBloque b : despues) nombres.put(b.getSeccionCurso().getId(), b.getSeccionCurso().getCurso().getNombre());
        Set<String> res = new TreeSet<>();
        nombres.forEach((id, nombre) -> {
            if (!Objects.equals(a.getOrDefault(id, Set.of()), d.getOrDefault(id, Set.of()))) res.add(nombre);
        });
        return new ArrayList<>(res);
    }

    private static Map<Long, Set<String>> huella(List<HorarioBloque> bloques) {
        Map<Long, Set<String>> m = new HashMap<>();
        for (HorarioBloque b : bloques) {
            m.computeIfAbsent(b.getSeccionCurso().getId(), k -> new HashSet<>())
                    .add(b.getDia() + "|" + b.getHoraInicio() + "|" + b.getHoraFin());
        }
        return m;
    }

    /** Mensaje automático a cada alumno (con cuenta) de la sección. */
    private void avisarAlumnos(Seccion s, List<String> cursos) {
        List<com.CristoMorado_api.CristoMorado_api.entity.Usuario> destinos = new ArrayList<>();
        for (Estudiante e : estudianteRepository.findByGradoAndSeccionOrderByNombreCompletoAsc(s.getGrado(), s.getLetra())) {
            if (e.getUsuario() != null && Boolean.TRUE.equals(e.getUsuario().getEstado())) destinos.add(e.getUsuario());
        }
        if (destinos.isEmpty()) return;
        String salon = MensajeService.escapar(s.getGrado() + " - Sección " + s.getLetra());
        String lista = MensajeService.escapar(String.join(", ", cursos));
        String html = "<p>Se hizo un cambio en el horario de <strong>" + salon + "</strong>.</p>"
                + "<p>" + (cursos.size() == 1 ? "Curso con cambios: " : "Cursos con cambios: ") + lista + ".</p>"
                + "<p>Por favor, revisa tu horario.</p>";
        mensajeService.enviarAviso(destinos, "Cambio de horario", html);
    }

    // ------------------------------------------------------------------

    private HorarioDto.Respuesta respuesta(Seccion s, Turnos.Turno t) {
        List<HorarioDto.Bloque> bloques = bloqueRepository.findBySeccionId(s.getId()).stream()
                .sorted(Comparator.comparing(HorarioBloque::getDia).thenComparing(HorarioBloque::getHoraInicio))
                .map(b -> {
                    SeccionCurso sc = b.getSeccionCurso();
                    return new HorarioDto.Bloque(b.getId(), sc.getId(), sc.getCurso().getNombre(), sc.getCurso().getColor(),
                            sc.getDocente() == null ? null : sc.getDocente().getNombreCompleto(), sc.getEstado(),
                            b.getDia(), texto(b.getHoraInicio()), texto(b.getHoraFin()));
                })
                .toList();
        return new HorarioDto.Respuesta(s.getId(), s.getGrado(), s.getLetra(), t.nombre(),
                texto(t.inicio()), texto(t.fin()), texto(t.recesoInicio()), texto(t.recesoFin()),
                Turnos.MINUTOS_BLOQUE, bloques);
    }

    private static Turnos.Turno turno(Seccion s) {
        Turnos.Turno t = Turnos.de(s.getGrado(), s.getLetra(), s.getTurno());
        if (t == null) throw new DatosInvalidosException("La sección " + s.getLetra() + " de " + s.getGrado()
                + " no tiene un turno definido (Mañana o Tarde).");
        return t;
    }

    private static LocalTime hora(String valor) {
        try {
            return LocalTime.parse(valor == null ? "" : valor.trim(), HHMM);
        } catch (DateTimeParseException e) {
            throw new DatosInvalidosException("Hora no válida: " + valor + " (usa HH:mm).");
        }
    }

    private static String texto(LocalTime h) {
        return h.format(HHMM);
    }

    private Seccion buscar(Long id) {
        return seccionRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sección no encontrada."));
    }
}
