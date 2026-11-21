package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.ArchivoDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ArchivoService {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Set<String> BLOQUEADAS = Set.of(
            "exe", "bat", "cmd", "com", "msi", "scr", "vbs", "ps1", "sh", "jar", "js", "dll", "apk", "reg");

    public static final Map<String, Set<String>> FORMATOS = new LinkedHashMap<>();
    static {
        FORMATOS.put("PDF", Set.of("pdf"));
        FORMATOS.put("WORD", Set.of("doc", "docx"));
        FORMATOS.put("EXCEL", Set.of("xls", "xlsx"));
        FORMATOS.put("PPT", Set.of("ppt", "pptx"));
        FORMATOS.put("IMAGEN", Set.of("jpg", "jpeg", "png", "gif", "webp"));
    }

    private final ArchivoAdjuntoRepository archivoRepository;
    private final MaterialRepository materialRepository;
    private final ActividadRepository actividadRepository;
    private final EntregaRepository entregaRepository;
    private final EntregaEnvioRepository envioRepository;
    private final AccesoService accesoService;

    public ArchivoService(ArchivoAdjuntoRepository archivoRepository, MaterialRepository materialRepository,
                          ActividadRepository actividadRepository, EntregaRepository entregaRepository,
                          EntregaEnvioRepository envioRepository, AccesoService accesoService) {
        this.archivoRepository = archivoRepository;
        this.materialRepository = materialRepository;
        this.actividadRepository = actividadRepository;
        this.entregaRepository = entregaRepository;
        this.envioRepository = envioRepository;
        this.accesoService = accesoService;
    }

    @Transactional
    public ArchivoAdjunto guardar(ArchivoDto.Request req, Long usuarioId) {
        return guardar(req, usuarioId, null);
    }

    @Transactional
    public ArchivoAdjunto guardar(ArchivoDto.Request req, Long usuarioId, Collection<String> formatos) {
        if (req == null || req.contenido() == null || req.contenido().isBlank()) return null;
        String nombre = limpiarNombre(req.nombre());
        String ext = nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (BLOQUEADAS.contains(ext)) {
            throw new DatosInvalidosException("No se permiten archivos ." + ext + " por seguridad.");
        }
        if (formatos != null && !extensiones(formatos).contains(ext)) {
            throw new DatosInvalidosException("Formato no permitido" + (ext.isEmpty() ? "" : " (." + ext + ")")
                    + ". Solo se aceptan: " + String.join(", ", nombresFormatos(formatos)) + ".");
        }
        String contenido = req.contenido().trim();
        int coma = contenido.indexOf(',');
        if (contenido.startsWith("data:") && coma > 0) contenido = contenido.substring(coma + 1);
        if (contenido.length() > (MAX_BYTES / 3 * 4) + 8) {
            throw new DatosInvalidosException("El archivo pesa más de 10 MB.");
        }
        byte[] datos;
        try {
            datos = Base64.getDecoder().decode(contenido.replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            throw new DatosInvalidosException("El archivo llegó dañado. Vuelve a adjuntarlo.");
        }
        if (datos.length == 0) throw new DatosInvalidosException("El archivo está vacío.");
        if (datos.length > MAX_BYTES) throw new DatosInvalidosException("El archivo pesa más de 10 MB.");
        if (formatos != null && !contenidoCoincide(ext, datos)) {
            throw new DatosInvalidosException("El archivo no es un ." + ext + " válido (puede estar dañado o renombrado).");
        }
        String tipo = req.tipo() == null || req.tipo().isBlank() ? "application/octet-stream" : req.tipo().trim();
        if (tipo.length() > 150) tipo = "application/octet-stream";
        return archivoRepository.save(ArchivoAdjunto.builder()
                .nombre(nombre).tipoMime(tipo).tamanio((long) datos.length).datos(datos).subidoPor(usuarioId)
                .build());
    }

    @Transactional
    public void eliminar(Long archivoId) {
        if (archivoId != null && archivoRepository.existsById(archivoId)) archivoRepository.deleteById(archivoId);
    }

    @Transactional(readOnly = true)
    public ArchivoAdjunto descargar(Long archivoId, Long usuarioId) {
        Usuario u = accesoService.usuario(usuarioId);
        ArchivoAdjunto a = archivoRepository.findById(archivoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El archivo ya no existe."));
        if (u.getId().equals(a.getSubidoPor()) || AccesoService.esAdministrativo(u)) return a;

        Material m = materialRepository.findFirstByArchivoId(archivoId).orElse(null);
        if (m != null && accesoService.puedeVer(u, m.getSeccionCurso())) return a;

        Actividad act = actividadRepository.findFirstByArchivoId(archivoId).orElse(null);
        if (act != null && accesoService.puedeVer(u, act.getSeccionCurso())) return a;

        Entrega e = entregaRepository.findFirstByArchivoId(archivoId).orElse(null);
        if (e == null) e = envioRepository.findFirstByArchivoId(archivoId).map(EntregaEnvio::getEntrega).orElse(null);
        if (e != null) {
            if (AccesoService.esDocenteDe(u.getId(), e.getActividad().getSeccionCurso())) return a;
            try {
                accesoService.estudianteVisible(u.getId(), e.getEstudiante().getId());
                if (!AccesoService.esDocente(u)) return a;
            } catch (AccesoDenegadoException ignorado) {  }
        }
        throw new AccesoDenegadoException("No tienes acceso a este archivo.");
    }

    public static List<String> normalizarFormatos(Collection<String> formatos) {
        if (formatos == null) return List.copyOf(FORMATOS.keySet());
        List<String> res = new ArrayList<>();
        for (String f : formatos) {
            String k = f == null ? "" : f.trim().toUpperCase(Locale.ROOT);
            if (k.isEmpty()) continue;
            if (!FORMATOS.containsKey(k)) throw new DatosInvalidosException("Formato desconocido: " + f + ".");
            if (!res.contains(k)) res.add(k);
        }
        return res.isEmpty() ? List.copyOf(FORMATOS.keySet()) : res;
    }

    private static Set<String> extensiones(Collection<String> formatos) {
        Set<String> ext = new HashSet<>();
        normalizarFormatos(formatos).forEach(f -> ext.addAll(FORMATOS.get(f)));
        return ext;
    }

    private static List<String> nombresFormatos(Collection<String> formatos) {
        return normalizarFormatos(formatos).stream().map(f -> switch (f) {
            case "WORD" -> "Word (.doc, .docx)";
            case "EXCEL" -> "Excel (.xls, .xlsx)";
            case "PPT" -> "PowerPoint (.ppt, .pptx)";
            case "IMAGEN" -> "imágenes (.jpg, .png, .gif, .webp)";
            default -> "PDF";
        }).toList();
    }

    static boolean contenidoCoincide(String ext, byte[] d) {
        return switch (ext) {
            case "pdf" -> empieza(d, 0x25, 0x50, 0x44, 0x46);
            case "docx", "xlsx", "pptx" -> empieza(d, 0x50, 0x4B);
            case "doc", "xls", "ppt" -> empieza(d, 0xD0, 0xCF, 0x11, 0xE0);
            case "png" -> empieza(d, 0x89, 0x50, 0x4E, 0x47);
            case "jpg", "jpeg" -> empieza(d, 0xFF, 0xD8, 0xFF);
            case "gif" -> empieza(d, 0x47, 0x49, 0x46, 0x38);
            case "webp" -> d.length > 12 && empieza(d, 0x52, 0x49, 0x46, 0x46) && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
            default -> true;
        };
    }

    private static boolean empieza(byte[] d, int... firma) {
        if (d.length < firma.length) return false;
        for (int i = 0; i < firma.length; i++) if ((d[i] & 0xFF) != firma[i]) return false;
        return true;
    }

    public static ArchivoDto.Info info(ArchivoAdjunto a) {
        return a == null ? null : new ArchivoDto.Info(a.getId(), a.getNombre(), a.getTipoMime(), a.getTamanio());
    }

    private static String limpiarNombre(String nombre) {
        String n = nombre == null ? "" : nombre.trim();
        n = n.replace("\\", "/");
        if (n.contains("/")) n = n.substring(n.lastIndexOf('/') + 1);
        n = n.replaceAll("[\\r\\n\"<>:|?*]", "_");
        if (n.isBlank()) n = "archivo";
        if (n.length() > 200) {
            int punto = n.lastIndexOf('.');
            String ext = punto > 0 && n.length() - punto <= 10 ? n.substring(punto) : "";
            n = n.substring(0, 200 - ext.length()) + ext;
        }
        return n;
    }
}
