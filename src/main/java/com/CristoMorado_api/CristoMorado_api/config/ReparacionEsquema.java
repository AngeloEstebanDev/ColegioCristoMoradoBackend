package com.CristoMorado_api.CristoMorado_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Corrige columnas que Hibernate (ddl-auto=update) no puede crear solo.
 *
 * Problema: cuando se agrega a una entidad un campo "nullable = false" y la tabla
 * ya tiene filas, PostgreSQL rechaza "ADD COLUMN ... NOT NULL" sin valor por defecto.
 * Hibernate solo registra el error y la columna NO se crea; luego cada consulta falla.
 * Eso pasaba con mensajes.aviso_sistema: el chat respondía "No se pudo conectar con el servidor".
 *
 * Al iniciar, este componente agrega las columnas que falten con su valor por defecto.
 * Todas las sentencias se pueden ejecutar muchas veces (IF NOT EXISTS).
 */
@Component
public class ReparacionEsquema implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReparacionEsquema.class);

    private static final List<String> SENTENCIAS = List.of(
            // Mensajería (chat y avisos del colegio)
            "ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS aviso_sistema BOOLEAN NOT NULL DEFAULT FALSE",
            "ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS eliminado_remitente BOOLEAN NOT NULL DEFAULT FALSE",
            "ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS respuesta_a_id BIGINT",
            // Los avisos automáticos no tienen remitente.
            "ALTER TABLE mensajes ALTER COLUMN remitente_id DROP NOT NULL",
            "ALTER TABLE mensaje_destinatarios ADD COLUMN IF NOT EXISTS leido BOOLEAN NOT NULL DEFAULT FALSE",
            "ALTER TABLE mensaje_destinatarios ADD COLUMN IF NOT EXISTS eliminado BOOLEAN NOT NULL DEFAULT FALSE",
            "ALTER TABLE mensaje_destinatarios ADD COLUMN IF NOT EXISTS fecha_lectura TIMESTAMP",
            // Mi perfil: foto, teléfono y dirección
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS foto TEXT",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS telefono VARCHAR(20)",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS direccion VARCHAR(200)",
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS direccion VARCHAR(200)",
            // Mis Cursos del docente: tipos de contenido, ocultar, carpetas, intentos y selección múltiple
            "ALTER TABLE materiales ADD COLUMN IF NOT EXISTS tipo VARCHAR(20)",
            "ALTER TABLE materiales ADD COLUMN IF NOT EXISTS visible BOOLEAN NOT NULL DEFAULT TRUE",
            "ALTER TABLE materiales ADD COLUMN IF NOT EXISTS carpeta_id BIGINT",
            "ALTER TABLE actividades ADD COLUMN IF NOT EXISTS visible BOOLEAN NOT NULL DEFAULT TRUE",
            "ALTER TABLE actividades ADD COLUMN IF NOT EXISTS intentos_permitidos INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE actividades ADD COLUMN IF NOT EXISTS carpeta_id BIGINT",
            "ALTER TABLE preguntas_cuestionario ADD COLUMN IF NOT EXISTS tipo VARCHAR(10) NOT NULL DEFAULT 'UNICA'",
            "ALTER TABLE entregas ADD COLUMN IF NOT EXISTS intentos INTEGER NOT NULL DEFAULT 1",
            // Mi Agenda: descripción adicional de la cita
            "ALTER TABLE citas ADD COLUMN IF NOT EXISTS detalle TEXT",
            // Entregas: formatos permitidos (el historial entrega_envios lo crea Hibernate)
            "ALTER TABLE actividades ADD COLUMN IF NOT EXISTS formatos_permitidos VARCHAR(100)"
    );

    private final JdbcTemplate jdbc;

    public ReparacionEsquema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String sql : SENTENCIAS) {
            try {
                jdbc.execute(sql);
            } catch (Exception e) {
                // La tabla aún no existe (base nueva) u otro detalle: no detiene el arranque.
                log.warn("Reparación de esquema omitida [{}]: {}", sql, e.getMessage());
            }
        }
    }
}
