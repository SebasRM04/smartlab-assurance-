package com.utp.smartlab.dto;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PrestamoResponseDTO {
    private Long id;
    private Long equipoId;
    private String equipoNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucionEstimada;
    private LocalDateTime fechaDevolucionReal;
    private String estado;
}