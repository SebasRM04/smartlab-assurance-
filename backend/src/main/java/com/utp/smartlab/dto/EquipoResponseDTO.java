package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class EquipoResponseDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String categoriaNombre;
    private String estadoEquipoNombre;
}