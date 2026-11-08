package com.utp.smartlab.dto;

import com.utp.smartlab.entity.EstadoEquipo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipoResponseDTO {

    private Long id;
    private String nombre;
    private Long categoriaId;
    private String categoriaNombre;
    private String ubicacion;
    private EstadoEquipo estado;
    private Integer contadorUsoHoras;
}
