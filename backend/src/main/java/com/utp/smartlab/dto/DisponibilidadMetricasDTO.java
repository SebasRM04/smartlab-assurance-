package com.utp.smartlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadMetricasDTO {

    private long totalEquiposEnMantenimiento;
    private long equiposCorrectamenteBloqueados;
    private double exactitudDisponibilidad;
}
