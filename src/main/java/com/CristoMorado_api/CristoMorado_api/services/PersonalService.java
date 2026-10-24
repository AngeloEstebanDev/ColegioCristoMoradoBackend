package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.PersonalRequest;
import com.CristoMorado_api.CristoMorado_api.dto.PersonalResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Personal;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.PersonalRepository;
import com.CristoMorado_api.CristoMorado_api.util.Permiso;
import com.CristoMorado_api.CristoMorado_api.util.TipoDocumento;
import com.CristoMorado_api.CristoMorado_api.util.Campos;
import com.CristoMorado_api.CristoMorado_api.util.UsuarioFormato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Gestión de Personal Staff" (Directiva). Cada registro tiene su cuenta
 * DIR + N.º de documento con rol DIRECTIVO.
 * - Al crearlo el ADMINISTRADOR: recibe TODOS los permisos (como el admin).
 * - Al crearlo alguien de la Directiva: recibe como máximo los permisos de quien lo crea.
 * - Cambian permisos: el ADMINISTRADOR y quien tenga el permiso "Gestionar permisos"
 *   (p.ej. el Director). Este último no puede tocar sus propios permisos ni dar
 *   permisos que él mismo no tiene.
 */
@Service
public class PersonalService {

    public static final String ROL_DIRECTIVO = "DIRECTIVO";

    private final PersonalRepository personalRepository;
    private final CuentaService cuentaService;
    private final PermisoService permisoService;

    public PersonalService(PersonalRepository personalRepository, CuentaService cuentaService,
                           PermisoService permisoService) {
        this.personalRepository = personalRepository;
        this.cuentaService = cuentaService;
        this.permisoService = permisoService;
    }

