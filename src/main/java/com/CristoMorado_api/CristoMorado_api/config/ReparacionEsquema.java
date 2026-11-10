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
 *
 * También pasa una sola vez los horarios de clase con las horas antiguas
 * (mañana 07:45-12:35, tarde 13:00-17:50) a las horas reales del colegio
 * (mañana 07:30-13:15, recreo 10:15-10:45; tarde 13:30-19:45, recreo 16:15-16:45).
 * Sin este ajuste, los cursos se dibujaban fuera del cuadro en el horario y en los dashboards,
 * y el editor no dejaba guardar el horario de esas secciones.
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

    /** Hora antigua -> hora real (misma posición en el día). Igual que Database/migracion_2026_10_24.sql. */
    private static final String[][] HORAS = {
            {"07:45", "07:30"}, {"08:30", "08:15"}, {"09:15", "09:00"}, {"10:00", "09:45"},
            {"10:20", "10:45"}, {"11:05", "11:30"}, {"11:50", "12:15"}, {"12:35", "13:00"},
            {"13:00", "13:30"}, {"13:45", "14:15"}, {"14:30", "15:00"}, {"15:15", "15:45"},
            {"15:35", "16:45"}, {"16:20", "17:30"}, {"17:05", "18:15"}, {"17:50", "19:00"}};
    private static final String AJUSTE_HORARIOS = "horarios_reales_2026_10_24";

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
        ajustarHorariosAntiguos();
    }

    /**
     * Mueve a las horas reales los bloques de las secciones cuyo horario completo sigue con las horas antiguas.
     * Se ejecuta una sola vez (queda anotado en ajustes_datos) y no toca secciones ya editadas con las horas nuevas.
     */
    void ajustarHorariosAntiguos() {
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS ajustes_datos (nombre VARCHAR(80) PRIMARY KEY, fecha TIMESTAMP NOT NULL DEFAULT NOW())");
            Integer hecho = jdbc.queryForObject("SELECT COUNT(*) FROM ajustes_datos WHERE nombre = ?", Integer.class, AJUSTE_HORARIOS);
            if (hecho != null && hecho > 0) return;
            int filas = jdbc.update(sqlAjusteHorarios());
            jdbc.update("INSERT INTO ajustes_datos (nombre) VALUES (?)", AJUSTE_HORARIOS);
            if (filas > 0) log.info("Horarios: {} bloques pasados a las horas reales del colegio.", filas);
        } catch (Exception e) {
            log.warn("Ajuste de horarios omitido: {}", e.getMessage());
        }
    }

    static String sqlAjusteHorarios() {
        StringBuilder antiguas = new StringBuilder(), caso = new StringBuilder();
        for (String[] h : HORAS) {
            if (antiguas.length() > 0) antiguas.append(", ");
            antiguas.append('\'').append(h[0]).append('\'');
            caso.append(" WHEN '").append(h[0]).append("' THEN TIME '").append(h[1]).append('\'');
        }
        String enAntiguas = "to_char(%s, 'HH24:MI') IN (" + antiguas + ")";
        String nueva = "CASE to_char(%s, 'HH24:MI')" + caso + " END";
        return "UPDATE horario_bloques SET hora_inicio = " + nueva.formatted("hora_inicio")
                + ", hora_fin = " + nueva.formatted("hora_fin")
                + " WHERE seccion_id IN (SELECT seccion_id FROM horario_bloques GROUP BY seccion_id HAVING bool_and("
                + enAntiguas.formatted("hora_inicio") + " AND " + enAntiguas.formatted("hora_fin") + "))";
    }
}
