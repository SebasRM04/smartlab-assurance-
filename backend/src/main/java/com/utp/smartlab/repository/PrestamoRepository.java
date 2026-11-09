package com.utp.smartlab.repository;

import com.utp.smartlab.entity.Prestamo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    @Query("SELECT p FROM Prestamo p WHERE p.reserva.id = :reservaId")
    Optional<Prestamo> buscarPorReservaId(@Param("reservaId") Long reservaId);

    @Query("""
           SELECT p FROM Prestamo p
           JOIN FETCH p.reserva r
           JOIN FETCH r.usuario
           WHERE r.equipo.id = :equipoId
           ORDER BY p.fechaEntrega ASC
           """)
    java.util.List<Prestamo> buscarPorEquipoId(@Param("equipoId") Long equipoId);

    @Query("SELECT COUNT(p) FROM Prestamo p WHERE p.estado = 'DEVUELTO' AND p.fechaDevolucionReal > p.fechaLimite")
    long contarConMora();

    @Query("SELECT COUNT(p) FROM Prestamo p WHERE p.estado = :estado")
    long contarPorEstado(@Param("estado") com.utp.smartlab.entity.EstadoPrestamo estado);

    // Préstamos vencidos: siguen ACTIVOS y su fecha límite ya pasó
    @Query("""
           SELECT COUNT(p) FROM Prestamo p
           WHERE p.estado = com.utp.smartlab.entity.EstadoPrestamo.ACTIVO
             AND p.fechaLimite < :ahora
           """)
    long contarVencidos(@Param("ahora") java.time.LocalDateTime ahora);

    @Query("""
           SELECT p FROM Prestamo p
           WHERE p.estado = com.utp.smartlab.entity.EstadoPrestamo.ACTIVO
             AND p.fechaLimite < :ahora
           ORDER BY p.fechaLimite ASC
           """)
    java.util.List<Prestamo> buscarVencidos(@Param("ahora") java.time.LocalDateTime ahora);

    @Query("""
           SELECT r.equipo.id, COUNT(p)
           FROM Prestamo p
           JOIN p.reserva r
           GROUP BY r.equipo.id
           """)
    java.util.List<Object[]> contarPorEquipo();

    // Listado por página, del préstamo más reciente al más antiguo
    @Query(value = """
           SELECT p FROM Prestamo p
           JOIN FETCH p.reserva
           WHERE p.estado IN :estados
             AND p.fechaEntrega >= :desde
             AND p.fechaEntrega <= :hasta
           ORDER BY p.fechaEntrega DESC, p.id DESC
           """,
           countQuery = """
           SELECT COUNT(p) FROM Prestamo p
           WHERE p.estado IN :estados
             AND p.fechaEntrega >= :desde
             AND p.fechaEntrega <= :hasta
           """)
    Page<Prestamo> buscarPagina(@Param("estados") Collection<com.utp.smartlab.entity.EstadoPrestamo> estados,
                                @Param("desde") java.time.LocalDateTime desde,
                                @Param("hasta") java.time.LocalDateTime hasta,
                                Pageable pageable);
}
