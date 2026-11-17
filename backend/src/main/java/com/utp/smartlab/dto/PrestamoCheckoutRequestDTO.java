package com.utp.smartlab.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoCheckoutRequestDTO {

    @NotNull(message = "La reserva es obligatoria")
    private Long reservaId;

    @NotNull(message = "La fecha límite de devolución es obligatoria")
    @Future(message = "La fecha límite debe ser futura")
    private LocalDateTime fechaLimite;
}
