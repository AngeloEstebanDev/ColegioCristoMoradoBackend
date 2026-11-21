package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.ArchivoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de la entidad ArchivoAdjunto: es la "puerta" a la tabla de los archivos subidos al
 * sistema (PDF, Word, imágenes, etc.) en PostgreSQL. Spring Data JPA crea solo las consultas a partir
 * del nombre de cada método (por ejemplo findByDni busca por DNI), así que no hace falta escribir SQL.
 * Los métodos heredados (findAll, findById, save, delete...) vienen de JpaRepository.
 */
public interface ArchivoAdjuntoRepository extends JpaRepository<ArchivoAdjunto, Long> {
}
