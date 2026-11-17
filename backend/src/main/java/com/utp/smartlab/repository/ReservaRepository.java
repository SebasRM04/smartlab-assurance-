package com.utp.smartlab.repository;

import com.utp.smartlab.entity.EstadoReserva;
import com.utp.smartlab.entity.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("""
           SELECT r FROM Reserva r
           WHERE r.equipo.id = :equipoId
             AND r.estado IN (com.utp.smartlab.entity.EstadoReserva.CONFIRMADA, com.utp.smartlab.entity.EstadoReserva.ATENDIDA)
             AND r.fechaInicio < :fechaFin
             AND r.fechaFin > :fechaInicio
           """)
    List<Reserva> buscarSolapadas(@Param("equipoId") Long equipoId,
                                   @Param("fechaInicio") LocalDateTime fechaInicio,
                                   @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.estado = :estado")
    long contarPorEstado(@Param("estado") EstadoReserva estado);

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.equipo.id = :equipoId")
    long contarPorEquipoId(@Param("equipoId") Long equipoId);

    @Query("SELECT r FROM Reserva r WHERE r.usuario.id = :usuarioId")
    List<Reserva> buscarPorUsuarioId(@Param("usuarioId") Long usuarioId);

    // Listados por página, del más reciente al más antiguo. El conteo va aparte porque la consulta trae las relaciones.
    @Query(value = """
           SELECT r FROM Reserva r
           JOIN FETCH r.equipo
           JOIN FETCH r.usuario
           WHERE r.estado IN :estados
             AND r.fechaInicio >= :desde AND r.fechaInicio <= :hasta
           ORDER BY r.fechaInicio DESC, r.id DESC
           """,
           countQuery = """
           SELECT COUNT(r) FROM Reserva r
           WHERE r.estado IN :estados AND r.fechaInicio >= :desde AND r.fechaInicio <= :hasta
           """)
    Page<Reserva> buscarPagina(@Param("estados") Collection<EstadoReserva> estados,
                               @Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta,
                               Pageable pageable);

    @Query(value = """
           SELECT r FROM Reserva r
           JOIN FETCH r.equipo
           JOIN FETCH r.usuario
           WHERE r.equipo.id = :equipoId AND r.estado IN :estados
             AND r.fechaInicio >= :desde AND r.fechaInicio <= :hasta
           ORDER BY r.fechaInicio DESC, r.id DESC
           """,
           countQuery = """
           SELECT COUNT(r) FROM Reserva r
           WHERE r.equipo.id = :equipoId AND r.estado IN :estados
             AND r.fechaInicio >= :desde AND r.fechaInicio <= :hasta
           """)
    Page<Reserva> buscarPaginaPorEquipo(@Param("equipoId") Long equipoId,
                                        @Param("estados") Collection<EstadoReserva> estados,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta,
                                        Pageable pageable);

    @Query(value = """
           SELECT r FROM Reserva r
           JOIN FETCH r.equipo
           JOIN FETCH r.usuario
           WHERE r.equipo.id = :equipoId AND r.usuario.id = :usuarioId AND r.estado IN :estados
             AND r.fechaInicio >= :desde AND r.fechaInicio <= :hasta
           ORDER BY r.fechaInicio DESC, r.id DESC
           """,
           countQuery = """
           SELECT COUNT(r) FROM Reserva r
           WHERE r.equipo.id = :equipoId AND r.usuario.id = :usuarioId AND r.estado IN :estados
             AND r.fechaInicio >= :desde AND r.fechaInicio <= :hasta
           """)
    Page<Reserva> buscarPaginaPorEquipoYUsuario(@Param("equipoId") Long equipoId,
                                                @Param("usuarioId") Long usuarioId,
                                                @Param("estados") Collection<EstadoReserva> estados,
                                                @Param("desde") LocalDateTime desde,
                                                @Param("hasta") LocalDateTime hasta,
                                                Pageable pageable);

    @Query(value = """
           SELECT r FROM Reserva r
           JOIN FETCH r.equipo
           JOIN FETCH r.usuario
           WHERE r.usuario.id = :usuarioId AND r.estado IN :estados
             AND r.fechaInicio >= :desde AND r.fechaInicio <= :hasta
           ORDER BY r.fechaInicio DESC, r.id DESC
           """,
           countQuery = """
           SELECT COUNT(r) FROM Reserva r
           WHERE r.usuario.id = :usuarioId AND r.estado IN :estados
             AND r.fechaInicio >= :desde AND r.fechaInicio <= :hasta
           """)
    Page<Reserva> buscarPaginaPorUsuario(@Param("usuarioId") Long usuarioId,
                                         @Param("estados") Collection<EstadoReserva> estados,
                                         @Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta,
                                         Pageable pageable);

    // Reservas CONFIRMADA que ya empezaron o empiezan antes de :hasta, de la más antigua a la más próxima.
    // Es la cola de trabajo del técnico para registrar entregas.
    @Query(value = """
           SELECT r FROM Reserva r
           JOIN FETCH r.equipo
           JOIN FETCH r.usuario
           WHERE r.estado = com.utp.smartlab.entity.EstadoReserva.CONFIRMADA
             AND r.fechaInicio <= :hasta
           ORDER BY r.fechaInicio ASC, r.id ASC
           """,
           countQuery = """
           SELECT COUNT(r) FROM Reserva r
           WHERE r.estado = com.utp.smartlab.entity.EstadoReserva.CONFIRMADA
             AND r.fechaInicio <= :hasta
           """)
    Page<Reserva> buscarPendientesDeEntrega(@Param("hasta") LocalDateTime hasta, Pageable pageable);
}
