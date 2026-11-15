package com.utp.smartlab.dto;

import com.utp.smartlab.entity.EstadoEquipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipoEstadoUpdateDTO(
        @NotNull EstadoEquipo estado,
        @NotBlank @Size(max = 255) String motivo) { }
