package com.utp.smartlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaRequestDTO {

    @NotNull(message = "El equipo es obligatorio")
    private Long equipoId;

    @NotBlank(message = "La descripción de la incidencia es obligatoria")
    private String descripcion;

    private String evidenciaUrl;
}
