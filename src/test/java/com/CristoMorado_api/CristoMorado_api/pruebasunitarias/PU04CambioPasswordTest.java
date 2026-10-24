package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.entity.CodigoVerificacion;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.CredencialesInvalidasException;
import com.CristoMorado_api.CristoMorado_api.repository.CodigoVerificacionRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.services.CorreoService;
import com.CristoMorado_api.CristoMorado_api.services.VerificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-04 – HU-33: Cambio de contraseña (Épica 1, Sprint 1).
 * VerificacionService.cambiarPassword exige la contraseña actual correcta y el código de 6 dígitos del Gmail.
 * El caso PU-04.2 (nueva contraseña y su confirmación diferentes) lo valida el formulario del frontend:
 * ver CristoMorado-frontend/src/app/pruebas-unitarias/pu04-confirmacion.spec.ts (Vitest).
 */
@DisplayName("PU-04 – HU-33: Cambio de contraseña")
class PU04CambioPasswordTest {

    private CodigoVerificacionRepository codigoRepository;
    private UsuarioRepository usuarioRepository;
    private VerificacionService servicio;
    private Usuario juan;

    @BeforeEach
    void setUp() throws Exception {
        codigoRepository = mock(CodigoVerificacionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        servicio = new VerificacionService(codigoRepository, usuarioRepository, mock(CorreoService.class), true);
        juan = Usuario.builder().id(30L).usuario("APO76543210").password("1234").estado(true)
                .correo("juan.perez@gmail.com").correoVerificado(true).build();
        when(usuarioRepository.findById(30L)).thenReturn(Optional.of(juan));
        when(codigoRepository.save(any(CodigoVerificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        // Código "482915" enviado al Gmail, vigente 5 minutos más (se guarda solo su hash SHA-256).
        CodigoVerificacion c = CodigoVerificacion.builder().id(1L).usuarioId(30L).tipo(CodigoVerificacion.CAMBIO_PASSWORD)
                .codigoHash(hash("482915", 30L)).expiraEn(LocalDateTime.now().plusMinutes(5)).intentos(0).usado(false).build();
        when(codigoRepository.findTopByUsuarioIdAndTipoAndUsadoFalseOrderByFechaCreacionDesc(30L,
                CodigoVerificacion.CAMBIO_PASSWORD)).thenReturn(Optional.of(c));
    }

    private static String hash(String codigo, Long usuarioId) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest((codigo + ":" + usuarioId).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("PU-04.1 Contraseña actual incorrecta: el cambio se rechaza")
    void c1_actualIncorrecta() {
        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> servicio.cambiarPassword(30L, "9999", "nueva2026", "482915"));

        assertEquals("La contraseña actual no es correcta.", ex.getMessage());
        assertEquals("1234", juan.getPassword(), "La contraseña no debe cambiar");
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("PU-04.3 Datos válidos (actual correcta, nueva válida y código correcto): el cambio se completa")
    void c3_cambioCorrecto() {
        servicio.cambiarPassword(30L, "1234", "nueva2026", "482915");

        assertEquals("nueva2026", juan.getPassword());
        verify(usuarioRepository, times(1)).save(juan);
    }
}
