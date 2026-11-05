package com.utp.smartlab.repository;

import com.utp.smartlab.entity.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    @Query("SELECT i FROM Incidencia i WHERE i.equipo.id = :equipoId ORDER BY i.fechaRegistro DESC")
    List<Incidencia> buscarPorEquipoId(@Param("equipoId") Long equipoId);

    @Query("SELECT COUNT(i) FROM Incidencia i WHERE i.equipo.id = :equipoId")
    long contarPorEquipoId(@Param("equipoId") Long equipoId);
}
