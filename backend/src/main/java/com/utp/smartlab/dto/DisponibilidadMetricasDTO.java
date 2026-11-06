package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class DisponibilidadMetricasDTO {
    private Long equiposDisponibles;
    private Long equiposEnUso;
    private Long equiposEnMantenimiento;
}