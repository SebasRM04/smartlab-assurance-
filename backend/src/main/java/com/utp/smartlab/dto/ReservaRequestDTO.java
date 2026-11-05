package com.utp.smartlab.dto;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReservaRequestDTO {
    private Long equipoId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
}