package com.utp.smartlab.dto;

import com.utp.smartlab.entity.TipoMantenimiento;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoRequestDTO {

    @NotNull(message = "El equipo es obligatorio")
    private Long equipoId;

    @NotNull(message = "El tipo de mantenimiento es obligatorio")
    private TipoMantenimiento tipo;

    @NotNull(message = "La fecha programada es obligatoria")
    private LocalDateTime fechaProgramada;
}
