package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.repository.PersonalRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.services.PermisoService;
import com.CristoMorado_api.CristoMorado_api.util.Permiso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-02 – HU-02 y HU-03: Control de roles y permisos (Épica 1, Sprint 1).
 * PermisoService decide si un usuario puede ejecutar una acción del panel de administración
 * (lo usa PermisoInterceptor en cada petición de gestión).
 */
@DisplayName("PU-02 – HU-02 y HU-03: Control de roles y permisos")
class PU02RolesPermisosTest {

    private UsuarioRepository usuarioRepository;
    private PermisoService servicio;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        servicio = new PermisoService(usuarioRepository, mock(PersonalRepository.class));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(
                Usuario.builder().id(1L).usuario("admin123").rol("ADMIN").estado(true).build()));
        when(usuarioRepository.findById(32L)).thenReturn(Optional.of(
                Usuario.builder().id(32L).usuario("ALU87654321").rol("ALUMNO").estado(true).build()));
    }

    @Test
    @DisplayName("PU-02.1 Rol Administrador con permiso autorizado (USUARIOS): acceso permitido")
    void c1_administradorPermitido() {
        Usuario u = servicio.exigir(1L, Permiso.USUARIOS);

        assertEquals("admin123", u.getUsuario());
        assertTrue(servicio.permisosDe(u).contains("USUARIOS"));
    }

    @Test
    @DisplayName("PU-02.2 Rol Estudiante intenta una acción exclusiva del administrador: acceso denegado")
    void c2_estudianteDenegado() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> servicio.exigir(32L, Permiso.USUARIOS));

        assertTrue(ex.getMessage().startsWith("No tienes permiso"));
        assertThrows(AccesoDenegadoException.class, () -> servicio.exigirAdministrador(32L));
    }

    @Test
    @DisplayName("PU-02.3 Permiso inexistente: no se autoriza la acción")
    void c3_permisoInexistente() {
        assertFalse(Permiso.esValido("BORRAR_TODO"));
        assertFalse(Permiso.esValido(null));
        assertFalse(servicio.permisosDe(usuarioRepository.findById(1L).get()).contains("BORRAR_TODO"));
    }
}
