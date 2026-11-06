package com.utp.smartlab.dto;
import java.time.LocalDate;
import lombok.Data;

@Data
public class MantenimientoRequestDTO {
    private Long equipoId;
    private String tipoMantenimiento;
    private String descripcion;
    private LocalDate fechaProgramada;
}