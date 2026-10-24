package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.PeriodoDto;
import com.CristoMorado_api.CristoMorado_api.entity.Periodo;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.PeriodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Académico > Periodos: bimestres del año escolar.
 * Un bimestre por año (1 a 4), sin cruces de fechas. El periodo activo NO se elige a mano:
 * es el que contiene la fecha de hoy (entre bimestres no hay periodo activo).
 */
@Service
public class PeriodoService {

    private static final String[] ROMANO = {"", "I", "II", "III", "IV"};
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PeriodoRepository periodoRepository;

    public PeriodoService(PeriodoRepository periodoRepository) {
        this.periodoRepository = periodoRepository;
    }

    /** Del más reciente al más antiguo; dentro del año, del I al IV bimestre. */
    @Transactional
    public List<PeriodoDto.Response> listar() {
        sincronizarActivo();
        return periodoRepository.findAll().stream()
                .sorted(Comparator.comparing(Periodo::getAnio).reversed().thenComparing(Periodo::getBimestre))
                .map(PeriodoDto.Response::fromEntity)
                .toList();
    }

    @Transactional
    public PeriodoDto.Response crear(PeriodoDto.Request req) {
        Periodo p = Periodo.builder().build();
        aplicar(p, req, null);
        p.setActivo(p.vigenteEn(LocalDate.now()));
        return PeriodoDto.Response.fromEntity(periodoRepository.save(p));
    }

    /** Editar el nombre, el año, el bimestre o las fechas de un periodo (con las mismas reglas que al crearlo). */
    @Transactional
    public PeriodoDto.Response actualizar(Long id, PeriodoDto.Request req) {
        Periodo p = periodoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Periodo no encontrado."));
        aplicar(p, req, p.getId());
        p.setActivo(p.vigenteEn(LocalDate.now()));
        return PeriodoDto.Response.fromEntity(periodoRepository.save(p));
    }

    /** Valida los datos y los copia al periodo. idActual = periodo que se edita (se excluye de los choques). */
    private void aplicar(Periodo p, PeriodoDto.Request req, Long idActual) {
        if (req == null) throw new DatosInvalidosException("Faltan los datos del periodo.");
        Integer anio = req.anio();
        if (anio == null || anio < 2000 || anio > 2100) throw new DatosInvalidosException("Ingresa un año válido.");
        Integer bim = req.bimestre();
        if (bim == null || bim < 1 || bim > 4) throw new DatosInvalidosException("Selecciona el bimestre (1° a 4°).");
        boolean repetido = periodoRepository.findAll().stream()
                .anyMatch(o -> !o.getId().equals(idActual) && anio.equals(o.getAnio()) && bim.equals(o.getBimestre()));
        if (repetido) {
            throw new RecursoDuplicadoException("El " + ROMANO[bim] + " Bimestre de " + anio + " ya está registrado.");
        }
        if (req.fechaInicio() == null || req.fechaFin() == null) throw new DatosInvalidosException("Indica la fecha de inicio y de fin.");
        if (!req.fechaFin().isAfter(req.fechaInicio())) throw new DatosInvalidosException("La fecha fin debe ser posterior a la fecha de inicio.");
        if (req.fechaInicio().getYear() != anio || req.fechaFin().getYear() != anio) {
            throw new DatosInvalidosException("Las fechas deben estar dentro del año " + anio + ".");
        }
        for (Periodo otro : periodoRepository.findAll()) {
            if (otro.getId().equals(idActual)) continue;
            if (!req.fechaInicio().isAfter(otro.getFechaFin()) && !req.fechaFin().isBefore(otro.getFechaInicio())) {
                throw new DatosInvalidosException("Las fechas se cruzan con " + otro.getNombre() + " ("
                        + otro.getFechaInicio().format(DMY) + " al " + otro.getFechaFin().format(DMY) + ").");
            }
        }
        String nombre = req.nombre() == null || req.nombre().isBlank()
                ? ROMANO[bim] + " Bimestre " + anio
                : req.nombre().trim().replaceAll("\\s+", " ");
        if (nombre.length() > 80) throw new DatosInvalidosException("El nombre admite hasta 80 caracteres.");
        p.setNombre(nombre);
        p.setAnio(anio);
        p.setBimestre(bim);
        p.setFechaInicio(req.fechaInicio());
        p.setFechaFin(req.fechaFin());
    }

    /**
     * Guarda en la columna "activo" si cada periodo contiene la fecha de hoy
     * (para que la base de datos muestre lo mismo que el sistema).
     */
    @Transactional
    public void sincronizarActivo() {
        LocalDate hoy = LocalDate.now();
        for (Periodo p : periodoRepository.findAll()) {
            boolean vigente = p.vigenteEn(hoy);
            if (!Boolean.valueOf(vigente).equals(p.getActivo())) {
                p.setActivo(vigente);
                periodoRepository.save(p);
            }
        }
    }
}
