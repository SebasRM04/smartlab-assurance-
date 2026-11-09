package com.utp.smartlab.repository;

import com.utp.smartlab.entity.EstadoMantenimiento;
import com.utp.smartlab.entity.Mantenimiento;
import com.utp.smartlab.entity.TipoMantenimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    @Query("SELECT m FROM Mantenimiento m WHERE m.equipo.id = :equipoId")
    List<Mantenimiento> buscarPorEquipoId(@Param("equipoId") Long equipoId);

    @Query("SELECT m FROM Mantenimiento m WHERE m.estado = :estado")
    List<Mantenimiento> buscarPorEstado(@Param("estado") EstadoMantenimiento estado);

    @Query("SELECT COUNT(m) FROM Mantenimiento m WHERE m.estado = 'CERRADO' AND m.fechaEjecucion > m.fechaProgramada")
    long contarTardios();

    @Query("SELECT COUNT(m) FROM Mantenimiento m WHERE m.estado = :estado")
    long contarPorEstado(@Param("estado") EstadoMantenimiento estado);

    // Otros mantenimientos todavía abiertos del mismo equipo (excluye el que se está cerrando)
    @Query("""
           SELECT COUNT(m) FROM Mantenimiento m
           WHERE m.equipo.id = :equipoId
             AND m.estado = com.utp.smartlab.entity.EstadoMantenimiento.PROGRAMADO
             AND m.id <> :mantenimientoId
           """)
    long contarProgramadosDelEquipoExcepto(@Param("equipoId") Long equipoId,
                                           @Param("mantenimientoId") Long mantenimientoId);

    @Query("SELECT COUNT(m) FROM Mantenimiento m WHERE m.equipo.id = :equipoId")
    long contarPorEquipoId(@Param("equipoId") Long equipoId);

    @Query("""
           SELECT m FROM Mantenimiento m
           JOIN FETCH m.equipo
           WHERE m.tipo = com.utp.smartlab.entity.TipoMantenimiento.PREVENTIVO
             AND m.estado = com.utp.smartlab.entity.EstadoMantenimiento.PROGRAMADO
             AND m.fechaProgramada <= :ahora
           ORDER BY m.fechaProgramada ASC
           """)
    List<Mantenimiento> buscarPreventivosProgramadosVencidos(@Param("ahora") LocalDateTime ahora);

    // Listados por página, del más reciente al más antiguo según la fecha programada
    @Query(value = """
           SELECT m FROM Mantenimiento m
           JOIN FETCH m.equipo
           JOIN FETCH m.usuario
           WHERE m.estado IN :estados AND m.tipo IN :tipos
             AND m.fechaProgramada >= :desde AND m.fechaProgramada <= :hasta
           ORDER BY m.fechaProgramada DESC, m.id DESC
           """,
           countQuery = """
           SELECT COUNT(m) FROM Mantenimiento m
           WHERE m.estado IN :estados AND m.tipo IN :tipos
             AND m.fechaProgramada >= :desde AND m.fechaProgramada <= :hasta
           """)
    Page<Mantenimiento> buscarPagina(@Param("estados") Collection<EstadoMantenimiento> estados,
                                     @Param("tipos") Collection<TipoMantenimiento> tipos,
                                     @Param("desde") LocalDateTime desde,
                                     @Param("hasta") LocalDateTime hasta,
                                     Pageable pageable);

    @Query(value = """
           SELECT m FROM Mantenimiento m
           JOIN FETCH m.equipo
           JOIN FETCH m.usuario
           WHERE m.equipo.id = :equipoId AND m.estado IN :estados AND m.tipo IN :tipos
             AND m.fechaProgramada >= :desde AND m.fechaProgramada <= :hasta
           ORDER BY m.fechaProgramada DESC, m.id DESC
           """,
           countQuery = """
           SELECT COUNT(m) FROM Mantenimiento m
           WHERE m.equipo.id = :equipoId AND m.estado IN :estados AND m.tipo IN :tipos
             AND m.fechaProgramada >= :desde AND m.fechaProgramada <= :hasta
           """)
    Page<Mantenimiento> buscarPaginaPorEquipo(@Param("equipoId") Long equipoId,
                                              @Param("estados") Collection<EstadoMantenimiento> estados,
                                              @Param("tipos") Collection<TipoMantenimiento> tipos,
                                              @Param("desde") LocalDateTime desde,
                                              @Param("hasta") LocalDateTime hasta,
                                              Pageable pageable);
}
