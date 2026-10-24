package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.CursoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.CursoResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Curso;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.CursoRepository;
import com.CristoMorado_api.CristoMorado_api.repository.SeccionCursoRepository;
import com.CristoMorado_api.CristoMorado_api.util.CategoriaCurso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Académico > Catálogo de Cursos. De aquí salen los cursos de "Agregar curso"
 * en cada sección y sus colores en el horario.
 */
@Service
public class CursoService {

    private final CursoRepository cursoRepository;
    private final SeccionCursoRepository seccionCursoRepository;

    public CursoService(CursoRepository cursoRepository, SeccionCursoRepository seccionCursoRepository) {
        this.cursoRepository = cursoRepository;
        this.seccionCursoRepository = seccionCursoRepository;
    }

    /** Cursos activos, ordenados por categoría y en el orden en que se registraron. */
    @Transactional(readOnly = true)
    public List<CursoResponse> listar() {
        return cursoRepository.findByEstadoOrderByNombreAsc("Activo").stream()
                .sorted(Comparator.comparingInt((Curso c) -> CategoriaCurso.orden(c.getCategoria()))
                        .thenComparing(Curso::getId))
                .map(c -> CursoResponse.fromEntity(c, seccionCursoRepository.countByCursoId(c.getId())))
                .toList();
    }

    @Transactional
    public CursoResponse crear(CursoRequest req) {
        String nombre = nombre(req);
        CategoriaCurso.Info cat = categoria(req);
        Curso c = cursoRepository.findByNombreIgnoreCase(nombre).orElse(null);
        if (c != null && "Activo".equals(c.getEstado())) {
            throw new RecursoDuplicadoException("Ya existe el curso \"" + c.getNombre() + "\" en el catálogo.");
        }
        if (c == null) {
            c = Curso.builder().nombre(nombre).build();
        }
        // Si se había eliminado (estaba en alguna sección), vuelve al catálogo.
        c.setNombre(nombre);
        c.setCategoria(cat.clave());
        c.setColor(cat.color());
        c.setEstado("Activo");
        c = cursoRepository.save(c);
        return CursoResponse.fromEntity(c, seccionCursoRepository.countByCursoId(c.getId()));
    }

    @Transactional
    public CursoResponse actualizar(Long id, CursoRequest req) {
        Curso c = buscar(id);
        String nombre = nombre(req);
        CategoriaCurso.Info cat = categoria(req);
        cursoRepository.findByNombreIgnoreCase(nombre)
                .filter(otro -> !otro.getId().equals(c.getId()))
                .ifPresent(otro -> { throw new RecursoDuplicadoException("Ya existe el curso \"" + otro.getNombre() + "\"."); });
        c.setNombre(nombre);
        c.setCategoria(cat.clave());
        c.setColor(cat.color());
        Curso guardado = cursoRepository.save(c);
        return CursoResponse.fromEntity(guardado, seccionCursoRepository.countByCursoId(guardado.getId()));
    }

    /**
     * Sin secciones: se borra. Si ya está en alguna sección, sale del catálogo
     * (no se ofrece más) pero esas secciones y sus horarios lo conservan.
     * @return cuántas secciones lo conservan
     */
    @Transactional
    public long eliminar(Long id) {
        Curso c = buscar(id);
        long usos = seccionCursoRepository.countByCursoId(c.getId());
        if (usos == 0) {
            cursoRepository.delete(c);
        } else {
            c.setEstado("Inactivo");
            cursoRepository.save(c);
        }
        return usos;
    }

    private static String nombre(CursoRequest req) {
        String n = req == null || req.nombre() == null ? "" : req.nombre().trim().replaceAll("\\s+", " ");
        if (n.isEmpty()) throw new DatosInvalidosException("Ingresa el nombre del curso.");
        if (n.length() < 3) throw new DatosInvalidosException("El nombre del curso debe tener al menos 3 caracteres.");
        if (n.length() > 80) throw new DatosInvalidosException("El nombre admite hasta 80 caracteres.");
        return n;
    }

    private static CategoriaCurso.Info categoria(CursoRequest req) {
        CategoriaCurso.Info cat = CategoriaCurso.de(req == null ? null : req.categoria());
        if (cat == null) throw new DatosInvalidosException("Selecciona una categoría.");
        return cat;
    }

    private Curso buscar(Long id) {
        return cursoRepository.findById(id)
                .filter(c -> "Activo".equals(c.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado."));
    }
}
