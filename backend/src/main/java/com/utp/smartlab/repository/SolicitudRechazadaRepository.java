package com.utp.smartlab.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.utp.smartlab.entity.SolicitudRechazada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRechazadaRepository extends JpaRepository<SolicitudRechazada, Long> {

    @Query("SELECT COUNT(s) FROM SolicitudRechazada s WHERE s.equipo.id = :equipoId")
    long contarPorEquipoId(@Param("equipoId") Long equipoId);
}