    @Transactional(readOnly = true)
    public List<PersonalResponse> listar() {
        return personalRepository.findAll().stream()
                .sorted(Comparator.comparing(Personal::getApellidoPaterno, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Personal::getNombres, String.CASE_INSENSITIVE_ORDER))
                .map(PersonalResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonalResponse obtener(Long id) {
        return PersonalResponse.fromEntity(buscar(id));
    }

    @Transactional
    public PersonalResponse crear(PersonalRequest req, Long actorId) {
        Usuario actor = permisoService.exigir(actorId, Permiso.USUARIOS);
        Personal p = mapear(new Personal(), req);
        if (personalRepository.existsByNumeroDocumento(p.getNumeroDocumento())) {
            throw new RecursoDuplicadoException("Ya existe personal registrado con ese número de documento.");
        }
        p.setUsuario(cuentaService.crear(UsuarioFormato.DIRECTIVA, p.getNumeroDocumento(), p.nombreCompleto(),
                ROL_DIRECTIVO, req.password(), true));
        // Mismos permisos que el administrador (o los de quien lo registra, si es de la Directiva).
        p.setPermisos(new LinkedHashSet<>(permisoService.permisosDe(actor)));
        return PersonalResponse.fromEntity(personalRepository.save(p));
    }

    @Transactional
    public PersonalResponse actualizar(Long id, PersonalRequest req, Long actorId) {
        permisoService.exigir(actorId, Permiso.USUARIOS);
        Personal p = buscar(id);
        noEsSuPropioRegistro(p, actorId, "editar tu propio registro aquí; usa \"Mi perfil\"");
        String numeroAnterior = p.getNumeroDocumento();
        mapear(p, req);
        if (!numeroAnterior.equals(p.getNumeroDocumento())
                && personalRepository.existsByNumeroDocumento(p.getNumeroDocumento())) {
            throw new RecursoDuplicadoException("Ya existe personal registrado con ese número de documento.");
        }
        cuentaService.sincronizar(p.getUsuario(), UsuarioFormato.DIRECTIVA, p.getNumeroDocumento(),
                p.nombreCompleto(), activo(p), req.password());
        return PersonalResponse.fromEntity(personalRepository.save(p));
    }

    @Transactional
    public PersonalResponse cambiarEstado(Long id, boolean activo, Long actorId) {
        permisoService.exigir(actorId, Permiso.USUARIOS);
        Personal p = buscar(id);
        noEsSuPropioRegistro(p, actorId, "desactivar tu propia cuenta");
        p.setEstado(activo ? "Activo" : "Inactivo");
        if (p.getUsuario() != null) p.getUsuario().setEstado(activo);
        return PersonalResponse.fromEntity(personalRepository.save(p));
    }

    /**
     * El administrador (o quien tenga "Gestionar permisos", como el Director)
     * decide qué puede hacer cada miembro de la Directiva.
     */
    @Transactional
    public PersonalResponse actualizarPermisos(Long id, List<String> permisos, Long actorId) {
        Usuario actor = permisoService.exigir(actorId, Permiso.PERMISOS);
        boolean esAdmin = PermisoService.esAdministrador(actor);
        Personal p = buscar(id);
        if (!esAdmin) {
            noEsSuPropioRegistro(p, actorId, "cambiar tus propios permisos");
        }
        List<String> propios = permisoService.permisosDe(actor);
        Set<String> nuevos = new LinkedHashSet<>();
        if (permisos != null) {
            for (String permiso : permisos) {
                String x = permiso == null ? "" : permiso.trim().toUpperCase();
                if (!Permiso.esValido(x)) throw new DatosInvalidosException("Permiso no válido: " + permiso);
                nuevos.add(x);
            }
        }
        // Quien no es administrador no puede dar ni quitar permisos que él mismo no tiene.
        if (!esAdmin) {
            for (String x : Permiso.todos()) {
                boolean antes = p.getPermisos().contains(x), despues = nuevos.contains(x);
                if (antes != despues && !propios.contains(x)) {
                    throw new AccesoDenegadoException("No puedes dar ni quitar un permiso que tú no tienes.");
                }
            }
        }
        p.getPermisos().clear();
        p.getPermisos().addAll(nuevos);
        return PersonalResponse.fromEntity(personalRepository.save(p));
    }

    // ------------------------------------------------------------------

    private static void noEsSuPropioRegistro(Personal p, Long actorId, String accion) {
        if (p.getUsuario() != null && p.getUsuario().getId().equals(actorId)) {
            throw new AccesoDenegadoException("No puedes " + accion + ".");
        }
    }

    private static boolean activo(Personal p) {
        return !"Inactivo".equalsIgnoreCase(p.getEstado());
    }

    private Personal mapear(Personal p, PersonalRequest req) {
        String tipo = TipoDocumento.normalizar(req.tipoDocumento());
        String numero = TipoDocumento.validarNumero(tipo, req.numeroDocumento());
        String nombres = obligatorio(req.nombres(), "Escribe los nombres.");
        String paterno = obligatorio(req.apellidoPaterno(), "Escribe el apellido paterno.");
        nombres = Campos.parteNombre(nombres, "los nombres", 100, true);
        paterno = Campos.parteNombre(paterno, "el apellido paterno", 60, true);
        String materno = Campos.parteNombre(req.apellidoMaterno(), "el apellido materno", 60, false);
        String correo = UsuarioFormato.correoOpcional(req.correo());
        String telefono = Campos.telefono(req.telefono(), false);
        Campos.password(req.password());
        p.setTipoDocumento(tipo);
        p.setNumeroDocumento(numero);
        p.setNombres(nombres);
        p.setApellidoPaterno(paterno);
        p.setApellidoMaterno(materno);
        p.setCorreo(correo);
        p.setTelefono(telefono);
        p.setFechaNacimiento(Campos.fechaNacimiento(req.fechaNacimiento(), 18, 100));
        p.setCargo(Campos.texto(Campos.limpio(req.cargo()), "el cargo", 3, 100, false));
        return p;
    }

    private static String obligatorio(String v, String mensaje) {
        if (v == null || v.trim().isEmpty()) throw new DatosInvalidosException(mensaje);
        return v.trim();
    }

    private static String vacioANull(String v) {
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }

    private Personal buscar(Long id) {
        return personalRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Registro de personal no encontrado."));
    }
}
