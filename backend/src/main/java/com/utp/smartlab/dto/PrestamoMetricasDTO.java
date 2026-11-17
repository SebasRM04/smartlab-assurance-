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
public class PrestamoMetricasDTO {

    private long total;
    private long activos;
    private long devueltos;
    private long conMora;
    private long vencidos;
    private double tasaMora;
}
