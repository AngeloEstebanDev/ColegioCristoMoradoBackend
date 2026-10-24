package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.*;
import com.CristoMorado_api.CristoMorado_api.entity.Docente;
import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import com.CristoMorado_api.CristoMorado_api.entity.PadreFamilia;
import com.CristoMorado_api.CristoMorado_api.entity.Personal;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import java.time.LocalDate;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.DocenteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PadreFamiliaRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PersonalRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.util.Campos;
import com.CristoMorado_api.CristoMorado_api.util.RolUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final PadreFamiliaRepository padreFamiliaRepository;
    private final VerificacionService verificacionService;
    private final PersonalRepository personalRepository;
    private final PermisoService permisoService;

    public PerfilService(UsuarioRepository usuarioRepository, DocenteRepository docenteRepository,
                         EstudianteRepository estudianteRepository, PadreFamiliaRepository padreFamiliaRepository,
                         VerificacionService verificacionService, PersonalRepository personalRepository,
                         PermisoService permisoService) {
        this.personalRepository = personalRepository;
        this.permisoService = permisoService;
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.estudianteRepository = estudianteRepository;
        this.padreFamiliaRepository = padreFamiliaRepository;
        this.verificacionService = verificacionService;
    }

    /** Perfil completo: cuenta + datos del alumno, docente o apoderado. */
    @Transactional(readOnly = true)
    public PerfilResponse obtener(Long usuarioId) {
        return construirRespuesta(buscarUsuario(usuarioId));
    }

    /** ~2 MB de imagen en base64. */
    private static final int FOTO_MAX = 2_800_000;

    /** Avatares predefinidos del colegio (archivos del frontend en /avatares). */
    private static final String AVATAR_REGEX = "^/avatares/[a-z0-9-]{1,40}\\.svg$";

    /**
     * Todos menos los alumnos pueden cambiar sus datos personales y su dirección.
     * La foto de perfil (foto propia o avatar del colegio) la pueden cambiar TODOS,
     * incluidos los alumnos.
     * No se cambian: número de documento (de él sale el usuario), rol, estado ni Gmail.
     */
    @Transactional
    public PerfilResponse actualizar(Long usuarioId, PerfilUpdateRequest req) {
        Usuario u = buscarUsuario(usuarioId);
        if (req == null) throw new DatosInvalidosException("Faltan los datos del perfil.");

        boolean cambiaFoto = Boolean.TRUE.equals(req.quitarFoto())
                || (req.foto() != null && !req.foto().isBlank());
        if (!puedeEditar(u) && traeDatosPersonales(req)) {
            throw new AccesoDenegadoException("Los alumnos solo pueden cambiar su foto de perfil. "
                    + "Si algún dato está mal, pide a la administración que lo corrija.");
        }

        // ---- Foto de perfil (en la cuenta, sirve para todos los roles)
        if (Boolean.TRUE.equals(req.quitarFoto())) {
            u.setFoto(null);
        } else if (req.foto() != null && !req.foto().isBlank()) {
            String foto = req.foto().trim();
            if (foto.matches(AVATAR_REGEX)) {
                u.setFoto(foto);
            } else {
                if (!foto.startsWith("data:image/") || foto.startsWith("data:image/svg"))
                    throw new DatosInvalidosException("La foto debe ser una imagen (JPG o PNG) o un avatar del colegio.");
                if (foto.length() > FOTO_MAX) throw new DatosInvalidosException("La foto es muy pesada (máximo 2 MB).");
                u.setFoto(foto);
            }
        }

        if (!puedeEditar(u)) {
            // Alumno: solo la foto.
            if (!cambiaFoto) throw new DatosInvalidosException("Elige una foto o un avatar.");
            usuarioRepository.save(u);
            return construirRespuesta(u);
        }

        String telefono = texto(req.telefono(), 20, "El teléfono");
        if (telefono != null && !telefono.isEmpty()) telefono = Campos.telefono(telefono, false);
        String direccion = texto(req.direccion(), 200, "La dirección");
        if (direccion != null && !direccion.isEmpty()) direccion = Campos.direccion(direccion);
        String correo = texto(req.correoContacto(), 150, "El correo de contacto");
        if (correo != null && !correo.isEmpty() && !correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new DatosInvalidosException("El correo de contacto no es válido.");
        }
        if (req.fechaNacimiento() != null && (req.fechaNacimiento().isAfter(LocalDate.now().minusYears(18))
                || req.fechaNacimiento().isBefore(LocalDate.now().minusYears(100)))) {
            throw new DatosInvalidosException("Revisa la fecha de nacimiento: la edad debe estar entre 18 y 100 años.");
        }

        Docente docente = docenteRepository.findByUsuarioId(usuarioId).orElse(null);
        PadreFamilia padre = padreFamiliaRepository.findByUsuarioId(usuarioId).orElse(null);
        Personal personal = personalRepository.findByUsuarioId(usuarioId).orElse(null);

        if (personal != null) {
            // Directiva: nombres y apellidos por separado; el nombre de la cuenta se arma con ellos.
            String nombres = texto(req.nombres(), 100, "Los nombres");
            String apPat = texto(req.apellidoPaterno(), 60, "El apellido paterno");
            String apMat = texto(req.apellidoMaterno(), 60, "El apellido materno");
            if (nombres != null) {
                if (nombres.isEmpty()) throw new DatosInvalidosException("Escribe tus nombres.");
                nombres = Campos.parteNombre(nombres, "tus nombres", 100, true);
                personal.setNombres(nombres);
            }
            if (apPat != null) {
                if (apPat.isEmpty()) throw new DatosInvalidosException("Escribe tu apellido paterno.");
                apPat = Campos.parteNombre(apPat, "el apellido paterno", 60, true);
                personal.setApellidoPaterno(apPat);
            }
            if (apMat != null) personal.setApellidoMaterno(Campos.parteNombre(apMat, "el apellido materno", 60, false));
            if (telefono != null) personal.setTelefono(vacioANull(telefono));
            if (direccion != null) personal.setDireccion(vacioANull(direccion));
            if (correo != null) personal.setCorreo(vacioANull(correo));
            if (req.fechaNacimiento() != null) personal.setFechaNacimiento(req.fechaNacimiento());
            personalRepository.save(personal);
            u.setNombre(recortar(personal.nombreCompleto(), 100));
        } else {
            String nombre = texto(req.nombre(), 150, "El nombre");
            if (nombre != null) {
                if (nombre.length() < 3) throw new DatosInvalidosException("Escribe tu nombre completo.");
                nombre = (docente != null || padre != null) ? Campos.nombreCompleto(nombre, "Escribe tu nombre completo.")
                        : Campos.parteNombre(nombre, "tu nombre", 100, true);
                u.setNombre(recortar(nombre, 100));
            }
            if (docente != null) {
                if (nombre != null) docente.setNombreCompleto(nombre);
                if (telefono != null) docente.setTelefono(vacioANull(telefono));
                if (direccion != null) docente.setDireccion(vacioANull(direccion));
                if (correo != null) docente.setCorreo(vacioANull(correo));
                if (req.fechaNacimiento() != null) docente.setFechaNacimiento(req.fechaNacimiento());
                String genero = texto(req.genero(), 20, "El género");
                if (genero != null) docente.setGenero(Campos.opcion(genero, java.util.List.of("Masculino", "Femenino"), "el género"));
                String esp = texto(req.especialidad(), 100, "La especialidad");
                if (esp != null) docente.setEspecialidad(vacioANull(esp));
                String titulo = texto(req.tituloProfesional(), 150, "El título profesional");
                if (titulo != null) docente.setTituloProfesional(vacioANull(titulo));
                docenteRepository.save(docente);
            } else if (padre != null) {
                if (nombre != null) padre.setNombreCompleto(nombre);
                if (telefono != null) {
                    if (telefono.isEmpty()) throw new DatosInvalidosException("El teléfono es obligatorio para los padres de familia.");
                    padre.setTelefono(telefono);
                }
                if (direccion != null) padre.setDireccion(vacioANull(direccion));
                if (correo != null) padre.setCorreo(vacioANull(correo));
                if (req.fechaNacimiento() != null) padre.setFechaNacimiento(req.fechaNacimiento());
                padreFamiliaRepository.save(padre);
            } else {
                // Administrador (sin registro de persona): teléfono y dirección en la cuenta.
                if (telefono != null) u.setTelefono(vacioANull(telefono));
                if (direccion != null) u.setDireccion(vacioANull(direccion));
            }
        }
        usuarioRepository.save(u);
        return construirRespuesta(u);
    }

    /** null = no se envió; "" = se borró; si no, el texto limpio (y con largo válido). */
    private static String texto(String valor, int max, String campo) {
        if (valor == null) return null;
        String t = valor.trim().replaceAll("\\s+", " ");
        if (t.length() > max) throw new DatosInvalidosException(campo + " admite hasta " + max + " caracteres.");
        return t;
    }

    private static String vacioANull(String v) {
        return v == null || v.isEmpty() ? null : v;
    }

    private static String recortar(String v, int max) {
        return v.length() > max ? v.substring(0, max) : v;
    }

    // ---- Gmail ----

    public CodigoEnviadoResponse enviarCodigoCorreo(Long usuarioId, String correo) {
        return verificacionService.enviarCodigoVinculacion(usuarioId, correo);
    }

    /**
     * Sin @Transactional propio: VerificacionService maneja la transacción
     * (así un código incorrecto sí cuenta como intento fallido). El controlador
     * vuelve a pedir el perfil completo después.
     */
    public void verificarCorreo(Long usuarioId, String codigo) {
        verificacionService.confirmarVinculacion(usuarioId, codigo);
    }

    // ---- Contraseña (con código al Gmail) ----

    public CodigoEnviadoResponse enviarCodigoPassword(Long usuarioId) {
        return verificacionService.enviarCodigoCambioPassword(usuarioId);
    }

    public void cambiarPassword(Long usuarioId, CambiarPasswordRequest req) {
        verificacionService.cambiarPassword(usuarioId, req.passwordActual(), req.passwordNueva(), req.codigo());
    }

    // ------------------------------------------------------------------

    /** ¿La solicitud trae algún dato distinto de la foto? */
    private static boolean traeDatosPersonales(PerfilUpdateRequest r) {
        return r.nombre() != null || r.nombres() != null || r.apellidoPaterno() != null
                || r.apellidoMaterno() != null || r.telefono() != null || r.direccion() != null
                || r.fechaNacimiento() != null || r.genero() != null || r.correoContacto() != null
                || r.especialidad() != null || r.tituloProfesional() != null;
    }

    static boolean puedeEditar(Usuario u) {
        return RolUtil.categoria(u.getRol()) != RolUtil.Categoria.ALUMNO;
    }

    private PerfilResponse construirRespuesta(Usuario u) {
        Docente docente = docenteRepository.findByUsuarioId(u.getId()).orElse(null);
        Estudiante estudiante = estudianteRepository.findByUsuarioId(u.getId()).orElse(null);
        PadreFamilia padre = padreFamiliaRepository.findByUsuarioId(u.getId()).orElse(null);
        var personal = personalRepository.findByUsuarioId(u.getId()).orElse(null);
        return new PerfilResponse(
                u.getId(), u.getNombre(), u.getUsuario(), u.getCorreo(),
                Boolean.TRUE.equals(u.getCorreoVerificado()), u.getRol(), u.getEstado(),
                u.getFechaRegistro(), puedeEditar(u), u.getFoto(),
                docente != null ? docente.getTelefono() : padre != null ? padre.getTelefono()
                        : personal != null ? personal.getTelefono() : estudiante != null ? estudiante.getTelefono() : u.getTelefono(),
                docente != null ? docente.getDireccion() : padre != null ? padre.getDireccion()
                        : personal != null ? personal.getDireccion() : estudiante != null ? estudiante.getDireccion() : u.getDireccion(),
                docente == null ? null : DocenteResponse.fromEntity(docente, u),
                estudiante == null ? null : EstudianteResponse.fromEntity(estudiante),
                padre == null ? null : PadreFamiliaResponse.fromEntity(padre),
                personal == null ? null : PersonalResponse.fromEntity(personal),
                permisoService.permisosDe(u)
        );
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }
}
