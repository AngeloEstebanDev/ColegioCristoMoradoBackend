package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.DocenteRequest;
import com.CristoMorado_api.CristoMorado_api.dto.DocenteResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Docente;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.DocenteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.util.TipoDocumento;
import com.CristoMorado_api.CristoMorado_api.util.Campos;
import com.CristoMorado_api.CristoMorado_api.util.UsuarioFormato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocenteService {

    /** Rol con el que se crean las cuentas de docente en `usuarios`. */
    public static final String ROL_DOCENTE = "DOCENTE";

    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaService cuentaService;

    public DocenteService(DocenteRepository docenteRepository, UsuarioRepository usuarioRepository,
                          CuentaService cuentaService) {
        this.docenteRepository = docenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuentaService = cuentaService;
    }

    @Transactional(readOnly = true)
    public List<DocenteResponse> listar() {
        return docenteRepository.findAll().stream()
                .map(this::respuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocenteResponse obtener(Long id) {
        return respuesta(buscarOFallar(id));
    }

    @Transactional
    public DocenteResponse crear(DocenteRequest req) {
        String dni = TipoDocumento.validarNumero(req.tipoDocumento(), req.dni());
        if (docenteRepository.existsByDni(dni)) {
            throw new RecursoDuplicadoException("Ya existe un docente con ese número de documento.");
        }
        Docente d = mapearDatos(new Docente(), req);
        // Todo docente nuevo nace con su cuenta: usuario DOC + DNI.
        Usuario cuenta = cuentaService.crear(UsuarioFormato.DOCENTE, d.getDni(), d.getNombreCompleto(),
                ROL_DOCENTE, req.password(), activo(d));
        d.setUsuarioId(cuenta.getId());
        return respuesta(docenteRepository.save(d));
    }

    @Transactional
    public DocenteResponse actualizar(Long id, DocenteRequest req) {
        Docente d = buscarOFallar(id);
        String dni = TipoDocumento.validarNumero(req.tipoDocumento(), req.dni());
        if (!d.getDni().equals(dni) && docenteRepository.existsByDni(dni)) {
            throw new RecursoDuplicadoException("Ya existe un docente con ese número de documento.");
        }
        mapearDatos(d, req);

        Usuario cuenta = d.getUsuarioId() == null ? null : usuarioRepository.findById(d.getUsuarioId()).orElse(null);
        if (cuenta == null) {
            // Docente antiguo sin cuenta: se le crea ahora.
            cuenta = cuentaService.crear(UsuarioFormato.DOCENTE, d.getDni(), d.getNombreCompleto(),
                    ROL_DOCENTE, req.password(), activo(d));
            d.setUsuarioId(cuenta.getId());
        } else {
            cuentaService.sincronizar(cuenta, UsuarioFormato.DOCENTE, d.getDni(), d.getNombreCompleto(),
                    activo(d), req.password());
        }
        return respuesta(docenteRepository.save(d));
    }

    /** Botón "Desactivar/Activar cuenta" de la tabla de Docentes. */
    @Transactional
    public DocenteResponse cambiarEstado(Long id, boolean activo) {
        Docente d = buscarOFallar(id);
        d.setEstado(activo ? "Activo" : "Inactivo");
        Usuario cuenta = d.getUsuarioId() == null ? null : usuarioRepository.findById(d.getUsuarioId()).orElse(null);
        if (cuenta == null) {
            cuenta = cuentaService.crear(UsuarioFormato.DOCENTE, d.getDni(), d.getNombreCompleto(),
                    ROL_DOCENTE, null, activo);
            d.setUsuarioId(cuenta.getId());
        } else {
            cuentaService.cambiarEstado(cuenta, activo);
        }
        return respuesta(docenteRepository.save(d));
    }

    @Transactional
    public void eliminar(Long id) {
        Docente d = buscarOFallar(id);
        Usuario cuenta = d.getUsuarioId() == null ? null : usuarioRepository.findById(d.getUsuarioId()).orElse(null);
        docenteRepository.delete(d);
        docenteRepository.flush();
        // Al borrar al docente también se borra su cuenta de acceso.
        cuentaService.eliminar(cuenta);
    }

    long contarTotal() {
        return docenteRepository.count();
    }

    private DocenteResponse respuesta(Docente d) {
        Usuario cuenta = d.getUsuarioId() == null ? null : usuarioRepository.findById(d.getUsuarioId()).orElse(null);
        return DocenteResponse.fromEntity(d, cuenta);
    }

    private static boolean activo(Docente d) {
        return !"Inactivo".equalsIgnoreCase(d.getEstado());
    }

    private Docente mapearDatos(Docente d, DocenteRequest req) {
        d.setNombreCompleto(UsuarioFormato.nombreCompleto(req.nombreCompleto()));
        d.setTipoDocumento(TipoDocumento.normalizar(req.tipoDocumento()));
        d.setDni(TipoDocumento.validarNumero(req.tipoDocumento(), req.dni()));
        d.setFotoUrl(req.fotoUrl());
        d.setFechaNacimiento(Campos.fechaNacimiento(req.fechaNacimiento(), 18, 100));
        d.setGenero(Campos.opcion(req.genero(), java.util.List.of("Masculino", "Femenino"), "el género"));
        d.setTelefono(Campos.telefono(req.telefono(), false));
        d.setCorreo(UsuarioFormato.correoOpcional(req.correo()));          // correo de contacto (no es el de inicio de sesión)
        d.setDireccion(Campos.direccion(req.direccion()));
        d.setEspecialidad(Campos.texto(Campos.limpio(req.especialidad()), "la especialidad", 3, 100, false));
        d.setTituloProfesional(Campos.texto(Campos.limpio(req.tituloProfesional()), "el título profesional", 3, 150, false));
        d.setFechaIngreso(Campos.noFutura(req.fechaIngreso(), "la fecha de ingreso"));
        if (d.getFechaIngreso() != null && d.getFechaNacimiento() != null && d.getFechaIngreso().isBefore(d.getFechaNacimiento().plusYears(18))) {
            throw new com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException("La fecha de ingreso debe ser posterior a los 18 años del docente.");
        }
        if (req.estado() != null) d.setEstado(Campos.opcion(req.estado(), java.util.List.of("Activo", "De licencia", "Inactivo"), "el estado"));
        Campos.password(req.password());
        return d;
    }

    private Docente buscarOFallar(Long id) {
        return docenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado."));
    }
}
