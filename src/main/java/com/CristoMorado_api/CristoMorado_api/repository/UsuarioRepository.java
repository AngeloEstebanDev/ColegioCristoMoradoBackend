package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByUsuarioIgnoreCase(String usuario);

    boolean existsByUsuarioIgnoreCase(String usuario);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByUsuario(String usuario);

    long countByEstadoTrue();

    java.util.List<Usuario> findByEstadoTrueOrderByNombreAsc();

    long countByRolIgnoreCase(String rol);

    /**
     * Índice de fotos de perfil: [id, valor] solo de las cuentas que tienen foto.
     * valor = ruta del avatar del colegio ("/avatares/x.svg") o "v" + md5 de la foto subida
     * (la versión cambia cuando el usuario cambia su foto). No carga las imágenes.
     */
    @Query(value = "SELECT id, CASE WHEN foto LIKE 'data:%' THEN 'v' || md5(foto) ELSE foto END "
            + "FROM usuarios WHERE foto IS NOT NULL AND foto <> ''", nativeQuery = true)
    java.util.List<Object[]> indiceFotos();
}
