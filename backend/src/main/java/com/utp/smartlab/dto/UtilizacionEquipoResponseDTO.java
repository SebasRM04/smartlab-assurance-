package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class UtilizacionEquipoResponseDTO {
    private Long equipoId;
    private String equipoNombre;
    private Double porcentajeUtilizacion;
}