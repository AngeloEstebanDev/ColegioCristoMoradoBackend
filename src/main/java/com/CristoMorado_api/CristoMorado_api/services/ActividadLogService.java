package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.entity.ActividadLog;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.repository.ActividadLogRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Actividad reciente (Dashboard del administrador y la Directiva).
 * Traduce cada petición de guardar/editar/eliminar a una frase ("Registró un alumno", módulo "Alumnos")
 * y la guarda en actividad_log. Los registros de más de 90 días se borran solos.
 */
@Service
public class ActividadLogService {

    private static final Logger log = LoggerFactory.getLogger(ActividadLogService.class);
    private static final int DIAS_GUARDADOS = 90;

    /** Método HTTP + ruta (regex) -> módulo y acción. La primera que coincide gana. */
    private record Regla(String metodo, Pattern ruta, String modulo, String accion) {}

    private static final List<Regla> REGLAS = List.of(
            // Usuarios
            r("POST", "/api/estudiantes", "Alumnos", "Registró un alumno"),
            r("PUT", "/api/estudiantes/\\d+", "Alumnos", "Editó los datos de un alumno"),
            r("PATCH", "/api/estudiantes/\\d+/estado", "Alumnos", "Cambió el estado de un alumno"),
            r("DELETE", "/api/estudiantes/\\d+", "Alumnos", "Eliminó un alumno"),
            r("POST", "/api/padres", "Padres", "Registró un padre de familia"),
            r("PUT", "/api/padres/\\d+", "Padres", "Editó los datos de un padre de familia"),
            r("PATCH", "/api/padres/\\d+/estado", "Padres", "Cambió el estado de un padre de familia"),
            r("DELETE", "/api/padres/\\d+", "Padres", "Eliminó un padre de familia"),
            r("POST", "/api/docentes", "Docentes", "Registró un docente"),
            r("PUT", "/api/docentes/\\d+", "Docentes", "Editó los datos de un docente"),
            r("PATCH", "/api/docentes/\\d+/estado", "Docentes", "Cambió el estado de un docente"),
            r("DELETE", "/api/docentes/\\d+", "Docentes", "Eliminó un docente"),
            r("POST", "/api/usuarios", "Usuarios y Roles", "Creó una cuenta de usuario"),
            r("PUT", "/api/usuarios/\\d+", "Usuarios y Roles", "Editó una cuenta de usuario"),
            r("PATCH", "/api/usuarios/\\d+/estado", "Usuarios y Roles", "Cambió el estado de una cuenta"),
            r("DELETE", "/api/usuarios/\\d+", "Usuarios y Roles", "Eliminó una cuenta de usuario"),
            r("POST", "/api/personal", "Personal", "Registró personal administrativo"),
            r("PUT", "/api/personal/\\d+/permisos", "Personal", "Cambió los permisos de un miembro del personal"),
            r("PUT", "/api/personal/\\d+", "Personal", "Editó los datos de un miembro del personal"),
            r("PATCH", "/api/personal/\\d+/estado", "Personal", "Cambió el estado de un miembro del personal"),
            // Académico
            r("POST", "/api/secciones", "Grados y Secciones", "Creó una sección"),
            r("PUT", "/api/secciones/\\d+/horario", "Grados y Secciones", "Actualizó el horario de una sección"),
            r("PUT", "/api/secciones/\\d+", "Grados y Secciones", "Editó una sección"),
            r("PATCH", "/api/secciones/\\d+/estado", "Grados y Secciones", "Cambió el estado de una sección"),
            r("PATCH", "/api/secciones/\\d+/tutor", "Grados y Secciones", "Cambió el tutor de una sección"),
            r("POST", "/api/secciones/\\d+/alumnos", "Grados y Secciones", "Matriculó un alumno en una sección"),
            r("DELETE", "/api/secciones/\\d+/alumnos/\\d+", "Grados y Secciones", "Retiró un alumno de una sección"),
            r("POST", "/api/secciones/\\d+/cursos", "Grados y Secciones", "Asignó un curso a una sección"),
            r("PATCH", "/api/secciones/\\d+/cursos/\\d+/docente", "Grados y Secciones", "Cambió el docente de un curso"),
            r("PATCH", "/api/secciones/\\d+/cursos/\\d+/descripcion", "Grados y Secciones", "Editó la descripción de un curso"),
            r("PATCH", "/api/secciones/\\d+/cursos/\\d+/estado", "Grados y Secciones", "Cambió el estado de un curso de la sección"),
            r("POST", "/api/cursos", "Catálogo de Cursos", "Creó un curso"),
            r("PUT", "/api/cursos/\\d+", "Catálogo de Cursos", "Editó un curso"),
            r("DELETE", "/api/cursos/\\d+", "Catálogo de Cursos", "Eliminó un curso"),
            r("POST", "/api/periodos", "Periodos", "Creó un periodo"),
            r("PUT", "/api/periodos/\\d+", "Periodos", "Editó un periodo"),
            r("POST", "/api/vinculos", "Vínculo Padre-Hijo", "Vinculó un padre con un alumno"),
            r("PUT", "/api/vinculos/\\d+", "Vínculo Padre-Hijo", "Editó un vínculo padre-hijo"),
            r("DELETE", "/api/vinculos/\\d+", "Vínculo Padre-Hijo", "Eliminó un vínculo padre-hijo"),
            // Comunicación
            r("POST", "/api/comunicados", "Comunicados", "Publicó un comunicado"),
            r("PUT", "/api/comunicados/\\d+", "Comunicados", "Editó un comunicado"),
            r("DELETE", "/api/comunicados/\\d+", "Comunicados", "Eliminó un comunicado"),
            r("POST", "/api/mensajes", "Mensajes", "Envió un mensaje"),
            r("POST", "/api/mensajes/conversaciones/\\d+", "Mensajes", "Envió un mensaje por chat"),
            r("DELETE", "/api/mensajes/conversaciones/\\d+", "Mensajes", "Eliminó una conversación"),
            r("DELETE", "/api/mensajes/\\d+", "Mensajes", "Eliminó un mensaje"),
            // Agenda
            r("POST", "/api/agenda/citas/solicitar", "Agenda", "Solicitó una cita"),
            r("POST", "/api/agenda/citas", "Agenda", "Programó una reunión"),
            r("PATCH", "/api/agenda/citas/\\d+/confirmar", "Agenda", "Confirmó una cita"),
            r("PATCH", "/api/agenda/citas/\\d+/rechazar", "Agenda", "Rechazó una cita"),
            r("PATCH", "/api/agenda/citas/\\d+/cancelar", "Agenda", "Canceló una cita"),
            r("PATCH", "/api/agenda/citas/\\d+/reprogramar", "Agenda", "Reprogramó una cita"),
            r("PATCH", "/api/agenda/citas/\\d+/realizada", "Agenda", "Marcó una cita como realizada"),
            r("DELETE", "/api/agenda/citas/\\d+", "Agenda", "Eliminó una cita"),
            r("*", "/api/agenda/disponibilidad.*", "Agenda", "Actualizó su disponibilidad de citas"),
            // Aula, notas y asistencia
            r("POST", "/api/aula/cursos/\\d+/materiales", "Aula virtual", "Publicó un material"),
            r("PUT", "/api/aula/materiales/\\d+", "Aula virtual", "Editó un material"),
            r("DELETE", "/api/aula/materiales/\\d+", "Aula virtual", "Eliminó un material"),
            r("POST", "/api/aula/cursos/\\d+/carpetas", "Aula virtual", "Creó una carpeta"),
            r("PUT", "/api/aula/carpetas/\\d+", "Aula virtual", "Editó una carpeta"),
            r("DELETE", "/api/aula/carpetas/\\d+", "Aula virtual", "Eliminó una carpeta"),
            r("POST", "/api/aula/cursos/\\d+/actividades", "Aula virtual", "Publicó una actividad"),
            r("PUT", "/api/aula/actividades/\\d+", "Aula virtual", "Editó una actividad"),
            r("DELETE", "/api/aula/actividades/\\d+", "Aula virtual", "Eliminó una actividad"),
            r("PATCH", "/api/aula/actividades/\\d+/tardia", "Aula virtual", "Cambió las entregas tardías de una actividad"),
            r("PATCH", "/api/aula/(materiales|carpetas|actividades)/\\d+/visible", "Aula virtual", "Cambió la visibilidad de un contenido"),
            r("POST", "/api/aula/actividades/\\d+/entrega", "Aula virtual", "Envió una entrega"),
            r("PATCH", "/api/aula/entregas/\\d+/calificar", "Aula virtual", "Calificó una entrega"),
            r("POST", "/api/notas/cursos/\\d+/evaluaciones", "Notas", "Creó una evaluación"),
            r("PUT", "/api/notas/evaluaciones/\\d+", "Notas", "Editó una evaluación"),
            r("DELETE", "/api/notas/evaluaciones/\\d+", "Notas", "Eliminó una evaluación"),
            r("PUT", "/api/notas/cursos/\\d+/notas", "Notas", "Registró notas"),
            r("PUT", "/api/asistencia/cursos/\\d+", "Asistencia", "Registró asistencia"),
            r("POST", "/api/asistencia/cursos/\\d+/sesiones", "Asistencia", "Creó una sesión de asistencia"),
            // Cuenta
            r("PUT", "/api/perfil/\\d+", "Mi Perfil", "Actualizó su perfil"),
            r("POST", "/api/perfil/\\d+/correo/verificar", "Mi Perfil", "Vinculó su Gmail"),
            r("PATCH", "/api/perfil/\\d+/password", "Mi Perfil", "Cambió su contraseña")
    );

