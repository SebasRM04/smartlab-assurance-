package com.utp.smartlab.dto;

import com.utp.smartlab.entity.EstadoPrestamo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Un uso real del equipo: el préstamo asociado a una reserva, con sus horas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilizacionEquipoResponseDTO {

    private Long prestamoId;
    private Long reservaId;
    private String usuarioNombre;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaLimite;
    private LocalDateTime fechaDevolucionReal;
    private Long horasUso;
    private EstadoPrestamo estado;
}
