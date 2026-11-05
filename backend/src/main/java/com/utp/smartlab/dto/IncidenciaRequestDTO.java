package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class IncidenciaRequestDTO {
    private Long equipoId;
    private String titulo;
    private String descripcion;
}