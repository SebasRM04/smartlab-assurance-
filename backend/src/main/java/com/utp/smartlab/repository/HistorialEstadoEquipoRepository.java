package com.utp.smartlab.repository;

import org.springframework.data.jpa.repository.Modifying;
import com.utp.smartlab.entity.HistorialEstadoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialEstadoEquipoRepository extends JpaRepository<HistorialEstadoEquipo, Long> {

    @Query("""
           SELECT h FROM HistorialEstadoEquipo h
           LEFT JOIN FETCH h.usuario
           WHERE h.equipo.id = :equipoId
           ORDER BY h.fechaCambio ASC
           """)
    List<HistorialEstadoEquipo> buscarPorEquipoIdOrdenado(@Param("equipoId") Long equipoId);

    @Modifying
    @Query("DELETE FROM HistorialEstadoEquipo h WHERE h.equipo.id = :equipoId")
    void eliminarPorEquipoId(@Param("equipoId") Long equipoId);
}
