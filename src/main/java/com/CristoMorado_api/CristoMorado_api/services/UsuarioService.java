package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.UsuarioRequest;
import com.CristoMorado_api.CristoMorado_api.dto.UsuarioResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.DocenteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PadreFamiliaRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PersonalRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.util.RolUtil;
import com.CristoMorado_api.CristoMorado_api.util.UsuarioFormato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Vista "Administrativo". Aquí se crean las cuentas de Administración
 * (usuario libre) y de Directiva (DIR + DNI). Las de alumnos, docentes y
 * apoderados se crean solas desde su propio módulo (ALU/DOC/APO + DNI).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final PadreFamiliaRepository padreFamiliaRepository;
    private final LimpiezaUsuarioService limpiezaUsuarioService;
    private final CuentaService cuentaService;
    private final PersonalRepository personalRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          EstudianteRepository estudianteRepository,
                          DocenteRepository docenteRepository,
                          PadreFamiliaRepository padreFamiliaRepository,
                          LimpiezaUsuarioService limpiezaUsuarioService,
                          CuentaService cuentaService,
                          PersonalRepository personalRepository) {
        this.personalRepository = personalRepository;
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.docenteRepository = docenteRepository;
        this.padreFamiliaRepository = padreFamiliaRepository;
        this.limpiezaUsuarioService = limpiezaUsuarioService;
        this.cuentaService = cuentaService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(u -> UsuarioResponse.fromEntity(u, vinculadoA(u.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        Usuario u = buscarOFallar(id);
        return UsuarioResponse.fromEntity(u, vinculadoA(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req, Long actorId) {
        String rol = rolObligatorio(req.rol());
        soloAdminTocaAdmins(rol, actorId);
        rechazarRolesDeModulo(rol);
        String usuario = nombreDeUsuario(rol, req);
        if (usuarioRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta con el usuario " + usuario + ".");
        }
        if (req.password() == null || req.password().isBlank()) {
            throw new DatosInvalidosException("La contraseña es obligatoria.");
        }
        CuentaService.validarPassword(req.password());

        Usuario u = Usuario.builder()
                .nombre(nombreObligatorio(req.nombre()))
                .usuario(usuario)
                .correo(null)
                .correoVerificado(false)
                .password(req.password())
                .rol(rol)
                .estado(req.estado() == null || req.estado())
                .build();
        return UsuarioResponse.fromEntity(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req, Long actorId) {
        noEsSuPropiaCuenta(id, actorId, "editar");
        Usuario u = buscarOFallar(id);
        soloAdminTocaAdmins(u.getRol(), actorId);
        if (req.rol() != null) soloAdminTocaAdmins(req.rol(), actorId);
        String vinculado = vinculadoA(id);

        if (vinculado != null) {
            // Cuenta de alumno/docente/apoderado: el usuario (PREFIJO + DNI) y el
            // rol dependen de su módulo; aquí solo se cambia estado y contraseña.
            if (req.rol() != null && !mismaCategoria(req.rol(), u.getRol())) {
                throw new DatosInvalidosException("No se puede cambiar el rol de una cuenta de " + vinculado.toLowerCase()
                        + ". Gestiona sus datos desde el módulo correspondiente.");
            }
        } else {
            String rol = rolObligatorio(req.rol());
            rechazarRolesDeModulo(rol);
            String usuario = nombreDeUsuario(rol, req);
            if (!usuario.equalsIgnoreCase(u.getUsuario()) && usuarioRepository.existsByUsuarioIgnoreCase(usuario)) {
                throw new RecursoDuplicadoException("Ya existe una cuenta con el usuario " + usuario + ".");
            }
            u.setUsuario(usuario);
            u.setRol(rol);
            u.setNombre(nombreObligatorio(req.nombre()));
        }

        if (req.estado() != null) u.setEstado(req.estado());
        // Si el admin escribe una contraseña nueva, se guarda en la BD.
        if (req.password() != null && !req.password().isBlank()) {
            CuentaService.validarPassword(req.password());
            u.setPassword(req.password());
        }
        return UsuarioResponse.fromEntity(usuarioRepository.save(u), vinculado);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean estado, Long actorId) {
        noEsSuPropiaCuenta(id, actorId, "desactivar");
        Usuario u = buscarOFallar(id);
        soloAdminTocaAdmins(u.getRol(), actorId);
        if (!estado) noDejarSinAdministradores(u);
        u.setEstado(estado);
        return UsuarioResponse.fromEntity(usuarioRepository.save(u), vinculadoA(id));
    }

    @Transactional
    public void eliminar(Long id, Long actorId) {
        noEsSuPropiaCuenta(id, actorId, "eliminar");
        Usuario u = buscarOFallar(id);
        soloAdminTocaAdmins(u.getRol(), actorId);
        noDejarSinAdministradores(u);
        String vinculado = vinculadoA(id);
        if (vinculado != null) {
            throw new DatosInvalidosException("Esta cuenta pertenece a un " + vinculado.toLowerCase()
                    + ". Para borrarla, elimina al " + vinculado.toLowerCase() + " desde su módulo.");
        }
        limpiezaUsuarioService.liberarReferencias(id);
        usuarioRepository.deleteById(id);
    }

    long contarActivos() {
        return usuarioRepository.countByEstadoTrue();
    }

    long contarPorRol(String rol) {
        return usuarioRepository.countByRolIgnoreCase(rol);
    }

    // ------------------------------------------------------------------

    /**
     * El administrador que está usando el sistema no puede editar,
     * desactivar ni eliminar su propia cuenta desde "Administrativo"
     * (así nunca se queda fuera por error).
     */
    private static void noEsSuPropiaCuenta(Long id, Long actorId, String accion) {
        if (actorId != null && actorId.equals(id)) {
            throw new AccesoDenegadoException("No puedes " + accion + " tu propia cuenta.");
        }
    }

    /** Debe quedar siempre al menos un administrador activo. */
    private void noDejarSinAdministradores(Usuario u) {
        if (!RolUtil.esAdministrativo(u.getRol()) || !Boolean.TRUE.equals(u.getEstado())) return;
        long otrosAdminsActivos = usuarioRepository.findAll().stream()
                .filter(x -> !x.getId().equals(u.getId()))
                .filter(x -> Boolean.TRUE.equals(x.getEstado()))
                .filter(x -> "admin".equalsIgnoreCase(x.getRol()) || "administrador".equalsIgnoreCase(x.getRol()))
                .count();
        boolean esAdmin = "admin".equalsIgnoreCase(u.getRol()) || "administrador".equalsIgnoreCase(u.getRol());
        if (esAdmin && otrosAdminsActivos == 0) {
            throw new DatosInvalidosException("Debe quedar al menos un administrador activo en el sistema.");
        }
    }

    /** "Alumno" / "Docente" / "Apoderado" si la cuenta pertenece a un registro de esos módulos. */
    private String vinculadoA(Long usuarioId) {
        if (estudianteRepository.findByUsuarioId(usuarioId).isPresent()) return "Alumno";
        if (docenteRepository.findByUsuarioId(usuarioId).isPresent()) return "Docente";
        if (padreFamiliaRepository.findByUsuarioId(usuarioId).isPresent()) return "Apoderado";
        if (personalRepository.findByUsuarioId(usuarioId).isPresent()) return "Directiva";
        return null;
    }

    private String nombreDeUsuario(String rol, UsuarioRequest req) {
        if (UsuarioFormato.DIRECTIVA.equals(UsuarioFormato.prefijoDeRol(rol))) {
            return cuentaService.usuarioPara(UsuarioFormato.DIRECTIVA, req.dni());
        }
        String usuario = req.usuario() == null ? "" : req.usuario().trim();
        if (usuario.length() < 3 || usuario.length() > 30 || !usuario.matches("[A-Za-z0-9._-]+")) {
            throw new DatosInvalidosException("El usuario debe tener de 3 a 30 caracteres: letras, números, punto, guion o guion bajo (sin espacios).");
        }
        return usuario;
    }

    private static void rechazarRolesDeModulo(String rol) {
        String prefijo = UsuarioFormato.prefijoDeRol(rol);
        if (UsuarioFormato.ALUMNO.equals(prefijo) || UsuarioFormato.DOCENTE.equals(prefijo)
                || UsuarioFormato.APODERADO.equals(prefijo)) {
            throw new DatosInvalidosException("Las cuentas de alumnos, docentes y apoderados se crean solas "
                    + "al registrarlos en su módulo (usuario ALU/DOC/APO + DNI).");
        }
        if (UsuarioFormato.DIRECTIVA.equals(prefijo)) {
            throw new DatosInvalidosException("El personal de Directiva se registra en \"Personal Staff\" "
                    + "(usuario DIR + N.º de documento).");
        }
    }

    /** Solo un administrador puede crear, editar, apagar o borrar cuentas de administrador. */
    private void soloAdminTocaAdmins(String rolObjetivo, Long actorId) {
        String r = rolObjetivo == null ? "" : rolObjetivo.trim().toLowerCase();
        if (!r.equals("admin") && !r.equals("administrador")) return;
        if (actorId == null) return; // sin actor (p.ej. pruebas internas): lo controla el interceptor
        Usuario actor = usuarioRepository.findById(actorId).orElse(null);
        if (actor == null || !PermisoService.esAdministrador(actor)) {
            throw new AccesoDenegadoException("Solo un administrador puede gestionar cuentas de administrador.");
        }
    }

    private static boolean mismaCategoria(String a, String b) {
        String pa = UsuarioFormato.prefijoDeRol(a), pb = UsuarioFormato.prefijoDeRol(b);
        return pa == null ? a.equalsIgnoreCase(b) : pa.equals(pb);
    }

    private static String rolObligatorio(String rol) {
        if (rol == null || rol.isBlank()) throw new DatosInvalidosException("Selecciona un rol.");
        return rol.trim();
    }

    private static String nombreObligatorio(String nombre) {
        if (nombre == null || nombre.trim().length() < 3) {
            throw new DatosInvalidosException("Escribe el nombre completo.");
        }
        return com.CristoMorado_api.CristoMorado_api.util.Campos.parteNombre(nombre, "el nombre", 100, true);
    }

    private Usuario buscarOFallar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }
}
