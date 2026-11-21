package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.entity.Personal;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.repository.PersonalRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.util.Permiso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quién puede hacer qué en el panel de administración:
 * - Administrador (ADMIN): todo, siempre.
 * - Directiva (DIRECTIVO): solo los permisos que el administrador le habilitó.
 * - Docentes, alumnos y apoderados: ninguno.
 */
@Service
public class PermisoService {

    private final UsuarioRepository usuarioRepository;
    private final PersonalRepository personalRepository;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public PermisoService(UsuarioRepository usuarioRepository, PersonalRepository personalRepository) {
        this.usuarioRepository = usuarioRepository;
        this.personalRepository = personalRepository;
    }

    /**
     * Indica si es administrador (devuelve verdadero o falso).
     */
    public static boolean esAdministrador(Usuario u) {
        String r = u.getRol() == null ? "" : u.getRol().trim().toLowerCase();
        return r.equals("admin") || r.equals("administrador");
    }

    /**
     * Indica si es directiva (devuelve verdadero o falso).
     */
    public static boolean esDirectiva(Usuario u) {
        String r = u.getRol() == null ? "" : u.getRol().trim().toLowerCase();
        return r.equals("directivo") || r.equals("directiva") || r.equals("director");
    }

    /**
     * Devuelve permisos de (como lista). Solo lee datos (transacción de solo lectura).
     */
    @Transactional(readOnly = true)
    public List<String> permisosDe(Usuario u) {
        if (esAdministrador(u)) return Permiso.todos();
        if (esDirectiva(u)) {
            Personal p = personalRepository.findByUsuarioId(u.getId()).orElse(null);
            if (p == null) return List.of();
            return Permiso.todos().stream().filter(p.getPermisos()::contains).toList();
        }
        return List.of();
    }

    /**
     * Indica si tiene lo indicado (devuelve verdadero o falso). Solo lee datos (transacción de solo
     * lectura).
     */
    @Transactional(readOnly = true)
    public boolean tiene(Usuario u, Permiso permiso) {
        return Boolean.TRUE.equals(u.getEstado()) && permisosDe(u).contains(permiso.name());
    }

    /** Devuelve al usuario si tiene el permiso; si no, 403. */
    @Transactional(readOnly = true)
    public Usuario exigir(Long actorId, Permiso permiso) {
        Usuario u = actor(actorId);
        if (!tiene(u, permiso)) {
            throw new AccesoDenegadoException("No tienes permiso para " + descripcion(permiso) + ".");
        }
        return u;
    }

    /** Solo el administrador (no la Directiva). */
    @Transactional(readOnly = true)
    public Usuario exigirAdministrador(Long actorId) {
        Usuario u = actor(actorId);
        if (!esAdministrador(u)) {
            throw new AccesoDenegadoException("Solo el administrador puede hacer esto.");
        }
        return u;
    }

    /**
     * Devuelve actor. Si el usuario no tiene permiso para esta acción, responde con error 403.
     */
    private Usuario actor(Long actorId) {
        if (actorId == null) {
            throw new AccesoDenegadoException("Falta identificar al usuario que realiza la acción.");
        }
        return usuarioRepository.findById(actorId)
                .orElseThrow(() -> new AccesoDenegadoException("Usuario no válido."));
    }

    /**
     * Devuelve descripción (como texto).
     */
    private static String descripcion(Permiso p) {
        return switch (p) {
            case COMUNICADOS -> "publicar comunicados";
            case REPORTES -> "ver los reportes globales";
            case ACADEMICO -> "gestionar la parte académica";
            case USUARIOS -> "gestionar usuarios";
            case PERMISOS -> "gestionar permisos";
        };
    }
}
