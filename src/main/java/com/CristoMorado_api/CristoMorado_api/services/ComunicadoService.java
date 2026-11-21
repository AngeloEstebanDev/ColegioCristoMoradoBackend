package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.ComunicadoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.ComunicadoResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Comunicado;
import com.CristoMorado_api.CristoMorado_api.entity.ComunicadoLectura;
import com.CristoMorado_api.CristoMorado_api.entity.Seccion;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.ComunicadoLecturaRepository;
import com.CristoMorado_api.CristoMorado_api.repository.ComunicadoRepository;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PadreFamiliaRepository;
import com.CristoMorado_api.CristoMorado_api.repository.SeccionRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.util.Grados;
import com.CristoMorado_api.CristoMorado_api.util.Permiso;
import com.CristoMorado_api.CristoMorado_api.util.RolUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ComunicadoService {

    private static final int IMAGEN_MAX = 3_000_000;

    private static final Set<String> GRUPOS_DOCENTE = Set.of(RolUtil.GRUPO_ALUMNOS, RolUtil.GRUPO_PADRES);

    private final ComunicadoRepository comunicadoRepository;
    private final ComunicadoLecturaRepository lecturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermisoService permisoService;
    private final AccesoService accesoService;
    private final SeccionRepository seccionRepository;
    private final EstudianteRepository estudianteRepository;
    private final PadreFamiliaRepository padreRepository;

    public ComunicadoService(ComunicadoRepository comunicadoRepository,
                             ComunicadoLecturaRepository lecturaRepository,
                             UsuarioRepository usuarioRepository,
                             PermisoService permisoService,
                             AccesoService accesoService,
                             SeccionRepository seccionRepository,
                             EstudianteRepository estudianteRepository,
                             PadreFamiliaRepository padreRepository) {
        this.comunicadoRepository = comunicadoRepository;
        this.lecturaRepository = lecturaRepository;
        this.usuarioRepository = usuarioRepository;
        this.permisoService = permisoService;
        this.accesoService = accesoService;
        this.seccionRepository = seccionRepository;
        this.estudianteRepository = estudianteRepository;
        this.padreRepository = padreRepository;
    }

    private record Contexto(Usuario usuario, boolean gestiona, Set<Long> secciones) {}

    @Transactional(readOnly = true)
    public List<ComunicadoResponse> listar(Long usuarioId) {
        Contexto ctx = contexto(usuarioId);
        Set<Long> leidos = new HashSet<>(lecturaRepository.idsLeidosPor(usuarioId));
        Map<Long, Long> lecturas = conteoLecturas();
        Map<Long, String> nombres = nombresSecciones();

        return comunicadoRepository.findAllByOrderByFechaCreacionDesc().stream()
                .filter(c -> puedeVer(ctx, c))
                .map(c -> ComunicadoResponse.of(c, leidos.contains(c.getId()),
                        lecturas.getOrDefault(c.getId(), 0L), secciones(c, nombres), editable(ctx, c)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ComunicadoResponse obtener(Long id, Long usuarioId) {
        Contexto ctx = contexto(usuarioId);
        Comunicado c = buscarComunicado(id);
        if (!puedeVer(ctx, c)) {
            throw new AccesoDenegadoException("No tienes acceso a este comunicado.");
        }
        return respuesta(c, ctx);
    }

    @Transactional
    public ComunicadoResponse crear(Long usuarioId, ComunicadoRequest req) {
        Contexto ctx = contexto(usuarioId);
        exigirPublicar(ctx);
        Comunicado c = new Comunicado();
        aplicarDatos(c, req, ctx);
        c.setAutor(ctx.usuario());
        c.setFechaCreacion(LocalDateTime.now());
        c = comunicadoRepository.save(c);
        marcarLeido(c.getId(), usuarioId);
        return respuesta(c, ctx);
    }

    @Transactional
    public ComunicadoResponse actualizar(Long id, Long usuarioId, ComunicadoRequest req) {
        Contexto ctx = contexto(usuarioId);
        Comunicado c = buscarComunicado(id);
        exigirEditar(ctx, c);
        aplicarDatos(c, req, ctx);
        c.setFechaActualizacion(LocalDateTime.now());
        return respuesta(comunicadoRepository.save(c), ctx);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Contexto ctx = contexto(usuarioId);
        Comunicado c = buscarComunicado(id);
        exigirEditar(ctx, c);
        lecturaRepository.eliminarPorComunicado(c.getId());
        comunicadoRepository.delete(c);
    }

    @Transactional
    public void marcarLeido(Long comunicadoId, Long usuarioId) {
        if (!comunicadoRepository.existsById(comunicadoId)) {
            throw new RecursoNoEncontradoException("Comunicado no encontrado.");
        }
        if (!lecturaRepository.existsByComunicadoIdAndUsuarioId(comunicadoId, usuarioId)) {
            lecturaRepository.save(ComunicadoLectura.builder()
                    .comunicadoId(comunicadoId)
                    .usuarioId(usuarioId)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public long contarNoLeidos(Long usuarioId) {
        return listar(usuarioId).stream().filter(c -> !c.leido()).count();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> seccionesParaPublicar(Long usuarioId) {
        Contexto ctx = contexto(usuarioId);
        Collection<Long> ids;
        if (ctx.gestiona()) ids = seccionRepository.findAll().stream().map(Seccion::getId).toList();
        else if (AccesoService.esDocente(ctx.usuario())) ids = ctx.secciones();
        else return List.of();
        Map<Long, Seccion> todas = new HashMap<>();
        seccionRepository.findAllById(ids).forEach(s -> todas.put(s.getId(), s));
        return todas.values().stream()
                .filter(s -> "Activa".equals(s.getEstado()))
                .sorted(Comparator.comparingInt((Seccion s) -> Grados.orden(s.getGrado())).thenComparing(Seccion::getLetra))
                .map(s -> Map.<String, Object>of("id", s.getId(), "nombre", AccesoService.salon(s)))
                .toList();
    }

    private Contexto contexto(Long usuarioId) {
        Usuario u = buscarUsuario(usuarioId);
        boolean gestiona = permisoService.tiene(u, Permiso.COMUNICADOS);
        Set<Long> secciones = new HashSet<>();
        if (AccesoService.esDocente(u)) {
            secciones.addAll(accesoService.seccionesDelDocente(u.getId()));
        } else if (AccesoService.esAlumno(u)) {
            estudianteRepository.findByUsuarioId(u.getId())
                    .flatMap(accesoService::seccionDe)
                    .ifPresent(s -> secciones.add(s.getId()));
        } else if (AccesoService.esPadre(u)) {
            padreRepository.findByUsuarioId(u.getId()).ifPresent(p -> accesoService.hijos(p)
                    .forEach(h -> accesoService.seccionDe(h).ifPresent(s -> secciones.add(s.getId()))));
        }
        return new Contexto(u, gestiona, secciones);
    }

    private boolean puedeVer(Contexto ctx, Comunicado c) {
        Usuario u = ctx.usuario();
        if (RolUtil.esAdministrativo(u.getRol())) return true;
        if (c.getAutor() != null && c.getAutor().getId().equals(u.getId())) return true;
        Set<String> grupos = c.getDestinatarios();
        String miGrupo = RolUtil.grupoComunicado(u.getRol());
        boolean grupo = grupos.contains(RolUtil.GRUPO_TODOS) || (miGrupo != null && grupos.contains(miGrupo));
        if (!grupo) return false;
        if (c.getSecciones().isEmpty()) return true;
        return c.getSecciones().stream().anyMatch(ctx.secciones()::contains);
    }

    private boolean editable(Contexto ctx, Comunicado c) {
        if (ctx.gestiona()) return true;
        return AccesoService.esDocente(ctx.usuario()) && c.getAutor() != null
                && c.getAutor().getId().equals(ctx.usuario().getId());
    }

    private void exigirPublicar(Contexto ctx) {
        if (ctx.gestiona() || AccesoService.esDocente(ctx.usuario())) return;
        throw new AccesoDenegadoException("No tienes permiso para gestionar comunicados.");
    }

    private void exigirEditar(Contexto ctx, Comunicado c) {
        if (!editable(ctx, c)) throw new AccesoDenegadoException("Solo puedes modificar tus propios comunicados.");
    }

    private void aplicarDatos(Comunicado c, ComunicadoRequest req, Contexto ctx) {
        if (req == null) throw new DatosInvalidosException("Faltan los datos del comunicado.");
        String titulo = req.titulo() == null ? "" : req.titulo().trim();
        if (titulo.isEmpty()) {
            throw new DatosInvalidosException("El título es obligatorio.");
        }
        if (titulo.length() < 5) {
            throw new DatosInvalidosException("El título debe tener al menos 5 caracteres.");
        }
        if (titulo.length() > 200) {
            throw new DatosInvalidosException("El título no puede superar los 200 caracteres.");
        }
        String contenido = req.contenido() == null ? "" : req.contenido().trim();
        String plano = contenido.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
        if (plano.isBlank()) {
            throw new DatosInvalidosException("El mensaje del comunicado es obligatorio.");
        }
        if (plano.length() < 10) {
            throw new DatosInvalidosException("El mensaje es muy corto (mínimo 10 caracteres).");
        }
        if (contenido.length() > 50_000) {
            throw new DatosInvalidosException("El mensaje es demasiado largo.");
        }
        if (req.imagen() != null && !req.imagen().isBlank()) {
            if (!req.imagen().matches("(?s)^data:image/(png|jpeg|jpg|webp|gif);base64,.+")) {
                throw new DatosInvalidosException("La imagen debe ser JPG, PNG, WEBP o GIF.");
            }
            if (req.imagen().length() > IMAGEN_MAX) {
                throw new DatosInvalidosException("La imagen es muy pesada (máximo 2 MB).");
            }
        }

        Set<String> grupos = new LinkedHashSet<>();
        if (req.destinatarios() != null) {
            for (String g : req.destinatarios()) {
                String grupo = g == null ? "" : g.trim().toUpperCase();
                if (!RolUtil.esGrupoValido(grupo)) {
                    throw new DatosInvalidosException("Destinatario no válido: " + g);
                }
                grupos.add(grupo);
            }
        }
        if (grupos.isEmpty()) {
            throw new DatosInvalidosException("Selecciona al menos un grupo de destinatarios.");
        }
        if (grupos.contains(RolUtil.GRUPO_TODOS)) {
            grupos = new LinkedHashSet<>(List.of(RolUtil.GRUPO_TODOS));
        }

        Set<Long> secciones = new LinkedHashSet<>();
        if (req.secciones() != null) req.secciones().stream().filter(Objects::nonNull).forEach(secciones::add);
        boolean anuncio = Boolean.TRUE.equals(req.anuncio());

        if (!ctx.gestiona()) {
            if (!GRUPOS_DOCENTE.containsAll(grupos)) {
                throw new DatosInvalidosException("Como docente puedes enviar comunicados a alumnos y/o padres de tus secciones.");
            }
            if (secciones.isEmpty()) throw new DatosInvalidosException("Elige al menos una de tus secciones.");
            if (!ctx.secciones().containsAll(secciones)) {
                throw new AccesoDenegadoException("Solo puedes enviar comunicados a las secciones donde enseñas.");
            }
            anuncio = false;
        } else if (!secciones.isEmpty()) {
            long existentes = seccionRepository.findAllById(secciones).size();
            if (existentes != secciones.size()) throw new DatosInvalidosException("Una de las secciones elegidas no existe.");
        }

        c.setTitulo(titulo);
        c.setContenido(contenido);
        c.setImagen(req.imagen() == null || req.imagen().isBlank() ? null : req.imagen());
        c.setAnuncio(anuncio);
        c.getDestinatarios().clear();
        c.getDestinatarios().addAll(grupos);
        c.getSecciones().clear();
        c.getSecciones().addAll(secciones);
    }

    private ComunicadoResponse respuesta(Comunicado c, Contexto ctx) {
        boolean leido = lecturaRepository.existsByComunicadoIdAndUsuarioId(c.getId(), ctx.usuario().getId());
        return ComunicadoResponse.of(c, leido, conteoLecturas().getOrDefault(c.getId(), 0L),
                secciones(c, nombresSecciones()), editable(ctx, c));
    }

    private Map<Long, String> nombresSecciones() {
        Map<Long, String> m = new HashMap<>();
        seccionRepository.findAll().forEach(s -> m.put(s.getId(), AccesoService.salon(s)));
        return m;
    }

    private static List<String> secciones(Comunicado c, Map<Long, String> nombres) {
        return c.getSecciones().stream().map(id -> nombres.getOrDefault(id, "Sección eliminada")).toList();
    }

    private Map<Long, Long> conteoLecturas() {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] fila : lecturaRepository.contarPorComunicado()) {
            mapa.put((Long) fila[0], (Long) fila[1]);
        }
        return mapa;
    }

    private Usuario buscarUsuario(Long id) {
        if (id == null) throw new DatosInvalidosException("Falta el usuario.");
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }

    private Comunicado buscarComunicado(Long id) {
        return comunicadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comunicado no encontrado."));
    }
}
