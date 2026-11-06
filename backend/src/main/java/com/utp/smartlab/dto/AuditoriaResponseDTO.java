package com.utp.smartlab.dto;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuditoriaResponseDTO {
    private Long id;
    private String usuarioEmail;
    private String accion;
    private String detalles;
    private LocalDateTime fecha;
}