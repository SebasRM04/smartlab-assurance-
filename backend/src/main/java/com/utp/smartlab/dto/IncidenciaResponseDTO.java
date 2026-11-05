package com.utp.smartlab.dto;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class IncidenciaResponseDTO {
    private Long id;
    private Long equipoId;
    private String equipoNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private String titulo;
    private String descripcion;
    private String estado;
    private LocalDateTime fechaRegistro;
}