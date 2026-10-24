package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.BorradorDto;
import com.CristoMorado_api.CristoMorado_api.dto.ChatDto;
import com.CristoMorado_api.CristoMorado_api.dto.ContactosResponse;
import com.CristoMorado_api.CristoMorado_api.dto.ContactosResponse.Contacto;
import com.CristoMorado_api.CristoMorado_api.dto.MensajeRequest;
import com.CristoMorado_api.CristoMorado_api.dto.MensajeResponse;
import com.CristoMorado_api.CristoMorado_api.dto.MensajeResponse.Participante;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.util.RolUtil;
import com.CristoMorado_api.CristoMorado_api.util.RolUtil.Categoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Mensajería interna. Reglas de quién puede escribir a quién:
 * - Administración: a cualquier usuario activo.
 * - Docente: a la administración/directiva, a los docentes, a los alumnos de las secciones
 *   donde enseña y a los padres de esos alumnos.
 * - Alumno: a los compañeros de SU salón (mismo grado y sección)
 *   y a los docentes.
 * - Padre: a los docentes y a administración.
 * Además, cualquiera puede RESPONDER a quien le escribió.
 *
 * La vista es de chat: una conversación por persona (ver {@link ChatDto}).
 * Los avisos automáticos del colegio (p. ej. cambio de horario) llegan en
 * la conversación "Avisos del colegio".
 */
@Service
public class MensajeService {

    private static final int MAX_CHAT = 2000;
    /** Asunto interno de los mensajes de chat (no se muestra). */
    public static final String ASUNTO_CHAT = "Chat";
    public static final String NOMBRE_AVISOS = "Avisos del colegio";

    private final MensajeRepository mensajeRepository;
    private final MensajeBorradorRepository borradorRepository;
    private final MensajeDestinatarioRepository destinatarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final SeccionCursoRepository seccionCursoRepository;

    public MensajeService(MensajeRepository mensajeRepository,
                          MensajeDestinatarioRepository destinatarioRepository,
                          UsuarioRepository usuarioRepository,
                          EstudianteRepository estudianteRepository,
                          DocenteRepository docenteRepository,
                          SeccionCursoRepository seccionCursoRepository,
                          MensajeBorradorRepository borradorRepository) {
        this.borradorRepository = borradorRepository;
        this.seccionCursoRepository = seccionCursoRepository;
        this.mensajeRepository = mensajeRepository;
        this.destinatarioRepository = destinatarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.docenteRepository = docenteRepository;
    }

