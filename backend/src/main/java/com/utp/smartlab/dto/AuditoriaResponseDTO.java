package com.utp.smartlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaResponseDTO {

    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String accion;
    private String entidadAfectada;
    private LocalDateTime fechaHora;
    private String detalle;
}
