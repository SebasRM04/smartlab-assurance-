package com.utp.smartlab.dto;

import com.utp.smartlab.entity.EstadoPrestamo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrestamoResponseDTO {

    private Long id;
    private Long reservaId;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaLimite;
    private LocalDateTime fechaDevolucionReal;
    private EstadoPrestamo estado;

    // Devuelto después de la fecha límite
    private boolean moroso;

    // Sigue ACTIVO y la fecha límite ya pasó (todavía no se devolvió)
    private boolean vencido;
}
