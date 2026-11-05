package com.utp.smartlab.dto;
import java.time.LocalDate;
import lombok.Data;

@Data
public class MantenimientoResponseDTO {
    private Long id;
    private Long equipoId;
    private String equipoNombre;
    private String tipoMantenimiento;
    private String descripcion;
    private LocalDate fechaProgramada;
    private String estado;
}