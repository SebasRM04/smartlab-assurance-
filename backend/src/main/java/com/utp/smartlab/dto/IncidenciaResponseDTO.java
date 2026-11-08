package com.utp.smartlab.dto;

import com.utp.smartlab.entity.EstadoIncidencia;
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
public class IncidenciaResponseDTO {

    private Long id;
    private Long equipoId;
    private Long usuarioId;
    private String usuarioNombre;
    private String descripcion;
    private String evidenciaUrl;
    private EstadoIncidencia estado;
    private LocalDateTime fechaRegistro;
}
