package com.utp.smartlab.dto;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HistorialEstadoResponseDTO {
    private Long id;
    private Long equipoId;
    private String estadoAnterior;
    private String estadoNuevo;
    private LocalDateTime fechaCambio;
    private String motivo;
}