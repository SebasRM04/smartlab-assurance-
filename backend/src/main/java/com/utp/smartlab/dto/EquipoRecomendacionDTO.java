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
public class EquipoRecomendacionDTO {

    private Long equipoId;
    private String nombre;
    private Integer contadorUsoHoras;
    private LocalDateTime fechaProgramada;
    private String motivo;
}