    // ------------------------------------------------------------------
    // Contactos
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ContactosResponse contactos(Long usuarioId) {
        Usuario yo = buscarUsuario(usuarioId);
        Categoria miCategoria = RolUtil.categoria(yo.getRol());

        // usuarioId -> "5° A" (alumnos) y usuarioId -> especialidad (docentes)
        Map<Long, String> salonPorUsuario = new HashMap<>();
        // usuarioId del padre -> salones de sus hijos
        Map<Long, Set<String>> salonesDePadre = new HashMap<>();
        // usuarioId del padre -> "Padre de Juan Pérez García" (se muestra en letra pequeña bajo su nombre)
        Map<Long, String> hijosDePadre = hijosDePadres();
        for (Estudiante e : estudianteRepository.findAll()) {
            if (e.getUsuario() != null) {
                salonPorUsuario.put(e.getUsuario().getId(), salon(e));
            }
            String s = salon(e);
            if (s == null || e.getVinculosFamiliares() == null) continue;
            for (EstudianteFamiliar v : e.getVinculosFamiliares()) {
                if (v.getPadreFamilia() != null && v.getPadreFamilia().getUsuario() != null) {
                    salonesDePadre.computeIfAbsent(v.getPadreFamilia().getUsuario().getId(), k -> new HashSet<>()).add(s);
                }
            }
        }
        // Salones donde enseña el docente
        Set<String> misSalones = new HashSet<>();
        if (miCategoria == Categoria.DOCENTE) {
            for (SeccionCurso sc : seccionCursoRepository.findByDocenteUsuarioId(yo.getId())) {
                if (sc.getSeccion() != null) misSalones.add(sc.getSeccion().getGrado() + " " + sc.getSeccion().getLetra());
            }
        }
        Map<Long, String> especialidadPorUsuario = new HashMap<>();
        for (Docente d : docenteRepository.findAll()) {
            if (d.getUsuarioId() != null && d.getEspecialidad() != null) {
                especialidadPorUsuario.put(d.getUsuarioId(), d.getEspecialidad());
            }
        }
        String miSalon = salonPorUsuario.get(yo.getId());

        // Padre: solo escribe a los docentes que enseñan (o son tutores) en los salones de sus hijos.
        // usuarioId del docente -> "Matemática · 3° Secundaria A" (se muestra bajo su nombre).
        Map<Long, Set<String>> docentesDeMisHijos = new LinkedHashMap<>();
        if (miCategoria == Categoria.PADRE) {
            Set<String> salonesHijos = salonesDePadre.getOrDefault(yo.getId(), Set.of());
            for (SeccionCurso sc : seccionCursoRepository.findAll()) {
                Seccion s = sc.getSeccion();
                if (s == null || !salonesHijos.contains(s.getGrado() + " " + s.getLetra())) continue;
                if (!"Activo".equals(sc.getEstado())) continue;
                if (sc.getDocente() != null && sc.getDocente().getUsuarioId() != null) {
                    docentesDeMisHijos.computeIfAbsent(sc.getDocente().getUsuarioId(), k -> new LinkedHashSet<>())
                            .add(sc.getCurso().getNombre() + " · " + s.getGrado() + " " + s.getLetra());
                }
                if (s.getTutor() != null && s.getTutor().getUsuarioId() != null) {
                    docentesDeMisHijos.computeIfAbsent(s.getTutor().getUsuarioId(), k -> new LinkedHashSet<>())
                            .add("Tutor(a) · " + s.getGrado() + " " + s.getLetra());
                }
            }
        }

        List<Contacto> contactos = new ArrayList<>();
        for (Usuario u : usuarioRepository.findByEstadoTrueOrderByNombreAsc()) {
            if (u.getId().equals(yo.getId())) continue;
            Categoria cat = RolUtil.categoria(u.getRol());
            if (miCategoria == Categoria.PADRE && cat == Categoria.DOCENTE && !docentesDeMisHijos.containsKey(u.getId())) continue;
            if (!puedeEscribirA(miCategoria, miSalon, misSalones, cat, salonPorUsuario.get(u.getId()),
                    salonesDePadre.getOrDefault(u.getId(), Set.of()))) continue;

            String detalle = switch (cat) {
                case ALUMNO -> salonPorUsuario.getOrDefault(u.getId(), "");
                case DOCENTE -> miCategoria == Categoria.PADRE
                        ? String.join(", ", docentesDeMisHijos.get(u.getId()))
                        : especialidadPorUsuario.getOrDefault(u.getId(), "");
                case PADRE -> hijosDePadre.getOrDefault(u.getId(), "");
                default -> "";
            };
            contactos.add(new Contacto(u.getId(), u.getNombre(), RolUtil.etiqueta(u.getRol()), detalle));
        }

        // Ya no se envían mensajes por grupos: el chat es siempre de persona a persona.
        return new ContactosResponse(contactos, List.of());
    }

    private boolean puedeEscribirA(Categoria yo, String miSalon, Set<String> misSalones, Categoria otro,
                                   String salonOtro, Set<String> salonesHijosOtro) {
        return switch (yo) {
            case ADMINISTRATIVO -> true;
            // El docente escribe a la directiva/administración, a otros docentes y a sus alumnos.
            // (A un padre solo puede responderle si el padre le escribió primero.)
            case DOCENTE -> otro == Categoria.ADMINISTRATIVO
                    || otro == Categoria.DOCENTE
                    || (otro == Categoria.ALUMNO && salonOtro != null && misSalones.contains(salonOtro));
            // El alumno escribe a sus profesores y a los compañeros de su salón
            // (a la administración solo puede responderle si ella le escribió primero).
            case ALUMNO -> otro == Categoria.DOCENTE
                    || (otro == Categoria.ALUMNO && miSalon != null && miSalon.equals(salonOtro));
            // El padre escribe a la administración y a los docentes de sus hijos (filtrados antes).
            case PADRE -> otro == Categoria.ADMINISTRATIVO || otro == Categoria.DOCENTE;
            default -> otro == Categoria.ADMINISTRATIVO;
        };
    }

