package com.utp.smartlab.dto;

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
public class ReservaMetricasDTO {

    private long total;
    private long confirmadas;
    private long atendidas;
    private long noShow;
    private long canceladas;
    /** Confirmadas que se conservan aunque luego pasen a ATENDIDA o NO_SHOW. No incluye canceladas. */
    private long totalConfirmadas;
    private double tasaNoShow;
}
