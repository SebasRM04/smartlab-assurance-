package com.utp.smartlab.dto;

import com.utp.smartlab.entity.EstadoEquipo;
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
public class HistorialEstadoResponseDTO {

    private EstadoEquipo estadoAnterior;
    private EstadoEquipo estadoNuevo;
    private Long usuarioId;
    private String usuarioNombre;
    private String motivo;
    private LocalDateTime fechaCambio;
}
