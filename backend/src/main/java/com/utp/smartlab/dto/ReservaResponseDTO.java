package com.utp.smartlab.dto;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReservaResponseDTO {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private Long equipoId;
    private String equipoNombre;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String estado;
}