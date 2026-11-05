package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class EquipoRequestDTO {
    private String codigo;
    private String nombre;
    private String descripcion;
    private Long categoriaId;
    private Long estadoEquipoId;
}