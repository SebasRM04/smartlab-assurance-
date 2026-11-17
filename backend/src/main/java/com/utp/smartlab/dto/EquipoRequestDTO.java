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
public class EquipoRequestDTO {

    @NotBlank(message = "El nombre del equipo es obligatorio")
    private String nombre;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    private String ubicacion;
}
