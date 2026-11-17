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
public class MantenimientoMetricasDTO {

    private long total;
    private long cerrados;
    private long tardios;
    private double tasaTardio;
}
