package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.ArchivoDto;
import com.CristoMorado_api.CristoMorado_api.entity.ArchivoAdjunto;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.AccesoService;
import com.CristoMorado_api.CristoMorado_api.services.ArchivoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-11 – HU-13 y HU-15: Validación de archivos (Épica 4, Sprint 4).
 * ArchivoService.guardar valida los materiales (HU-13) y las entregas (HU-15): extensión, contenido real y tamaño (máx. 10 MB).
 */
@DisplayName("PU-11 – HU-13 y HU-15: Validación de archivos")
class PU11ArchivosTest {

    private ArchivoAdjuntoRepository archivoRepository;
    private ArchivoService servicio;

    @BeforeEach
    void setUp() {
        archivoRepository = mock(ArchivoAdjuntoRepository.class);
        servicio = new ArchivoService(archivoRepository, mock(MaterialRepository.class), mock(ActividadRepository.class),
                mock(EntregaRepository.class), mock(EntregaEnvioRepository.class), mock(AccesoService.class));
        when(archivoRepository.save(any(ArchivoAdjunto.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Archivo como lo envía el navegador: data URL en base64. */
    private static ArchivoDto.Request archivo(String nombre, String tipo, byte[] datos) {
        return new ArchivoDto.Request(nombre, tipo, "data:" + tipo + ";base64," + Base64.getEncoder().encodeToString(datos));
    }

    @Test
    @DisplayName("PU-11.1 Archivo con extensión permitida (.pdf): la validación lo acepta")
    void c1_extensionPermitida() {
        ArchivoAdjunto a = servicio.guardar(archivo("tarea_fracciones.pdf", "application/pdf",
                "%PDF-1.4 tarea de Juan".getBytes(StandardCharsets.UTF_8)), 32L);

        assertNotNull(a);
        assertEquals("tarea_fracciones.pdf", a.getNombre());
        verify(archivoRepository, times(1)).save(any(ArchivoAdjunto.class));
    }

    @Test
    @DisplayName("PU-11.2 Archivo .exe: la validación lo rechaza")
    void c2_exeRechazado() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> servicio.guardar(archivo("juego.exe", "application/octet-stream", new byte[]{'M', 'Z', 0, 1}), 32L));

        assertEquals("No se permiten archivos .exe por seguridad.", ex.getMessage());
        verify(archivoRepository, never()).save(any(ArchivoAdjunto.class));
    }

    @Test
    @DisplayName("PU-11.3 Archivo que excede el tamaño máximo (10 MB): se rechaza")
    void c3_excedeTamanio() {
        byte[] grande = new byte[(int) ArchivoService.MAX_BYTES + 1024];
        Arrays.fill(grande, (byte) 'a');
        byte[] inicio = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(inicio, 0, grande, 0, inicio.length);

        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> servicio.guardar(archivo("libro.pdf", "application/pdf", grande), 32L));

        assertEquals("El archivo pesa más de 10 MB.", ex.getMessage());
        verify(archivoRepository, never()).save(any(ArchivoAdjunto.class));
    }
}
