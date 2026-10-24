package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.CitaDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.util.RolUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Mi Agenda:
 *  HU-27 Disponibilidad: docentes, administración y directiva registran sus horarios de atención.
 *  HU-28 El padre consulta los horarios libres y solicita una reunión (queda PENDIENTE).
 *  HU-29 El responsable programa una reunión con un padre (queda CONFIRMADA).
 *  HU-22 Cada usuario ve sus citas y recibe avisos (mensajería: "Avisos del colegio").
 *  HU-30 Reprogramar o cancelar (con motivo); la otra parte recibe un aviso.
 */
@Service
public class AgendaService {

    /** Las citas se pueden pedir hasta 4 semanas adelante. */
    public static final int DIAS_ADELANTE = 28;
    /** Anticipación mínima para pedir una cita (horas). */
    public static final int HORAS_ANTICIPACION = 2;

    private static final Set<Integer> DURACIONES = Set.of(15, 20, 30, 45, 60);
    private static final Set<String> MODALIDADES = Set.of("PRESENCIAL", "VIRTUAL");
    private static final List<String> ACTIVAS = List.of(Cita.PENDIENTE, Cita.CONFIRMADA);
    private static final String[] DIAS = {"", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DisponibilidadRepository disponibilidadRepository;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PadreFamiliaRepository padreRepository;
    private final EstudianteRepository estudianteRepository;
    private final PersonalRepository personalRepository;
    private final SeccionCursoRepository seccionCursoRepository;
    private final SeccionRepository seccionRepository;
    private final AccesoService accesoService;
    private final MensajeService mensajeService;

    public AgendaService(DisponibilidadRepository disponibilidadRepository, CitaRepository citaRepository,
                         UsuarioRepository usuarioRepository, PadreFamiliaRepository padreRepository,
                         EstudianteRepository estudianteRepository, PersonalRepository personalRepository,
                         SeccionCursoRepository seccionCursoRepository, SeccionRepository seccionRepository,
                         AccesoService accesoService, MensajeService mensajeService) {
        this.disponibilidadRepository = disponibilidadRepository;
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
        this.padreRepository = padreRepository;
        this.estudianteRepository = estudianteRepository;
        this.personalRepository = personalRepository;
        this.seccionCursoRepository = seccionCursoRepository;
        this.seccionRepository = seccionRepository;
        this.accesoService = accesoService;
        this.mensajeService = mensajeService;
    }

    // ==================================================================
    // HU-27 Disponibilidad
    // ==================================================================

    @Transactional(readOnly = true)
    public List<CitaDto.Disponibilidad> misDisponibilidades(Long usuarioId) {
        Usuario u = exigirResponsable(usuarioId);
        return disponibilidadRepository.findByUsuarioIdOrderByDiaAscHoraInicioAsc(u.getId()).stream()
                .map(AgendaService::disponibilidad).toList();
    }

    @Transactional
    public CitaDto.Disponibilidad crearDisponibilidad(Long usuarioId, CitaDto.DisponibilidadRequest req) {
        Usuario u = exigirResponsable(usuarioId);
        Disponibilidad d = Disponibilidad.builder().usuario(u).build();
        aplicar(d, req);
        return disponibilidad(disponibilidadRepository.save(d));
    }

    @Transactional
    public CitaDto.Disponibilidad actualizarDisponibilidad(Long usuarioId, Long id, CitaDto.DisponibilidadRequest req) {
        Disponibilidad d = miDisponibilidad(usuarioId, id);
        aplicar(d, req);
        return disponibilidad(disponibilidadRepository.save(d));
    }

    @Transactional
    public CitaDto.Disponibilidad cambiarEstadoDisponibilidad(Long usuarioId, Long id, boolean activo) {
        Disponibilidad d = miDisponibilidad(usuarioId, id);
        if (activo) validarCruce(d, d.getDia(), d.getHoraInicio(), d.getHoraFin());
        d.setActivo(activo);
        return disponibilidad(disponibilidadRepository.save(d));
    }

    /** Las citas ya pedidas no se borran: siguen en "Mis citas". */
    @Transactional
    public void eliminarDisponibilidad(Long usuarioId, Long id) {
        disponibilidadRepository.delete(miDisponibilidad(usuarioId, id));
    }

    // ==================================================================
    // HU-28 El padre consulta horarios y solicita
    // ==================================================================

    /** Con quién puede reunirse el padre: docentes/tutores de sus hijos y la administración, con horarios activos. */
    @Transactional(readOnly = true)
    public List<CitaDto.Responsable> responsables(Long usuarioId) {
        PadreFamilia padre = accesoService.padreLogeado(usuarioId);
        List<Estudiante> hijos = accesoService.hijos(padre);
        Map<Long, List<Disponibilidad>> porUsuario = new LinkedHashMap<>();
        for (Disponibilidad d : disponibilidadRepository.findByActivoTrue()) {
            if (Boolean.TRUE.equals(d.getUsuario().getEstado())) {
                porUsuario.computeIfAbsent(d.getUsuario().getId(), k -> new ArrayList<>()).add(d);
            }
        }
        List<CitaDto.Responsable> lista = new ArrayList<>();
        for (List<Disponibilidad> ds : porUsuario.values()) {
            Usuario r = ds.get(0).getUsuario();
            List<String> relacion = relacion(r, hijos);
            if (relacion.isEmpty()) continue;
            ds.sort(Comparator.comparing(Disponibilidad::getDia).thenComparing(Disponibilidad::getHoraInicio));
            lista.add(new CitaDto.Responsable(r.getId(), r.getNombre(), RolUtil.etiqueta(r.getRol()), detalle(r),
                    relacion, ds.stream().map(AgendaService::disponibilidad).toList()));
        }
        lista.sort(Comparator.comparing((CitaDto.Responsable x) -> "Docente".equals(x.rol()) ? 0 : 1)
                .thenComparing(CitaDto.Responsable::nombre, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    /** Horarios libres del responsable en las próximas 4 semanas. */
    @Transactional(readOnly = true)
    public List<CitaDto.Horario> horariosLibres(Long usuarioId, Long responsableId) {
        PadreFamilia padre = accesoService.padreLogeado(usuarioId);
        Usuario r = responsableDelPadre(padre, responsableId);
        return libres(r, padre.getUsuario().getId(), null);
    }

    @Transactional
    public CitaDto.Cita solicitar(Long usuarioId, CitaDto.SolicitarRequest req) {
        PadreFamilia padre = accesoService.padreLogeado(usuarioId);
        if (req == null || req.responsableId() == null) throw new DatosInvalidosException("Elige con quién quieres reunirte.");
        Usuario r = responsableDelPadre(padre, req.responsableId());
        Estudiante hijo = hijoOpcional(padre, req.estudianteId());
        String motivo = motivo(req.motivo());
        CitaDto.Horario h = horarioElegido(r, padre.getUsuario().getId(), null, req.fecha(), req.horaInicio());

        Cita c = citaRepository.save(Cita.builder()
                .responsable(r).padre(padre.getUsuario()).estudiante(hijo)
                .fecha(h.fecha()).horaInicio(hora(h.horaInicio())).horaFin(hora(h.horaFin()))
                .modalidad(h.modalidad()).lugar(h.lugar()).motivo(motivo)
                .detalle(NotaService.recortar(req.detalle(), 2000))
                .origen(Cita.SOLICITUD_PADRE).estado(Cita.PENDIENTE).actualizadoPor(usuarioId)
                .build());
        avisar(r, "Nueva solicitud de reunión", "<p><strong>" + esc(padre.getUsuario().getNombre())
                + "</strong> solicitó una reunión para el " + cuando(c) + ".</p>" + motivoHtml(c.getMotivo())
                + "<p>Confírmala o recházala en <em>Mi Agenda &gt; Mis Citas</em>.</p>");
        return cita(c, usuarioId);
    }

    // ==================================================================
    // HU-29 El responsable programa una reunión
    // ==================================================================

    /** Padres con cuenta a los que el responsable puede citar (docente: los de sus secciones). */
    @Transactional(readOnly = true)
    public List<CitaDto.PadreContacto> padresParaProgramar(Long usuarioId) {
        Usuario u = exigirResponsable(usuarioId);
        Map<Long, CitaDto.PadreContacto> mapa = new LinkedHashMap<>();
        if (AccesoService.esDocente(u)) {
            Set<Long> secciones = accesoService.seccionesDelDocente(u.getId());
            for (Seccion s : seccionRepository.findAllById(secciones)) {
                for (Estudiante e : accesoService.alumnosDe(s)) agregarPadres(mapa, e);
            }
        } else {
            for (PadreFamilia p : padreRepository.findAll()) {
                if (p.getUsuario() == null || !Boolean.TRUE.equals(p.getUsuario().getEstado())) continue;
                List<CitaDto.Hijo> hijos = accesoService.hijos(p).stream()
                        .map(h -> new CitaDto.Hijo(h.getId(), h.getNombreCompleto(), AccesoService.salon(h))).toList();
                mapa.put(p.getUsuario().getId(), new CitaDto.PadreContacto(p.getUsuario().getId(), p.getNombreCompleto(),
                        p.getTelefono(), hijos));
            }
        }
        List<CitaDto.PadreContacto> lista = new ArrayList<>(mapa.values());
        lista.sort(Comparator.comparing(CitaDto.PadreContacto::nombre, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    @Transactional
    public CitaDto.Cita programar(Long usuarioId, CitaDto.ProgramarRequest req) {
        Usuario r = exigirResponsable(usuarioId);
        if (req == null || req.padreId() == null) throw new DatosInvalidosException("Elige al padre o apoderado.");
        CitaDto.PadreContacto contacto = padresParaProgramar(usuarioId).stream()
                .filter(p -> p.usuarioId().equals(req.padreId())).findFirst().orElse(null);
        Usuario padre;
        if (contacto != null) {
            padre = usuarioRepository.findById(contacto.usuarioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Padre de familia no encontrado."));
        } else if (AccesoService.esAdministrativo(r)) {
            // La administración puede citar a docentes, padres o alumnos.
            padre = usuarioRepository.findById(req.padreId())
                    .filter(x -> Boolean.TRUE.equals(x.getEstado()) && !x.getId().equals(r.getId()))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada."));
        } else {
            throw new AccesoDenegadoException("Solo puedes citar a padres o tutores de tus estudiantes.");
        }
        Estudiante hijo = null;
        if (req.estudianteId() != null && contacto != null) {
            if (contacto.hijos().stream().noneMatch(h -> h.id().equals(req.estudianteId()))) {
                throw new DatosInvalidosException("El alumno elegido no es hijo de ese padre de familia.");
            }
            hijo = estudianteRepository.findById(req.estudianteId()).orElse(null);
        }
        LocalTime ini = hora(req.horaInicio());
        LocalTime fin = req.horaFin() != null && !req.horaFin().isBlank() ? hora(req.horaFin())
                : ini.plusMinutes(duracion(req.duracionMinutos()));
        validarHorarioLibre(r, padre, req.fecha(), ini, fin, null);
        // El docente cita dentro de su disponibilidad; la administración, en cualquier horario.
        if (!AccesoService.esAdministrativo(r)) validarDentroDeDisponibilidad(r, req.fecha(), ini, fin);
        String modalidad = modalidad(req.modalidad());

        Cita c = citaRepository.save(Cita.builder()
                .responsable(r).padre(padre).estudiante(hijo)
                .fecha(req.fecha()).horaInicio(ini).horaFin(fin)
                .modalidad(modalidad).lugar(NotaService.recortar(req.lugar(), 300)).motivo(motivo(req.motivo()))
                .detalle(NotaService.recortar(req.detalle(), 2000))
                .origen(Cita.PROGRAMADA).estado(Cita.PENDIENTE).actualizadoPor(usuarioId)
                .build());
        avisar(padre, "Nueva cita", "<p><strong>" + esc(r.getNombre()) + "</strong> ("
                + esc(RolUtil.etiqueta(r.getRol())) + ") te envió una cita para el " + cuando(c) + ".</p>"
                + motivoHtml(c.getMotivo()) + lugarHtml(c)
                + "<p>Acéptala o cancélala en <em>Mis Citas</em>.</p>");
        return cita(c, usuarioId);
    }

    // ==================================================================
    // HU-22 Mis citas
    // ==================================================================

    @Transactional(readOnly = true)
    public List<CitaDto.Cita> misCitas(Long usuarioId) {
        Usuario u = accesoService.usuario(usuarioId);
        List<Cita> lista;
        if (AccesoService.esPadre(u)) lista = citaRepository.findByPadreIdOrderByFechaDescHoraInicioDesc(u.getId());
        else if (AccesoService.esAlumno(u)) {
            Estudiante e = accesoService.alumnoLogeado(usuarioId);
            lista = citaRepository.findByEstudianteIdOrderByFechaDescHoraInicioDesc(e.getId());
        } else {
            // Docente / administración: las que creó o atiende, y las que la administración le envió a él.
            LinkedHashMap<Long, Cita> mapa = new LinkedHashMap<>();
            citaRepository.findByResponsableIdOrderByFechaDescHoraInicioDesc(u.getId()).forEach(c -> mapa.put(c.getId(), c));
            citaRepository.findByPadreIdOrderByFechaDescHoraInicioDesc(u.getId()).forEach(c -> mapa.putIfAbsent(c.getId(), c));
            lista = new ArrayList<>(mapa.values());
            lista.sort(Comparator.comparing(Cita::getFecha).thenComparing(Cita::getHoraInicio).reversed());
        }
        return lista.stream().map(c -> cita(c, usuarioId)).toList();
    }

    @Transactional(readOnly = true)
    public CitaDto.Resumen resumen(Long usuarioId) {
        List<CitaDto.Cita> citas = misCitas(usuarioId);
        long porConfirmar = citas.stream().filter(CitaDto.Cita::puedeConfirmar).count();
        long proximas = citas.stream().filter(c -> Cita.CONFIRMADA.equals(c.estado()) && !c.pasada()).count();
        long esperando = citas.stream().filter(c -> Cita.PENDIENTE.equals(c.estado()) && !c.puedeConfirmar() && !c.pasada()).count();
        return new CitaDto.Resumen(porConfirmar, proximas, esperando);
    }

    // ==================================================================
    // Acciones sobre una cita (HU-22 / HU-30)
    // ==================================================================

    @Transactional
    public CitaDto.Cita confirmar(Long usuarioId, Long citaId, String observacion) {
        Cita c = cita(citaId);
        accesoService.usuario(usuarioId);
        if (!receptor(c).getId().equals(usuarioId)) throw new AccesoDenegadoException("Solo quien recibe la cita puede aceptarla.");
        if (!Cita.PENDIENTE.equals(c.getEstado())) throw new DatosInvalidosException("Solo se confirman citas pendientes.");
        if (pasada(c)) throw new DatosInvalidosException("La fecha de esta cita ya pasó.");
        validarHorarioLibre(c.getResponsable(), c.getPadre(), c.getFecha(), c.getHoraInicio(), c.getHoraFin(), c.getId());
        c.setEstado(Cita.CONFIRMADA);
        actualizar(c, usuarioId, NotaService.recortar(observacion, 500));
        Usuario quien = receptor(c);
        avisar(creador(c), "Reunión confirmada", "<p><strong>" + esc(quien.getNombre())
                + "</strong> aceptó la reunión del " + cuando(c) + ".</p>" + lugarHtml(c) + observacionHtml(c));
        return cita(c, usuarioId);
    }

    /** Quien creó la cita puede eliminarla mientras siga pendiente. */
    @Transactional
    public void eliminar(Long usuarioId, Long citaId) {
        Cita c = cita(citaId);
        accesoService.usuario(usuarioId);
        if (!creador(c).getId().equals(usuarioId)) throw new AccesoDenegadoException("Solo quien envió la cita puede eliminarla.");
        if (!Cita.PENDIENTE.equals(c.getEstado())) throw new DatosInvalidosException("Solo se eliminan citas pendientes.");
        Usuario otro = receptor(c);
        String cuando = cuando(c);
        String nombre = creador(c).getNombre();
        citaRepository.delete(c);
        avisar(otro, "Solicitud de reunión retirada", "<p><strong>" + esc(nombre)
                + "</strong> retiró la solicitud de reunión del " + cuando + ".</p>");
    }

    /** Horas de inicio libres del usuario logeado en una fecha (según su disponibilidad y sus citas). */
    @Transactional(readOnly = true)
    public List<String> misHorasLibres(Long usuarioId, LocalDate fecha, Integer duracionMinutos) {
        Usuario u = exigirResponsable(usuarioId);
        if (fecha == null) throw new DatosInvalidosException("Indica la fecha.");
        int dur = duracion(duracionMinutos);
        LocalDateTime ahora = LocalDateTime.now();
        List<Cita> ocupadas = new ArrayList<>(citaRepository.findByResponsableIdAndFechaAndEstadoIn(u.getId(), fecha, ACTIVAS));
        ocupadas.addAll(citaRepository.findByPadreIdAndFechaAndEstadoIn(u.getId(), fecha, ACTIVAS));
        int dia = fecha.getDayOfWeek().getValue();
        TreeSet<String> horas = new TreeSet<>();
        for (Disponibilidad d : disponibilidadRepository.findByUsuarioIdAndActivoTrue(u.getId())) {
            if (d.getDia() != dia) continue;
            for (LocalTime t = d.getHoraInicio(); !t.plusMinutes(dur).isAfter(d.getHoraFin()); t = t.plusMinutes(15)) {
                LocalTime ini = t, fin = t.plusMinutes(dur);
                if (LocalDateTime.of(fecha, ini).isBefore(ahora)) continue;
                boolean cruza = ocupadas.stream().anyMatch(c -> ini.isBefore(c.getHoraFin()) && fin.isAfter(c.getHoraInicio()));
                if (!cruza) horas.add(ini.format(HHMM));
                if (fin.isBefore(t)) break;
            }
        }
        return new ArrayList<>(horas);
    }

    /** Administración: personas a las que puede citar (padres, docentes y alumnos con cuenta activa). */
    @Transactional(readOnly = true)
    public List<CitaDto.Persona> personasParaCitar(Long usuarioId) {
        Usuario u = exigirResponsable(usuarioId);
        if (!AccesoService.esAdministrativo(u)) throw new AccesoDenegadoException("Esta opción es para la administración.");
        List<CitaDto.Persona> lista = new ArrayList<>();
        for (Usuario x : usuarioRepository.findByEstadoTrueOrderByNombreAsc()) {
            if (x.getId().equals(u.getId())) continue;
            String rol = RolUtil.etiqueta(x.getRol());
            String detalle = "";
            if (AccesoService.esAlumno(x)) {
                detalle = estudianteRepository.findByUsuarioId(x.getId()).map(AccesoService::salon).orElse("");
            } else if (AccesoService.esDocente(x)) {
                detalle = accesoService.docenteDe(x.getId()).map(Docente::getEspecialidad).orElse("");
            } else if (AccesoService.esPadre(x)) {
                detalle = padreRepository.findByUsuarioId(x.getId())
                        .map(p -> "Padre/tutor de " + String.join(", ", accesoService.hijos(p).stream()
                                .map(Estudiante::getNombreCompleto).toList()))
                        .filter(t -> !t.endsWith("de ")).orElse("");
            } else {
                continue;
            }
            lista.add(new CitaDto.Persona(x.getId(), x.getNombre(), rol, detalle == null ? "" : detalle));
        }
        return lista;
    }

    /**
     * Reemplaza la disponibilidad semanal (grilla de 15 minutos). Las citas ya creadas no se tocan.
     * Los padres ven turnos de 30 minutos (o del largo del bloque si es más corto).
     */
    @Transactional
    public List<CitaDto.Disponibilidad> guardarSemana(Long usuarioId, CitaDto.DisponibilidadSemanaRequest req) {
        Usuario u = exigirResponsable(usuarioId);
        List<CitaDto.BloqueSemana> bloques = req == null || req.bloques() == null ? List.of() : req.bloques();
        List<Disponibilidad> nuevas = new ArrayList<>();
        for (CitaDto.BloqueSemana b : bloques) {
            if (b == null || b.dia() == null || b.dia() < 1 || b.dia() > 6) throw new DatosInvalidosException("Día no válido (lunes a sábado).");
            LocalTime ini = hora(b.horaInicio()), fin = hora(b.horaFin());
            if (!fin.isAfter(ini)) throw new DatosInvalidosException("La hora de fin debe ser mayor que la de inicio.");
            if (ini.getMinute() % 15 != 0 || fin.getMinute() % 15 != 0) throw new DatosInvalidosException("Usa bloques de 15 minutos.");
            for (Disponibilidad o : nuevas) {
                if (o.getDia().equals(b.dia()) && ini.isBefore(o.getHoraFin()) && fin.isAfter(o.getHoraInicio())) {
                    throw new DatosInvalidosException("Hay bloques que se cruzan el " + DIAS[b.dia()] + ".");
                }
            }
            nuevas.add(Disponibilidad.builder().usuario(u).dia(b.dia()).horaInicio(ini).horaFin(fin)
                    .duracionMinutos((int) Math.min(30, java.time.Duration.between(ini, fin).toMinutes()))
                    .modalidad("PRESENCIAL").activo(true).build());
        }
        disponibilidadRepository.findByUsuarioIdOrderByDiaAscHoraInicioAsc(u.getId()).forEach(disponibilidadRepository::delete);
        disponibilidadRepository.flush();
        nuevas.forEach(disponibilidadRepository::save);
        return misDisponibilidades(usuarioId);
    }

    @Transactional
    public CitaDto.Cita rechazar(Long usuarioId, Long citaId, String motivo) {
        Cita c = cita(citaId);
        exigirResponsableDe(usuarioId, c);
        if (!Cita.PENDIENTE.equals(c.getEstado())) throw new DatosInvalidosException("Solo se rechazan citas pendientes.");
        c.setEstado(Cita.RECHAZADA);
        actualizar(c, usuarioId, motivoObligatorio(motivo, "rechazo"));
        avisar(c.getPadre(), "Solicitud de reunión rechazada", "<p><strong>" + esc(c.getResponsable().getNombre())
                + "</strong> no podrá atender la reunión del " + cuando(c) + ".</p>" + observacionHtml(c)
                + "<p>Puedes solicitar otro horario en <em>Mis Citas</em>.</p>");
        return cita(c, usuarioId);
    }

    @Transactional
    public CitaDto.Cita cancelar(Long usuarioId, Long citaId, String motivo) {
        Cita c = cita(citaId);
        Usuario u = exigirParticipante(usuarioId, c);
        if (!ACTIVAS.contains(c.getEstado())) throw new DatosInvalidosException("Esta cita ya no está vigente.");
        if (pasada(c)) throw new DatosInvalidosException("La fecha de esta cita ya pasó.");
        c.setEstado(Cita.CANCELADA);
        actualizar(c, usuarioId, motivoObligatorio(motivo, "cancelación"));
        avisar(otro(c, u), "Reunión cancelada", "<p><strong>" + esc(u.getNombre()) + "</strong> canceló la reunión del "
                + cuando(c) + ".</p>" + observacionHtml(c));
        return cita(c, usuarioId);
    }

    /**
     * Padre: elige otro horario libre del responsable -> la cita vuelve a PENDIENTE.
     * Responsable: cualquier horario sin cruces -> queda CONFIRMADA.
     */
    @Transactional
    public CitaDto.Cita reprogramar(Long usuarioId, Long citaId, CitaDto.ReprogramarRequest req) {
        Cita c = cita(citaId);
        Usuario u = exigirParticipante(usuarioId, c);
        if (!ACTIVAS.contains(c.getEstado())) throw new DatosInvalidosException("Esta cita ya no está vigente.");
        if (req == null) throw new DatosInvalidosException("Indica la nueva fecha y hora.");
        String motivo = motivoObligatorio(req.motivo(), "reprogramación");
        String antes = cuando(c);
        boolean esResponsable = c.getResponsable().getId().equals(u.getId());
        if (esResponsable) {
            LocalTime ini = hora(req.horaInicio());
            LocalTime fin = req.horaFin() == null || req.horaFin().isBlank()
                    ? ini.plusMinutes(Duration.between(c.getHoraInicio(), c.getHoraFin()).toMinutes())
                    : hora(req.horaFin());
            validarHorarioLibre(c.getResponsable(), c.getPadre(), req.fecha(), ini, fin, c.getId());
            c.setFecha(req.fecha());
            c.setHoraInicio(ini);
            c.setHoraFin(fin);
            c.setEstado(Cita.CONFIRMADA);
        } else {
            CitaDto.Horario h = horarioElegido(c.getResponsable(), c.getPadre().getId(), c.getId(), req.fecha(), req.horaInicio());
            c.setFecha(h.fecha());
            c.setHoraInicio(hora(h.horaInicio()));
            c.setHoraFin(hora(h.horaFin()));
            c.setModalidad(h.modalidad());
            c.setLugar(h.lugar());
            c.setEstado(Cita.PENDIENTE);
        }
        c.setReprogramaciones(c.getReprogramaciones() + 1);
        actualizar(c, usuarioId, motivo);
        avisar(otro(c, u), "Reunión reprogramada", "<p><strong>" + esc(u.getNombre()) + "</strong> reprogramó la reunión del "
                + antes + " para el <strong>" + cuando(c) + "</strong>.</p>" + observacionHtml(c)
                + (esResponsable ? "" : "<p>Confírmala en <em>Mi Agenda &gt; Mis Citas</em>.</p>"));
        return cita(c, usuarioId);
    }

    @Transactional
    public CitaDto.Cita marcarRealizada(Long usuarioId, Long citaId, String observacion) {
        Cita c = cita(citaId);
        exigirResponsableDe(usuarioId, c);
        if (!Cita.CONFIRMADA.equals(c.getEstado())) throw new DatosInvalidosException("Solo se marcan como realizadas las citas confirmadas.");
        if (c.getFecha().isAfter(LocalDate.now())) throw new DatosInvalidosException("La reunión todavía no ocurre.");
        c.setEstado(Cita.REALIZADA);
        String obs = NotaService.recortar(observacion, 500);
        actualizar(c, usuarioId, obs == null ? c.getObservacion() : obs);
        return cita(c, usuarioId);
    }

    // ==================================================================
    // Cálculo de horarios
    // ==================================================================

    /** Horarios libres de r (sin cruces con sus citas ni con las del padre). ignorarCitaId: la que se reprograma. */
    private List<CitaDto.Horario> libres(Usuario r, Long padreUsuarioId, Long ignorarCitaId) {
        List<Disponibilidad> ds = disponibilidadRepository.findByUsuarioIdAndActivoTrue(r.getId());
        if (ds.isEmpty()) return List.of();
        LocalDate hoy = LocalDate.now();
        LocalDate hasta = hoy.plusDays(DIAS_ADELANTE);
        LocalDateTime minimo = LocalDateTime.now().plusHours(HORAS_ANTICIPACION);
        List<Cita> ocupadas = new ArrayList<>(citaRepository.findByResponsableIdAndFechaBetweenAndEstadoIn(r.getId(), hoy, hasta, ACTIVAS));
        if (padreUsuarioId != null) {
            citaRepository.findByPadreIdOrderByFechaDescHoraInicioDesc(padreUsuarioId).stream()
                    .filter(c -> ACTIVAS.contains(c.getEstado()) && !c.getFecha().isBefore(hoy))
                    .forEach(ocupadas::add);
        }
        ocupadas.removeIf(c -> c.getId().equals(ignorarCitaId));

        List<CitaDto.Horario> lista = new ArrayList<>();
        for (LocalDate f = hoy; !f.isAfter(hasta); f = f.plusDays(1)) {
            int dia = f.getDayOfWeek().getValue();
            for (Disponibilidad d : ds) {
                if (d.getDia() != dia) continue;
                for (LocalTime t = d.getHoraInicio(); !t.plusMinutes(d.getDuracionMinutos()).isAfter(d.getHoraFin());
                     t = t.plusMinutes(d.getDuracionMinutos())) {
                    LocalTime fin = t.plusMinutes(d.getDuracionMinutos());
                    if (LocalDateTime.of(f, t).isBefore(minimo)) continue;
                    final LocalDate fecha = f;
                    final LocalTime ini = t;
                    boolean cruza = ocupadas.stream().anyMatch(c -> c.getFecha().equals(fecha)
                            && ini.isBefore(c.getHoraFin()) && fin.isAfter(c.getHoraInicio()));
                    if (!cruza) lista.add(new CitaDto.Horario(f, ini.format(HHMM), fin.format(HHMM), d.getModalidad(), d.getLugar()));
                    if (fin.equals(LocalTime.MAX) || fin.isBefore(t)) break;
                }
            }
        }
        lista.sort(Comparator.comparing(CitaDto.Horario::fecha).thenComparing(CitaDto.Horario::horaInicio));
        return lista;
    }

    private CitaDto.Horario horarioElegido(Usuario r, Long padreUsuarioId, Long ignorarCitaId, LocalDate fecha, String horaInicio) {
        if (fecha == null || horaInicio == null || horaInicio.isBlank()) throw new DatosInvalidosException("Elige un horario disponible.");
        String hi = hora(horaInicio).format(HHMM);
        return libres(r, padreUsuarioId, ignorarCitaId).stream()
                .filter(h -> h.fecha().equals(fecha) && h.horaInicio().equals(hi))
                .findFirst()
                .orElseThrow(() -> new DatosInvalidosException("Ese horario ya no está disponible. Elige otro."));
    }

    private void validarHorarioLibre(Usuario r, Usuario padre, LocalDate fecha, LocalTime ini, LocalTime fin, Long ignorarCitaId) {
        if (fecha == null) throw new DatosInvalidosException("Indica la fecha de la reunión.");
        if (!fin.isAfter(ini)) throw new DatosInvalidosException("La hora de fin debe ser mayor que la de inicio.");
        if (Duration.between(ini, fin).toMinutes() > 240) throw new DatosInvalidosException("La reunión no puede durar más de 4 horas.");
        if (LocalDateTime.of(fecha, ini).isBefore(LocalDateTime.now())) throw new DatosInvalidosException("La fecha y hora ya pasaron.");
        if (fecha.isAfter(LocalDate.now().plusMonths(6))) throw new DatosInvalidosException("Solo se programan reuniones hasta 6 meses adelante.");
        for (Cita c : citaRepository.findByResponsableIdAndFechaAndEstadoIn(r.getId(), fecha, ACTIVAS)) {
            if (!c.getId().equals(ignorarCitaId) && ini.isBefore(c.getHoraFin()) && fin.isAfter(c.getHoraInicio())) {
                throw new DatosInvalidosException("Ya tienes una reunión de " + c.getHoraInicio().format(HHMM) + " a "
                        + c.getHoraFin().format(HHMM) + " ese día.");
            }
        }
        for (Cita c : citaRepository.findByPadreIdAndFechaAndEstadoIn(padre.getId(), fecha, ACTIVAS)) {
            if (!c.getId().equals(ignorarCitaId) && ini.isBefore(c.getHoraFin()) && fin.isAfter(c.getHoraInicio())) {
                throw new DatosInvalidosException(padre.getNombre() + " ya tiene una reunión de " + c.getHoraInicio().format(HHMM)
                        + " a " + c.getHoraFin().format(HHMM) + " ese día.");
            }
        }
    }

    // ==================================================================
    // Relaciones y permisos
    // ==================================================================

    /** Por qué el padre puede reunirse con r. Vacío = no corresponde. */
    private List<String> relacion(Usuario r, List<Estudiante> hijos) {
        if (AccesoService.esAdministrativo(r)) return List.of("Administración del colegio");
        if (!AccesoService.esDocente(r)) return List.of();
        List<String> lista = new ArrayList<>();
        for (Estudiante h : hijos) {
            Optional<Seccion> s = accesoService.seccionDe(h);
            if (s.isEmpty()) continue;
            String nombre = primerNombre(h.getNombreCompleto());
            if (s.get().getTutor() != null && r.getId().equals(s.get().getTutor().getUsuarioId())) {
                lista.add("Tutor(a) de " + nombre);
            }
            for (SeccionCurso sc : seccionCursoRepository.findBySeccionId(s.get().getId())) {
                if ("Activo".equals(sc.getEstado()) && AccesoService.esDocenteDe(r.getId(), sc)) {
                    lista.add(sc.getCurso().getNombre() + " de " + nombre);
                }
            }
        }
        return lista;
    }

    private String detalle(Usuario r) {
        if (AccesoService.esDocente(r)) {
            return accesoService.docenteDe(r.getId()).map(Docente::getEspecialidad).orElse("Docente");
        }
        return personalRepository.findByUsuarioId(r.getId()).map(Personal::getCargo)
                .filter(c -> c != null && !c.isBlank()).orElse("Administración");
    }

    private Usuario responsableDelPadre(PadreFamilia padre, Long responsableId) {
        Usuario r = usuarioRepository.findById(responsableId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Responsable no encontrado."));
        if (!Boolean.TRUE.equals(r.getEstado()) || relacion(r, accesoService.hijos(padre)).isEmpty()) {
            throw new AccesoDenegadoException("No puedes solicitar reuniones con esa persona.");
        }
        return r;
    }

    private Estudiante hijoOpcional(PadreFamilia padre, Long estudianteId) {
        if (estudianteId == null) return null;
        return accesoService.hijos(padre).stream().filter(h -> h.getId().equals(estudianteId)).findFirst()
                .orElseThrow(() -> new DatosInvalidosException("El alumno elegido no es tu hijo."));
    }

    private void agregarPadres(Map<Long, CitaDto.PadreContacto> mapa, Estudiante e) {
        for (EstudianteFamiliar v : e.getVinculosFamiliares()) {
            PadreFamilia p = v.getPadreFamilia();
            Usuario u = p.getUsuario();
            if (u == null || !Boolean.TRUE.equals(u.getEstado())) continue;
            CitaDto.Hijo hijo = new CitaDto.Hijo(e.getId(), e.getNombreCompleto(), AccesoService.salon(e));
            CitaDto.PadreContacto actual = mapa.get(u.getId());
            List<CitaDto.Hijo> hijos = new ArrayList<>(actual == null ? List.of() : actual.hijos());
            if (hijos.stream().noneMatch(h -> h.id().equals(e.getId()))) hijos.add(hijo);
            mapa.put(u.getId(), new CitaDto.PadreContacto(u.getId(), p.getNombreCompleto(), p.getTelefono(), hijos));
        }
    }

    private Usuario exigirResponsable(Long usuarioId) {
        Usuario u = accesoService.usuario(usuarioId);
        if (!AccesoService.esResponsable(u)) {
            throw new AccesoDenegadoException("Esta opción es para docentes, administración y directiva.");
        }
        return u;
    }

    private void exigirResponsableDe(Long usuarioId, Cita c) {
        accesoService.usuario(usuarioId);
        if (!c.getResponsable().getId().equals(usuarioId)) throw new AccesoDenegadoException("Esta cita no está a tu cargo.");
    }

    private Usuario exigirParticipante(Long usuarioId, Cita c) {
        Usuario u = accesoService.usuario(usuarioId);
        if (!c.getResponsable().getId().equals(u.getId()) && !c.getPadre().getId().equals(u.getId())) {
            throw new AccesoDenegadoException("No participas en esta cita.");
        }
        return u;
    }

    private Disponibilidad miDisponibilidad(Long usuarioId, Long id) {
        Usuario u = exigirResponsable(usuarioId);
        Disponibilidad d = disponibilidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado."));
        if (!d.getUsuario().getId().equals(u.getId())) throw new AccesoDenegadoException("Ese horario no es tuyo.");
        return d;
    }

    private void aplicar(Disponibilidad d, CitaDto.DisponibilidadRequest req) {
        if (req == null) throw new DatosInvalidosException("Faltan los datos del horario.");
        if (req.dia() == null || req.dia() < 1 || req.dia() > 6) throw new DatosInvalidosException("Elige el día (lunes a sábado).");
        LocalTime ini = hora(req.horaInicio()), fin = hora(req.horaFin());
        if (!fin.isAfter(ini)) throw new DatosInvalidosException("La hora de fin debe ser mayor que la de inicio.");
        int dur = req.duracionMinutos() == null ? 30 : req.duracionMinutos();
        if (!DURACIONES.contains(dur)) throw new DatosInvalidosException("La duración de cada cita debe ser 15, 20, 30, 45 o 60 minutos.");
        if (Duration.between(ini, fin).toMinutes() < dur) {
            throw new DatosInvalidosException("El rango debe alcanzar para al menos una cita de " + dur + " minutos.");
        }
        validarCruce(d, req.dia(), ini, fin);
        d.setDia(req.dia());
        d.setHoraInicio(ini);
        d.setHoraFin(fin);
        d.setDuracionMinutos(dur);
        d.setModalidad(modalidad(req.modalidad()));
        d.setLugar(NotaService.recortar(req.lugar(), 300));
    }

    private void validarCruce(Disponibilidad d, Integer dia, LocalTime ini, LocalTime fin) {
        for (Disponibilidad otra : disponibilidadRepository.findByUsuarioIdAndActivoTrue(d.getUsuario().getId())) {
            if (otra.getId().equals(d.getId()) || !otra.getDia().equals(dia)) continue;
            if (ini.isBefore(otra.getHoraFin()) && fin.isAfter(otra.getHoraInicio())) {
                throw new DatosInvalidosException("Se cruza con tu horario del " + DIAS[dia] + " de "
                        + otra.getHoraInicio().format(HHMM) + " a " + otra.getHoraFin().format(HHMM) + ".");
            }
        }
    }

    // ==================================================================
    // Respuestas y avisos
    // ==================================================================

    private CitaDto.Cita cita(Cita c, Long usuarioId) {
        boolean soyResponsable = c.getResponsable().getId().equals(usuarioId);
        boolean soyPadre = c.getPadre().getId().equals(usuarioId);
        boolean pasada = pasada(c);
        boolean activa = ACTIVAS.contains(c.getEstado());
        boolean soyCreador = creador(c).getId().equals(usuarioId);
        boolean soyReceptor = receptor(c).getId().equals(usuarioId);
        boolean pendiente = Cita.PENDIENTE.equals(c.getEstado());
        String miRol = soyResponsable ? "RESPONSABLE" : soyPadre ? "PADRE" : "ALUMNO";
        Usuario r = c.getResponsable();
        Estudiante e = c.getEstudiante();
        PadreFamilia pf = padreRepository.findByUsuarioId(c.getPadre().getId()).orElse(null);
        return new CitaDto.Cita(c.getId(), c.getFecha(), c.getHoraInicio().format(HHMM), c.getHoraFin().format(HHMM),
                c.getModalidad(), c.getLugar(), c.getMotivo(), c.getOrigen(), c.getEstado(), c.getObservacion(),
                new CitaDto.Persona(r.getId(), r.getNombre(), RolUtil.etiqueta(r.getRol()), detalle(r)),
                new CitaDto.Persona(c.getPadre().getId(), pf == null ? c.getPadre().getNombre() : pf.getNombreCompleto(),
                        pf == null ? RolUtil.etiqueta(c.getPadre().getRol()) : "Padre", pf == null ? null : pf.getTelefono()),
                e == null ? null : new CitaDto.Hijo(e.getId(), e.getNombreCompleto(), AccesoService.salon(e)),
                miRol, pasada, c.getReprogramaciones(), c.getFechaCreacion(), c.getFechaActualizacion(),
                // Tabla de acciones: Enviada -> ver (+ eliminar si está pendiente);
                // Recibida -> aceptar y cancelar si está pendiente; cancelar si está confirmada.
                soyReceptor && pendiente && !pasada,
                false,
                false,
                soyReceptor && activa && !pasada,
                false,
                soyCreador ? "ENVIADA" : "RECIBIDA",
                c.getDetalle(),
                soyCreador && pendiente);
    }

    private static CitaDto.Disponibilidad disponibilidad(Disponibilidad d) {
        int citas = (int) (Duration.between(d.getHoraInicio(), d.getHoraFin()).toMinutes() / d.getDuracionMinutos());
        return new CitaDto.Disponibilidad(d.getId(), d.getDia(), d.getHoraInicio().format(HHMM), d.getHoraFin().format(HHMM),
                d.getDuracionMinutos(), d.getModalidad(), d.getLugar(), d.getActivo(), citas);
    }

    private void actualizar(Cita c, Long usuarioId, String observacion) {
        c.setObservacion(observacion);
        c.setActualizadoPor(usuarioId);
        c.setFechaActualizacion(LocalDateTime.now());
        citaRepository.save(c);
    }

    private void avisar(Usuario destino, String asunto, String html) {
        if (destino != null && Boolean.TRUE.equals(destino.getEstado())) {
            mensajeService.enviarAviso(List.of(destino), asunto, html);
        }
    }

    /** Quien creó la cita: la programó el responsable o la solicitó el padre. */
    private static Usuario creador(Cita c) {
        return Cita.SOLICITUD_PADRE.equals(c.getOrigen()) ? c.getPadre() : c.getResponsable();
    }

    private static Usuario receptor(Cita c) {
        return Cita.SOLICITUD_PADRE.equals(c.getOrigen()) ? c.getResponsable() : c.getPadre();
    }

    private static int duracion(Integer d) {
        int v = d == null ? 30 : d;
        if (!DURACIONES.contains(v)) throw new DatosInvalidosException("La duración debe ser 15, 20, 30, 45 o 60 minutos.");
        return v;
    }

    /** El rango [ini, fin) debe caber en alguno de los horarios de disponibilidad de r ese día. */
    private void validarDentroDeDisponibilidad(Usuario r, LocalDate fecha, LocalTime ini, LocalTime fin) {
        int dia = fecha.getDayOfWeek().getValue();
        boolean cabe = disponibilidadRepository.findByUsuarioIdAndActivoTrue(r.getId()).stream()
                .anyMatch(d -> d.getDia() == dia && !ini.isBefore(d.getHoraInicio()) && !fin.isAfter(d.getHoraFin()));
        if (!cabe) throw new DatosInvalidosException("Esa hora no está dentro de tu disponibilidad. Elige una hora disponible.");
    }

    private static Usuario otro(Cita c, Usuario yo) {
        return c.getResponsable().getId().equals(yo.getId()) ? c.getPadre() : c.getResponsable();
    }

    private static boolean pasada(Cita c) {
        return LocalDateTime.of(c.getFecha(), c.getHoraFin()).isBefore(LocalDateTime.now());
    }

    private Cita cita(Long id) {
        return citaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada."));
    }

    /** "martes 14/10/2026 de 15:00 a 15:30" */
    private static String cuando(Cita c) {
        return DIAS[c.getFecha().getDayOfWeek().getValue()] + " " + c.getFecha().format(DMY) + " de "
                + c.getHoraInicio().format(HHMM) + " a " + c.getHoraFin().format(HHMM);
    }

    private static String motivoHtml(String motivo) {
        return motivo == null ? "" : "<p>Motivo: " + esc(motivo) + "</p>";
    }

    private static String observacionHtml(Cita c) {
        return c.getObservacion() == null ? "" : "<p>Comentario: " + esc(c.getObservacion()) + "</p>";
    }

    private static String lugarHtml(Cita c) {
        String modo = "VIRTUAL".equals(c.getModalidad()) ? "Virtual" : "Presencial";
        return "<p>Modalidad: " + modo + (c.getLugar() == null ? "" : " — " + esc(c.getLugar())) + "</p>";
    }

    private static String esc(String t) {
        return MensajeService.escapar(t == null ? "" : t);
    }

    private static String motivo(String m) {
        String v = NotaService.recortar(m, 500);
        if (v == null) throw new DatosInvalidosException("Escribe el motivo de la reunión.");
        if (v.length() < 5) throw new DatosInvalidosException("El motivo debe tener al menos 5 caracteres.");
        return v;
    }

    private static String motivoObligatorio(String m, String que) {
        String v = NotaService.recortar(m, 500);
        if (v == null) throw new DatosInvalidosException("Escribe el motivo de la " + que + ".");
        if (v.length() < 5) throw new DatosInvalidosException("El motivo debe tener al menos 5 caracteres.");
        return v;
    }

    private static String modalidad(String m) {
        String v = m == null || m.isBlank() ? "PRESENCIAL" : m.trim().toUpperCase();
        if (!MODALIDADES.contains(v)) throw new DatosInvalidosException("La modalidad debe ser presencial o virtual.");
        return v;
    }

    private static LocalTime hora(String valor) {
        try {
            return LocalTime.parse(valor == null ? "" : valor.trim(), HHMM);
        } catch (DateTimeParseException e) {
            throw new DatosInvalidosException("Hora no válida: " + valor + " (usa HH:mm).");
        }
    }

    private static String primerNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return "tu hijo(a)";
        return nombre.trim().split("\\s+")[0];
    }
}
