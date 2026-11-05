package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class ReservaMetricasDTO {
    private Long totalReservas;
    private Long pendientes;
    private Long completadas;
}