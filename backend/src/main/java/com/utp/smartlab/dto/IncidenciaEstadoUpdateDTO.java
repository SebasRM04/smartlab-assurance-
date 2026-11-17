package com.utp.smartlab.dto;

import com.utp.smartlab.entity.EstadoIncidencia;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaEstadoUpdateDTO {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoIncidencia estado;
}
