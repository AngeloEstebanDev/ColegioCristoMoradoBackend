package com.CristoMorado_api.CristoMorado_api.repository;

import com.CristoMorado_api.CristoMorado_api.entity.ActividadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActividadLogRepository extends JpaRepository<ActividadLog, Long> {

    List<ActividadLog> findTop20ByOrderByFechaDescIdDesc();

    @Modifying
    @Query("DELETE FROM ActividadLog a WHERE a.fecha < :limite")
    int borrarAnterioresA(@Param("limite") LocalDateTime limite);
}
