package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import com.CristoMorado_api.CristoMorado_api.util.UsuarioFormato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea y mantiene las cuentas de acceso (tabla `usuarios`) de alumnos,
 * docentes, apoderados y directiva con el formato PREFIJO + DNI.
 * Así las reglas del nombre de usuario viven en un solo lugar.
 */
@Service
public class CuentaService {

    public static final int PASSWORD_MIN = 4;

    private final UsuarioRepository usuarioRepository;
    private final LimpiezaUsuarioService limpiezaUsuarioService;

    public CuentaService(UsuarioRepository usuarioRepository, LimpiezaUsuarioService limpiezaUsuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.limpiezaUsuarioService = limpiezaUsuarioService;
    }

    /**
     * Crea la cuenta. Si no se indica contraseña, la contraseña inicial es el DNI.
     */
    @Transactional
    public Usuario crear(String prefijo, String dni, String nombre, String rol, String password, boolean activo) {
        String usuario = usuarioPara(prefijo, dni);
        if (usuarioRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta con el usuario " + usuario + ".");
        }
        String clave = (password == null || password.isBlank()) ? UsuarioFormato.limpiarDni(dni) : password;
        validarPassword(clave);

        Usuario u = Usuario.builder()
                .nombre(recortar(nombre, 100))
                .usuario(usuario)
                .correo(null)                 // el Gmail lo vincula el propio usuario
                .correoVerificado(false)
                .password(clave)
                .rol(rol)
                .estado(activo)
                .build();
        return usuarioRepository.save(u);
    }

    /**
     * Mantiene la cuenta al día cuando cambian los datos de la persona:
     * nuevo DNI => nuevo usuario, nombre, estado y (opcional) contraseña.
     */
    @Transactional
    public Usuario sincronizar(Usuario cuenta, String prefijo, String dni, String nombre,
                               boolean activo, String passwordNueva) {
        String usuario = usuarioPara(prefijo, dni);
        if (!usuario.equalsIgnoreCase(cuenta.getUsuario())) {
            usuarioRepository.findByUsuarioIgnoreCase(usuario).ifPresent(otro -> {
                if (!otro.getId().equals(cuenta.getId())) {
                    throw new RecursoDuplicadoException("Ya existe una cuenta con el usuario " + usuario + ".");
                }
            });
            cuenta.setUsuario(usuario);
        }
        cuenta.setNombre(recortar(nombre, 100));
        cuenta.setEstado(activo);
        if (passwordNueva != null && !passwordNueva.isBlank()) {
            validarPassword(passwordNueva);
            cuenta.setPassword(passwordNueva);
        }
        return usuarioRepository.save(cuenta);
    }

    /** Solo activa o desactiva el inicio de sesión de la cuenta. */
    @Transactional
    public Usuario cambiarEstado(Usuario cuenta, boolean activo) {
        cuenta.setEstado(activo);
        return usuarioRepository.save(cuenta);
    }

    /** Borra la cuenta soltando antes sus mensajes, lecturas, etc. */
    @Transactional
    public void eliminar(Usuario cuenta) {
        if (cuenta == null) return;
        limpiezaUsuarioService.liberarReferencias(cuenta.getId());
        usuarioRepository.deleteById(cuenta.getId());
    }

    public String usuarioPara(String prefijo, String dni) {
        if (!UsuarioFormato.dniValido(dni)) {
            throw new DatosInvalidosException("El número de documento no es válido.");
        }
        return UsuarioFormato.generar(prefijo, dni);
    }

    public static void validarPassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new DatosInvalidosException(
                    "La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres.");
        }
        com.CristoMorado_api.CristoMorado_api.util.Campos.password(password);
    }

    private static String recortar(String texto, int max) {
        if (texto == null) return null;
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
