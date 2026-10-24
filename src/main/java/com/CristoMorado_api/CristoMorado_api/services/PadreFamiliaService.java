package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.PadreFamiliaRequest;
import com.CristoMorado_api.CristoMorado_api.dto.PadreFamiliaResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import com.CristoMorado_api.CristoMorado_api.entity.EstudianteFamiliar;
import com.CristoMorado_api.CristoMorado_api.entity.PadreFamilia;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteFamiliarRepository;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PadreFamiliaRepository;
import com.CristoMorado_api.CristoMorado_api.util.TipoDocumento;
import com.CristoMorado_api.CristoMorado_api.util.Campos;
import com.CristoMorado_api.CristoMorado_api.util.UsuarioFormato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PadreFamiliaService {

    private final PadreFamiliaRepository padreFamiliaRepository;
    private final EstudianteRepository estudianteRepository;
    private final EstudianteFamiliarRepository vinculoRepository;
    private final CuentaService cuentaService;

    /** Rol con el que se crean las cuentas de padre/apoderado en `usuarios`. */
    public static final String ROL_PADRE = "PADRE";

    public PadreFamiliaService(PadreFamiliaRepository padreFamiliaRepository,
                                EstudianteRepository estudianteRepository,
                                EstudianteFamiliarRepository vinculoRepository,
                                CuentaService cuentaService) {
        this.padreFamiliaRepository = padreFamiliaRepository;
        this.estudianteRepository = estudianteRepository;
        this.vinculoRepository = vinculoRepository;
        this.cuentaService = cuentaService;
    }

    @Transactional(readOnly = true)
    public List<PadreFamiliaResponse> listar() {
        return padreFamiliaRepository.findAll().stream()
                .map(PadreFamiliaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PadreFamiliaResponse obtener(Long id) {
        return PadreFamiliaResponse.fromEntity(buscarOFallar(id));
    }

    @Transactional
    public PadreFamiliaResponse crear(PadreFamiliaRequest req) {
        String documento = TipoDocumento.validarNumero(req.tipoDocumento(), req.dni());
        if (padreFamiliaRepository.existsByDni(documento)) {
            throw new RecursoDuplicadoException("Ya existe un padre de familia con ese número de documento.");
        }
        PadreFamilia p = mapearDatos(new PadreFamilia(), req);
        // Todo apoderado nuevo nace con su cuenta: usuario APO + DNI.
        p.setUsuario(cuentaService.crear(UsuarioFormato.APODERADO, p.getDni(), p.getNombreCompleto(),
                ROL_PADRE, req.password(), activo(p)));
        p = padreFamiliaRepository.save(p);
        // Los hijos se vinculan solo en "Vínculo Padre-Hijo" (/api/vinculos).
        return PadreFamiliaResponse.fromEntity(padreFamiliaRepository.findById(p.getId()).orElseThrow());
    }

    @Transactional
    public PadreFamiliaResponse actualizar(Long id, PadreFamiliaRequest req) {
        PadreFamilia p = buscarOFallar(id);
        String dni = TipoDocumento.validarNumero(req.tipoDocumento(), req.dni());
        if (!p.getDni().equals(dni) && padreFamiliaRepository.existsByDni(dni)) {
            throw new RecursoDuplicadoException("Ya existe un padre de familia con ese número de documento.");
        }
        mapearDatos(p, req);
        if (p.getUsuario() == null) {
            // Apoderado antiguo sin cuenta: se le crea ahora.
            p.setUsuario(cuentaService.crear(UsuarioFormato.APODERADO, p.getDni(), p.getNombreCompleto(),
                    ROL_PADRE, req.password(), activo(p)));
        } else {
            cuentaService.sincronizar(p.getUsuario(), UsuarioFormato.APODERADO, p.getDni(),
                    p.getNombreCompleto(), activo(p), req.password());
        }
        padreFamiliaRepository.save(p);
        return PadreFamiliaResponse.fromEntity(padreFamiliaRepository.findById(id).orElseThrow());
    }

    /** Botón "Desactivar/Activar cuenta" de la tabla de Padres. No toca los hijos vinculados. */
    @Transactional
    public PadreFamiliaResponse cambiarEstado(Long id, boolean activo) {
        PadreFamilia p = buscarOFallar(id);
        p.setEstado(activo ? "Activo" : "Inactivo");
        if (p.getUsuario() == null) {
            p.setUsuario(cuentaService.crear(UsuarioFormato.APODERADO, p.getDni(), p.getNombreCompleto(),
                    ROL_PADRE, null, activo));
        } else {
            cuentaService.cambiarEstado(p.getUsuario(), activo);
        }
        padreFamiliaRepository.save(p);
        return PadreFamiliaResponse.fromEntity(p);
    }

    @Transactional
    public void eliminar(Long id) {
        PadreFamilia p = buscarOFallar(id);
        Usuario cuenta = p.getUsuario();
        padreFamiliaRepository.delete(p);
        padreFamiliaRepository.flush();
        // Al borrar al apoderado también se borra su cuenta de acceso.
        cuentaService.eliminar(cuenta);
    }

    long contarTotal() {
        return padreFamiliaRepository.count();
    }

    List<PadreFamilia> ultimosRegistrados() {
        return padreFamiliaRepository.findTop5ByOrderByFechaRegistroDesc();
    }

    private PadreFamilia mapearDatos(PadreFamilia p, PadreFamiliaRequest req) {
        p.setNombreCompleto(UsuarioFormato.nombreCompleto(req.nombreCompleto()));
        p.setTipoDocumento(TipoDocumento.normalizar(req.tipoDocumento()));
        p.setDni(TipoDocumento.validarNumero(req.tipoDocumento(), req.dni()));
        p.setFechaNacimiento(Campos.fechaNacimiento(req.fechaNacimiento(), 18, 100));
        p.setTelefono(Campos.telefono(req.telefono(), true));
        p.setCorreo(UsuarioFormato.correoOpcional(req.correo()));
        p.setDireccion(Campos.direccion(req.direccion()));
        if (req.estado() != null) p.setEstado(Campos.opcion(req.estado(), java.util.List.of("Activo", "En proceso", "Inactivo"), "el estado"));
        Campos.password(req.password());
        return p;
    }

    private static boolean activo(PadreFamilia p) {
        return !"Inactivo".equalsIgnoreCase(p.getEstado());
    }

    private PadreFamilia buscarOFallar(Long id) {
        return padreFamiliaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Padre de familia no encontrado."));
    }
}
