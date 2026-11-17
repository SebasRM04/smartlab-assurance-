package com.utp.smartlab.dto;

import com.utp.smartlab.entity.EstadoMantenimiento;
import com.utp.smartlab.entity.TipoMantenimiento;
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
public class MantenimientoResponseDTO {

    private Long id;
    private Long equipoId;
    private Long usuarioId;
    private String usuarioNombre;
    private TipoMantenimiento tipo;
    private LocalDateTime fechaProgramada;
    private LocalDateTime fechaEjecucion;
    private EstadoMantenimiento estado;

    private boolean tardio;
}
