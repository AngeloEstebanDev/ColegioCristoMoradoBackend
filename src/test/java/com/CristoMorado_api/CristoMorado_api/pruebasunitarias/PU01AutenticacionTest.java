package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.controller.AuthController;
import com.CristoMorado_api.CristoMorado_api.dto.UsuarioResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.services.PermisoService;
import com.CristoMorado_api.CristoMorado_api.services.VerificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-01 – HU-01: Autenticación de usuarios (Épica 1, Sprint 1).
 * Prueba unitaria del método AuthController.login con la base de datos simulada (Mockito):
 * no se levanta el servidor ni PostgreSQL.
 */
@DisplayName("PU-01 – HU-01: Autenticación de usuarios")
class PU01AutenticacionTest {

    private UsuarioRepository usuarioRepository;
    private PermisoService permisoService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        permisoService = mock(PermisoService.class);
        controller = new AuthController(usuarioRepository, mock(VerificacionService.class), permisoService);
        Usuario juan = Usuario.builder().id(32L).nombre("Juan Pérez García").usuario("ALU87654321")
                .password("1234").rol("ALUMNO").estado(true).build();
        when(usuarioRepository.findByUsuarioIgnoreCase("ALU87654321")).thenReturn(Optional.of(juan));
        when(permisoService.permisosDe(juan)).thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> error(ResponseEntity<?> r) {
        return (Map<String, String>) r.getBody();
    }

    @Test
    @DisplayName("PU-01.1 Credenciales correctas: autenticación exitosa (200)")
    void c1_credencialesCorrectas() {
        ResponseEntity<?> r = controller.login(new AuthController.LoginRequest("ALU87654321", "1234"));

        assertEquals(200, r.getStatusCode().value());
        UsuarioResponse u = (UsuarioResponse) r.getBody();
        assertEquals("ALU87654321", u.usuario());
        assertEquals("ALUMNO", u.rol());
    }

    @Test
    @DisplayName("PU-01.2 Contraseña incorrecta: autenticación fallida (401)")
    void c2_passwordIncorrecta() {
        ResponseEntity<?> r = controller.login(new AuthController.LoginRequest("ALU87654321", "9999"));

        assertEquals(401, r.getStatusCode().value());
        assertEquals("Contraseña incorrecta.", error(r).get("mensaje"));
        assertEquals("password", error(r).get("campo"));
    }

    @Test
    @DisplayName("PU-01.3 Campos vacíos: se rechazan los datos (400) sin consultar la base de datos")
    void c3_camposVacios() {
        ResponseEntity<?> r = controller.login(new AuthController.LoginRequest("   ", ""));

        assertEquals(400, r.getStatusCode().value());
        assertEquals("Ingresa tu usuario y contraseña.", error(r).get("mensaje"));
        verifyNoInteractions(usuarioRepository);
    }
}
