package com.utp.smartlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilizacionDashboardDTO {

    private int umbralSobreutilizacionHoras;
    private long totalEquipos;
    private long subutilizados;
    private long normales;
    private long sobreutilizados;
    private List<UtilizacionEquipoResumenDTO> equipos;
}
