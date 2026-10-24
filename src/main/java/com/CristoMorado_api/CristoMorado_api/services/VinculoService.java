package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.VinculoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.VinculoResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import com.CristoMorado_api.CristoMorado_api.entity.EstudianteFamiliar;
import com.CristoMorado_api.CristoMorado_api.entity.PadreFamilia;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteFamiliarRepository;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PadreFamiliaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Único lugar donde se vinculan padres/apoderados con sus hijos
 * (módulo Académico > Vínculo Padre-Hijo). Escribir requiere el permiso ACADEMICO.
 */
@Service
public class VinculoService {

    /** Parentescos aceptados (columna de 20 caracteres). */
    public static final List<String> PARENTESCOS = List.of("Padre", "Madre", "Apoderado", "Tutor", "Otro familiar");

    private final EstudianteFamiliarRepository vinculoRepository;
    private final EstudianteRepository estudianteRepository;
    private final PadreFamiliaRepository padreRepository;

    public VinculoService(EstudianteFamiliarRepository vinculoRepository,
                          EstudianteRepository estudianteRepository,
                          PadreFamiliaRepository padreRepository) {
        this.vinculoRepository = vinculoRepository;
        this.estudianteRepository = estudianteRepository;
        this.padreRepository = padreRepository;
    }

    @Transactional(readOnly = true)
    public List<VinculoResponse> listar() {
        return vinculoRepository.findAll().stream()
                .map(VinculoResponse::fromEntity)
                .sorted(Comparator.comparing(VinculoResponse::estudianteNombre, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(VinculoResponse::padreNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public VinculoResponse crear(VinculoRequest req) {
        if (req == null || req.padreId() == null) throw new DatosInvalidosException("Selecciona al padre o apoderado.");
        if (req.estudianteId() == null) throw new DatosInvalidosException("Selecciona al alumno.");
        String parentesco = parentesco(req.parentesco());
        PadreFamilia padre = padreRepository.findById(req.padreId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Padre de familia no encontrado."));
        Estudiante alumno = estudianteRepository.findById(req.estudianteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado."));
        if (vinculoRepository.existsByEstudianteIdAndPadreFamiliaId(alumno.getId(), padre.getId())) {
            throw new RecursoDuplicadoException(padre.getNombreCompleto() + " ya está vinculado con " + alumno.getNombreCompleto() + ".");
        }
        validarUnico(alumno, parentesco, null);
        EstudianteFamiliar v = vinculoRepository.save(EstudianteFamiliar.builder()
                .estudiante(alumno).padreFamilia(padre).parentesco(parentesco).build());
        return VinculoResponse.fromEntity(v);
    }

    /** Solo cambia el parentesco; para cambiar de padre o de alumno se desvincula y se vuelve a vincular. */
    @Transactional
    public VinculoResponse actualizar(Long id, VinculoRequest req) {
        EstudianteFamiliar v = buscarOFallar(id);
        String parentesco = parentesco(req == null ? null : req.parentesco());
        validarUnico(v.getEstudiante(), parentesco, v.getId());
        v.setParentesco(parentesco);
        return VinculoResponse.fromEntity(vinculoRepository.save(v));
    }

    @Transactional
    public void eliminar(Long id) {
        vinculoRepository.delete(buscarOFallar(id));
    }

    /** Un alumno tiene como máximo un "Padre" y una "Madre". */
    private void validarUnico(Estudiante alumno, String parentesco, Long excluirId) {
        if (!parentesco.equals("Padre") && !parentesco.equals("Madre")) return;
        boolean existe = excluirId == null
                ? vinculoRepository.existsByEstudianteIdAndParentescoIgnoreCase(alumno.getId(), parentesco)
                : vinculoRepository.existsByEstudianteIdAndParentescoIgnoreCaseAndIdNot(alumno.getId(), parentesco, excluirId);
        if (existe) {
            throw new RecursoDuplicadoException(alumno.getNombreCompleto() + " ya tiene registrado(a) a su " + parentesco.toLowerCase() + ".");
        }
    }

    private static String parentesco(String valor) {
        if (valor == null || valor.isBlank()) throw new DatosInvalidosException("Selecciona el parentesco.");
        return PARENTESCOS.stream().filter(p -> p.equalsIgnoreCase(valor.trim())).findFirst()
                .orElseThrow(() -> new DatosInvalidosException("Parentesco no válido. Usa: " + String.join(", ", PARENTESCOS) + "."));
    }

    private EstudianteFamiliar buscarOFallar(Long id) {
        return vinculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vínculo no encontrado."));
    }
}