    private final ActividadLogRepository repo;
    private final UsuarioRepository usuarioRepository;
    private final AtomicInteger contador = new AtomicInteger();

    public ActividadLogService(ActividadLogRepository repo, UsuarioRepository usuarioRepository) {
        this.repo = repo;
        this.usuarioRepository = usuarioRepository;
    }

    private static Regla r(String metodo, String ruta, String modulo, String accion) {
        return new Regla(metodo, Pattern.compile(ruta + "/?"), modulo, accion);
    }

    /** Módulo y acción de una petición de escritura, o null si no se registra (p. ej. marcar como leído). */
    public static String[] describir(String metodo, String ruta) {
        if (metodo == null || ruta == null) return null;
        for (Regla g : REGLAS) {
            if ((g.metodo().equals("*") || g.metodo().equalsIgnoreCase(metodo)) && g.ruta().matcher(ruta).matches()) {
                return new String[]{g.modulo(), g.accion()};
            }
        }
        return null;
    }

    /** Guarda una acción. Nunca lanza errores: la actividad no debe romper la operación del usuario. */
    @Transactional
    public void registrar(Long usuarioId, String nombreEscrito, String modulo, String accion, String estado) {
        try {
            Usuario u = usuarioId == null ? null : usuarioRepository.findById(usuarioId).orElse(null);
            String nombre = u != null ? u.getNombre() : (nombreEscrito == null || nombreEscrito.isBlank() ? "Visitante" : nombreEscrito.trim());
            repo.save(ActividadLog.builder()
                    .usuarioId(u == null ? null : u.getId())
                    .usuario(recortar(nombre, 150))
                    .rol(u == null ? null : recortar(u.getRol(), 30))
                    .modulo(recortar(modulo, 60))
                    .accion(recortar(accion, 160))
                    .estado(estado)
                    .build());
            if (contador.incrementAndGet() % 200 == 0) {
                repo.borrarAnterioresA(LocalDateTime.now().minusDays(DIAS_GUARDADOS));
            }
        } catch (Exception e) {
            log.warn("No se pudo guardar la actividad '{}': {}", accion, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ActividadLog> recientes() {
        return repo.findTop20ByOrderByFechaDescIdDesc();
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
