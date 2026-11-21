package com.CristoMorado_api.CristoMorado_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del backend. Al ejecutarla, Spring Boot arranca el servidor web (puerto 8080), se
 * conecta a PostgreSQL y deja listos todos los controladores, servicios y repositorios.
 */
@SpringBootApplication
public class CristoMoradoApiApplication {

    /**
     * Punto de entrada:.
     */
    public static void main(String[] args) {
        SpringApplication.run(CristoMoradoApiApplication.class, args);
    }
}