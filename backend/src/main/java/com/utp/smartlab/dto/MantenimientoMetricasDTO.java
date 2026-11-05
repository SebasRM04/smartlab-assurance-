package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class MantenimientoMetricasDTO {
    private Long totalMantenimientos;
    private Long preventivos;
    private Long correctivos;
}