    /** usuarioId del padre -> "Padre de Juan Pérez García, Madre de ..." según sus vínculos. */
    private Map<Long, String> hijosDePadres() {
        Map<Long, List<String>> mapa = new HashMap<>();
        for (Estudiante e : estudianteRepository.findAll()) {
            if (e.getVinculosFamiliares() == null) continue;
            for (EstudianteFamiliar v : e.getVinculosFamiliares()) {
                if (v.getPadreFamilia() == null || v.getPadreFamilia().getUsuario() == null) continue;
                String rel = v.getParentesco() == null || v.getParentesco().isBlank() ? "Apoderado" : v.getParentesco();
                mapa.computeIfAbsent(v.getPadreFamilia().getUsuario().getId(), k -> new ArrayList<>())
                        .add(rel + " de " + e.getNombreCompleto());
            }
        }
        Map<Long, String> res = new HashMap<>();
        mapa.forEach((k, l) -> res.put(k, String.join(" · ", l)));
        return res;
    }

    private static String salon(Estudiante e) {
        if (e.getGrado() == null || e.getSeccion() == null) return null;   // sin matrícula
        return e.getGrado() + " " + e.getSeccion();
    }

    // ------------------------------------------------------------------
    // Bandejas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MensajeResponse> recibidos(Long usuarioId) {
        buscarUsuario(usuarioId);
        return destinatarioRepository.recibidosPor(usuarioId).stream()
                .map(d -> respuesta(d.getMensaje(), "RECIBIDO", d.getLeido()))
                .toList();
    }

    /**
     * Avisos del colegio (cambios de horario, nuevas actividades, notas, citas...) que el usuario
     * aún no leyó, del más reciente al más antiguo (máximo 20). Se muestran en el modal al entrar.
     */
    @Transactional(readOnly = true)
    public List<MensajeResponse> avisosPendientes(Long usuarioId) {
        buscarUsuario(usuarioId);
        return destinatarioRepository.recibidosPor(usuarioId).stream()
                .filter(d -> !Boolean.TRUE.equals(d.getLeido()) && Boolean.TRUE.equals(d.getMensaje().getAvisoSistema()))
                .limit(20)
                .map(d -> respuesta(d.getMensaje(), "RECIBIDO", false))
                .toList();
    }

    /** Marca como leídos varios avisos del usuario (los que no son suyos se ignoran). */
    @Transactional
    public void marcarAvisosLeidos(Long usuarioId, List<Long> mensajeIds) {
        buscarUsuario(usuarioId);
        if (mensajeIds == null) return;
        for (Long id : mensajeIds) {
            if (id == null) continue;
            destinatarioRepository.findByMensajeIdAndDestinatarioId(id, usuarioId).ifPresent(d -> {
                if (!Boolean.TRUE.equals(d.getLeido())) {
                    d.setLeido(true);
                    d.setFechaLectura(LocalDateTime.now());
                    destinatarioRepository.save(d);
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public List<MensajeResponse> enviados(Long usuarioId) {
        buscarUsuario(usuarioId);
        return mensajeRepository.enviadosPor(usuarioId).stream()
                .map(m -> respuesta(m, "ENVIADO", true))
                .toList();
    }

    @Transactional
    public MensajeResponse enviar(Long usuarioId, MensajeRequest req) {
        Usuario yo = buscarUsuario(usuarioId);

        String asunto = req.asunto() == null ? "" : req.asunto().trim();
        if (asunto.isEmpty()) asunto = "(Sin asunto)";
        if (asunto.length() > 200) {
            throw new DatosInvalidosException("El asunto no puede superar los 200 caracteres.");
        }
        String contenido = req.contenido() == null ? "" : req.contenido().trim();
        if (contenido.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").isBlank()) {
            throw new DatosInvalidosException("Escribe el mensaje antes de enviarlo.");
        }

        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (req.destinatariosIds() != null) {
            for (Long id : req.destinatariosIds()) {
                if (id != null && !id.equals(yo.getId())) ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            throw new DatosInvalidosException("Elige al menos un destinatario.");
        }
        if (ids.size() > 1) {
            throw new DatosInvalidosException("Los mensajes son de persona a persona: elige un solo destinatario.");
        }

        // A quién puedo escribir: mis contactos + (si respondo) quien me escribió
        Set<Long> permitidos = new HashSet<>();
        contactos(usuarioId).contactos().forEach(c -> permitidos.add(c.usuarioId()));
        if (req.respuestaAId() != null) {
            Mensaje original = mensajeRepository.findById(req.respuestaAId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("El mensaje original no existe."));
            boolean soyDestinatario = original.getDestinatarios().stream()
                    .anyMatch(d -> d.getDestinatario().getId().equals(yo.getId()));
            boolean soyRemitente = original.getRemitente() != null
                    && original.getRemitente().getId().equals(yo.getId());
            if (soyDestinatario && original.getRemitente() != null) {
                permitidos.add(original.getRemitente().getId());
            }
            if (soyRemitente) {
                original.getDestinatarios().forEach(d -> permitidos.add(d.getDestinatario().getId()));
            }
        }

        Mensaje m = Mensaje.builder()
                .remitente(yo)
                .asunto(asunto)
                .contenido(contenido)
                .respuestaAId(req.respuestaAId())
                .fechaEnvio(LocalDateTime.now())
                .build();

        for (Long id : ids) {
            Usuario destino = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Destinatario no encontrado: " + id));
            if (!permitidos.contains(id)) {
                throw new AccesoDenegadoException("No puedes enviar mensajes a " + destino.getNombre() + ".");
            }
            m.getDestinatarios().add(MensajeDestinatario.builder()
                    .mensaje(m)
                    .destinatario(destino)
                    .build());
        }
        return respuesta(mensajeRepository.save(m), "ENVIADO", true);
    }

    // ------------------------------------------------------------------
    // Borradores (solo los ve su autor; no llegan a nadie hasta enviarlos)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BorradorDto.Borrador> borradores(Long usuarioId) {
        buscarUsuario(usuarioId);
        return borradorRepository.findByUsuarioIdOrderByFechaActualizacionDesc(usuarioId).stream()
                .map(MensajeService::borrador).toList();
    }

    @Transactional
    public BorradorDto.Borrador guardarBorrador(Long usuarioId, BorradorDto.Request req) {
        Usuario yo = buscarUsuario(usuarioId);
        if (req == null) throw new DatosInvalidosException("No hay nada que guardar.");
        String asunto = req.asunto() == null ? "" : req.asunto().trim();
        if (asunto.length() > 200) throw new DatosInvalidosException("El asunto no puede superar los 200 caracteres.");
        String contenido = req.contenido() == null ? "" : req.contenido().trim();
        if (contenido.length() > 200_000) throw new DatosInvalidosException("El mensaje es demasiado largo.");
        boolean vacio = asunto.isEmpty() && contenido.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").isBlank();
        if (vacio && req.destinatarioId() == null) throw new DatosInvalidosException("Escribe algo antes de guardar el borrador.");

        MensajeBorrador b = req.id() == null ? MensajeBorrador.builder().usuario(yo).build()
                : borradorRepository.findByIdAndUsuarioId(req.id(), usuarioId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Borrador no encontrado."));
        Usuario destino = null;
        if (req.destinatarioId() != null) {
            destino = usuarioRepository.findById(req.destinatarioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Destinatario no encontrado."));
        }
        b.setDestinatario(destino);
        b.setAsunto(asunto);
        b.setContenido(contenido);
        b.setRespuestaAId(req.respuestaAId());
        b.setFechaActualizacion(LocalDateTime.now());
        return borrador(borradorRepository.save(b));
    }

    @Transactional
    public void eliminarBorrador(Long usuarioId, Long id) {
        borradorRepository.delete(borradorRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Borrador no encontrado.")));
    }

    private static BorradorDto.Borrador borrador(MensajeBorrador b) {
        Usuario d = b.getDestinatario();
        return new BorradorDto.Borrador(b.getId(), d == null ? null : d.getId(), d == null ? null : d.getNombre(),
                d == null ? null : RolUtil.etiqueta(d.getRol()), b.getAsunto(), b.getContenido(), b.getRespuestaAId(),
                b.getFechaActualizacion());
    }

    @Transactional
    public void marcarLeido(Long mensajeId, Long usuarioId) {
        MensajeDestinatario d = destinatarioRepository.findByMensajeIdAndDestinatarioId(mensajeId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado."));
        if (!Boolean.TRUE.equals(d.getLeido())) {
            d.setLeido(true);
            d.setFechaLectura(LocalDateTime.now());
            destinatarioRepository.save(d);
        }
    }

    /**
     * Quita el mensaje de la bandeja del usuario (no de la de los demás).
     * Cuando ya nadie lo tiene en su bandeja, se borra de la base de datos.
     */
    @Transactional
    public void eliminar(Long mensajeId, Long usuarioId) {
        Mensaje m = mensajeRepository.findById(mensajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado."));
        boolean afectado = false;

        if (m.getRemitente() != null && m.getRemitente().getId().equals(usuarioId)) {
            m.setEliminadoRemitente(true);
            afectado = true;
        }
        for (MensajeDestinatario d : m.getDestinatarios()) {
            if (d.getDestinatario().getId().equals(usuarioId)) {
                d.setEliminado(true);
                afectado = true;
            }
        }
        if (!afectado) {
            throw new RecursoNoEncontradoException("Mensaje no encontrado.");
        }

        boolean remitenteLoQuito = m.getRemitente() == null || Boolean.TRUE.equals(m.getEliminadoRemitente());
        boolean todosLoQuitaron = m.getDestinatarios().stream().allMatch(d -> Boolean.TRUE.equals(d.getEliminado()));
        if (remitenteLoQuito && todosLoQuitaron) {
            mensajeRepository.delete(m);
        } else {
            mensajeRepository.save(m);
        }
    }

    @Transactional(readOnly = true)
    public long contarNoLeidos(Long usuarioId) {
        return destinatarioRepository.countByDestinatarioIdAndLeidoFalseAndEliminadoFalse(usuarioId);
    }


    // ------------------------------------------------------------------
    // Chat: una conversación por persona
    // ------------------------------------------------------------------

    /** Lista de chats del usuario, el más reciente primero. */
    @Transactional(readOnly = true)
    public List<ChatDto.Conversacion> conversaciones(Long usuarioId) {
        buscarUsuario(usuarioId);
        Map<Long, Acumulado> porOtro = new LinkedHashMap<>();

        for (MensajeDestinatario d : destinatarioRepository.recibidosPor(usuarioId)) {
            Mensaje m = d.getMensaje();
            Acumulado a = porOtro.computeIfAbsent(otroDe(m), k -> new Acumulado());
            a.agregar(m, false, m.getRemitente());
            a.meEscribio = true;
            if (!Boolean.TRUE.equals(d.getLeido())) a.noLeidos++;
        }
        for (Mensaje m : mensajeRepository.enviadosPor(usuarioId)) {
            for (MensajeDestinatario d : m.getDestinatarios()) {
                porOtro.computeIfAbsent(d.getDestinatario().getId(), k -> new Acumulado())
                        .agregar(m, true, d.getDestinatario());
            }
        }

        Map<Long, Contacto> ctx = contexto(usuarioId);
        List<ChatDto.Conversacion> lista = new ArrayList<>();
        porOtro.forEach((otroId, a) -> lista.add(cabecera(otroId, a.usuario, ctx, a.ultimo, a.ultimoEsMio, a.noLeidos, a.meEscribio)));
        lista.sort(Comparator.comparing(ChatDto.Conversacion::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return lista;
    }

    /** Mensajes con una persona (o los avisos del colegio). Marca como leído lo recibido. */
    @Transactional
    public ChatDto.Chat conversacion(Long usuarioId, Long otroId) {
        buscarUsuario(usuarioId);
        if (otroId == null) throw new DatosInvalidosException("Falta la conversación.");
        Usuario otro = otroId > 0
                ? usuarioRepository.findById(otroId).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."))
                : null;

        List<ChatDto.MensajeChat> mensajes = new ArrayList<>();
        List<Mensaje> ordenados = new ArrayList<>();
        boolean meEscribio = false;
        for (MensajeDestinatario d : destinatarioRepository.recibidosPor(usuarioId)) {
            Mensaje m = d.getMensaje();
            if (!otroId.equals(otroDe(m))) continue;
            if (!Boolean.TRUE.equals(d.getLeido())) {
                d.setLeido(true);
                d.setFechaLectura(LocalDateTime.now());
                destinatarioRepository.save(d);
            }
            mensajes.add(burbuja(m, false, true));
            ordenados.add(m);
            meEscribio = true;
        }
        if (otroId > 0) {
            for (Mensaje m : mensajeRepository.enviadosPor(usuarioId)) {
                m.getDestinatarios().stream()
                        .filter(d -> d.getDestinatario().getId().equals(otroId))
                        .findFirst()
                        .ifPresent(d -> { mensajes.add(burbuja(m, true, Boolean.TRUE.equals(d.getLeido()))); ordenados.add(m); });
            }
        }
        mensajes.sort(Comparator.comparing(ChatDto.MensajeChat::fecha).thenComparing(ChatDto.MensajeChat::id));
        ordenados.sort(Comparator.comparing(Mensaje::getFechaEnvio).thenComparing(Mensaje::getId));
        Mensaje ultimo = ordenados.isEmpty() ? null : ordenados.get(ordenados.size() - 1);
        boolean ultimoEsMio = ultimo != null && ultimo.getRemitente() != null && ultimo.getRemitente().getId().equals(usuarioId);
        return new ChatDto.Chat(cabecera(otroId, otro, contexto(usuarioId), ultimo, ultimoEsMio, 0, meEscribio), mensajes);
    }

    /** Envía un mensaje de chat (texto plano) a una persona. */
    @Transactional
    public ChatDto.MensajeChat enviarChat(Long usuarioId, Long otroId, ChatDto.EnviarRequest req) {
        if (otroId == null || otroId <= 0) {
            throw new DatosInvalidosException("Los avisos del colegio no se pueden responder.");
        }
        String texto = req == null || req.contenido() == null ? "" : req.contenido().strip();
        if (texto.isEmpty()) throw new DatosInvalidosException("Escribe el mensaje antes de enviarlo.");
        if (texto.length() > MAX_CHAT) {
            throw new DatosInvalidosException("El mensaje no puede superar los " + MAX_CHAT + " caracteres.");
        }
        // Si el otro me escribió antes, puedo contestarle aunque no esté en mis contactos.
        Long ultimoRecibido = destinatarioRepository.recibidosPor(usuarioId).stream()
                .map(MensajeDestinatario::getMensaje)
                .filter(m -> otroId.equals(otroDe(m)))
                .max(Comparator.comparing(Mensaje::getFechaEnvio).thenComparing(Mensaje::getId))
                .map(Mensaje::getId).orElse(null);
        MensajeResponse r = enviar(usuarioId, new MensajeRequest(List.of(otroId), ASUNTO_CHAT, aHtml(texto), ultimoRecibido));
        return new ChatDto.MensajeChat(r.id(), null, r.contenido(), r.fechaEnvio(), true, false, false);
    }

    /** Quita toda la conversación de MI lista (el otro la conserva). */
    @Transactional
    public void eliminarConversacion(Long usuarioId, Long otroId) {
        ChatDto.Chat chat = conversacion(usuarioId, otroId);
        if (chat.mensajes().isEmpty()) throw new RecursoNoEncontradoException("La conversación no existe.");
        chat.mensajes().forEach(m -> eliminar(m.id(), usuarioId));
    }

    /**
     * Aviso automático del colegio a varios usuarios (sin remitente).
     * Lo usa, por ejemplo, el editor de horario cuando se guarda un cambio.
     */
    @Transactional
    public int enviarAviso(Collection<Usuario> destinos, String asunto, String contenidoHtml) {
        Mensaje m = Mensaje.builder()
                .remitente(null)
                .avisoSistema(true)
                .asunto(asunto)
                .contenido(contenidoHtml)
                .fechaEnvio(LocalDateTime.now())
                .build();
        Set<Long> vistos = new HashSet<>();
        for (Usuario u : destinos) {
            if (u == null || !vistos.add(u.getId())) continue;
            m.getDestinatarios().add(MensajeDestinatario.builder().mensaje(m).destinatario(u).build());
        }
        if (m.getDestinatarios().isEmpty()) return 0;
        mensajeRepository.save(m);
        return m.getDestinatarios().size();
    }

    /** Escapa HTML y convierte saltos de línea en &lt;br&gt;. */
    public static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String aHtml(String texto) {
        return escapar(texto).replace("\r\n", "\n").replace("\n", "<br>");
    }

    /** Con quién es la conversación de un mensaje RECIBIDO. */
    private static Long otroDe(Mensaje m) {
        if (Boolean.TRUE.equals(m.getAvisoSistema())) return ChatDto.AVISOS;
        return m.getRemitente() == null ? ChatDto.ELIMINADO : m.getRemitente().getId();
    }

    private static class Acumulado {
        Mensaje ultimo;
        boolean ultimoEsMio;
        Usuario usuario;
        long noLeidos;
        boolean meEscribio;

        void agregar(Mensaje m, boolean mio, Usuario otro) {
            if (otro != null) usuario = otro;
            if (ultimo == null || m.getFechaEnvio().isAfter(ultimo.getFechaEnvio())
                    || (m.getFechaEnvio().equals(ultimo.getFechaEnvio()) && m.getId() > ultimo.getId())) {
                ultimo = m;
                ultimoEsMio = mio;
            }
        }
    }

    /** Mis contactos por usuarioId (para rol/detalle y saber si puedo escribir). */
    private Map<Long, Contacto> contexto(Long usuarioId) {
        Map<Long, Contacto> mapa = new HashMap<>();
        contactos(usuarioId).contactos().forEach(c -> mapa.put(c.usuarioId(), c));
        return mapa;
    }

    private ChatDto.Conversacion cabecera(Long otroId, Usuario otro, Map<Long, Contacto> ctx, Mensaje ultimo,
                                          boolean ultimoEsMio, long noLeidos, boolean meEscribio) {
        String resumen = ultimo == null ? "" : resumen(ultimo);
        LocalDateTime fecha = ultimo == null ? null : ultimo.getFechaEnvio();
        if (otroId.longValue() == ChatDto.AVISOS) {
            return new ChatDto.Conversacion(otroId, NOMBRE_AVISOS, "Colegio", "Avisos automáticos",
                    resumen, false, fecha, noLeidos, false);
        }
        if (otroId.longValue() == ChatDto.ELIMINADO || otro == null) {
            return new ChatDto.Conversacion(otroId, "Usuario eliminado", "", "",
                    resumen, ultimoEsMio, fecha, noLeidos, false);
        }
        Contacto c = ctx.get(otroId);
        // Puedo escribirle si es mi contacto o si alguna vez me escribió (para responder).
        boolean puede = Boolean.TRUE.equals(otro.getEstado()) && (c != null || meEscribio);
        String detalle = c != null ? c.detalle()
                : RolUtil.categoria(otro.getRol()) == Categoria.PADRE ? hijosDePadres().getOrDefault(otroId, "") : "";
        return new ChatDto.Conversacion(otroId, otro.getNombre(), RolUtil.etiqueta(otro.getRol()),
                detalle, resumen, ultimoEsMio, fecha, noLeidos, puede);
    }

    private ChatDto.MensajeChat burbuja(Mensaje m, boolean mio, boolean leido) {
        String asunto = m.getAsunto();
        if (asunto == null || asunto.isBlank() || ASUNTO_CHAT.equals(asunto) || "(Sin asunto)".equals(asunto)) asunto = null;
        return new ChatDto.MensajeChat(m.getId(), asunto, m.getContenido(), m.getFechaEnvio(), mio, leido,
                Boolean.TRUE.equals(m.getAvisoSistema()));
    }

    /** Texto plano corto para la lista de chats. */
    private static String resumen(Mensaje m) {
        String texto = (m.getContenido() == null ? "" : m.getContenido())
                .replaceAll("(?i)<br\\s*/?>|</p>", " ")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&")
                .replaceAll("\\s+", " ").strip();
        if (Boolean.TRUE.equals(m.getAvisoSistema()) || !(ASUNTO_CHAT.equals(m.getAsunto()) || "(Sin asunto)".equals(m.getAsunto()))) {
            texto = m.getAsunto() + (texto.isEmpty() ? "" : ": " + texto);
        }
        return texto.length() > 90 ? texto.substring(0, 89).strip() + "…" : texto;
    }

    // ------------------------------------------------------------------

    private MensajeResponse respuesta(Mensaje m, String bandeja, Boolean leido) {
        Usuario r = m.getRemitente();
        Participante remitente = r == null
                ? (Boolean.TRUE.equals(m.getAvisoSistema())
                    ? new Participante(null, NOMBRE_AVISOS, "Colegio", true)
                    : new Participante(null, "Usuario eliminado", "", true))
                : new Participante(r.getId(), r.getNombre(), RolUtil.etiqueta(r.getRol()), true);
        List<Participante> destinatarios = m.getDestinatarios().stream()
                .map(d -> new Participante(d.getDestinatario().getId(), d.getDestinatario().getNombre(),
                        RolUtil.etiqueta(d.getDestinatario().getRol()), d.getLeido()))
                .toList();
        return new MensajeResponse(m.getId(), m.getAsunto(), m.getContenido(), m.getFechaEnvio(),
                m.getRespuestaAId(), remitente, destinatarios, leido, bandeja);
    }

    private Usuario buscarUsuario(Long id) {
        if (id == null) throw new DatosInvalidosException("Falta el usuario.");
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }
}
