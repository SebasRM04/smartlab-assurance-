package com.utp.smartlab.dto;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PrestamoRequestDTO {
    private Long equipoId;
    private Long usuarioId;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucionEstimada;
}