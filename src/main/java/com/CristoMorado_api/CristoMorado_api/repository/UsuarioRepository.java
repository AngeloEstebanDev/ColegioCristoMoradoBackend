package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repositorio de la entidad Usuario: es la "puerta" a la tabla de las cuentas de acceso al sistema
 * (usuario, contraseña, rol y estado) en PostgreSQL. Spring Data JPA crea solo las consultas a partir
 * del nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL.
 * Los métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca los registros cuyo correo sea igual al valor recibido. Devuelve un Optional: vacío si no
     * encuentra ninguno.
     */
    Optional<Usuario> findByCorreo(String correo);

    /**
     * Busca los registros cuyo usuario (sin importar mayúsculas). Devuelve un Optional: vacío si no
     * encuentra ninguno.
     */
    Optional<Usuario> findByUsuarioIgnoreCase(String usuario);

    /**
     * Indica si existe algún registro cuyo usuario (sin importar mayúsculas).
     */
    boolean existsByUsuarioIgnoreCase(String usuario);

    /**
     * Busca los registros cuyo correo (sin importar mayúsculas). Devuelve un Optional: vacío si no
     * encuentra ninguno.
     */
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    /**
     * Indica si existe algún registro cuyo correo sea igual al valor recibido.
     */
    boolean existsByCorreo(String correo);

    /**
     * Indica si existe algún registro cuyo correo (sin importar mayúsculas).
     */
    boolean existsByCorreoIgnoreCase(String correo);

    /**
     * Indica si existe algún registro cuyo usuario sea igual al valor recibido.
     */
    boolean existsByUsuario(String usuario);

    /**
     * Cuenta los registros cuyo estado sea verdadero.
     */
    long countByEstadoTrue();

    /**
     * Busca los registros cuyo estado sea verdadero, ordenados por nombre (de menor a mayor).
     */
    java.util.List<Usuario> findByEstadoTrueOrderByNombreAsc();

    /**
     * Cuenta los registros cuyo rol (sin importar mayúsculas).
     */
    long countByRolIgnoreCase(String rol);

    /**
     * Índice de fotos de perfil: [id, valor] solo de las cuentas que tienen foto.
     * valor = ruta del avatar del colegio ("/avatares/x.svg") o "v" + md5 de la foto subida
     * (la versión cambia cuando el usuario cambia su foto). No carga las imágenes.
     */
    @Query(value = "SELECT id, CASE WHEN foto LIKE 'data:%' THEN 'v' || md5(foto) ELSE foto END "
            + "FROM usuarios WHERE foto IS NOT NULL AND foto <> ''", nativeQuery = true)
    /**
     * Consulta «indice fotos» sobre la tabla.
     */
    java.util.List<Object[]> indiceFotos();
}
