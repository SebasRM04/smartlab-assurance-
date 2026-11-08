package com.utp.smartlab.dto;

import com.utp.smartlab.entity.ClasificacionUtilizacion;
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
public class UtilizacionEquipoResumenDTO {

    private Long equipoId;
    private String nombre;
    private Integer horasUso;
    private long frecuenciaPrestamos;
    private ClasificacionUtilizacion clasificacion;
}
