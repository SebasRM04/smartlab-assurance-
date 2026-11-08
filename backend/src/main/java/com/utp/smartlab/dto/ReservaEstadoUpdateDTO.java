package com.utp.smartlab.dto;

import com.utp.smartlab.entity.EstadoReserva;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaEstadoUpdateDTO {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoReserva estado;
}
