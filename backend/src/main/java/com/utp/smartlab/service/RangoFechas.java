package com.utp.smartlab.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Rango inclusivo de un filtro por fecha. Si un extremo no viene, se usa un limite amplio. */
public final class RangoFechas {

    public static final LocalDateTime MINIMO = LocalDateTime.of(1970, 1, 1, 0, 0);
    public static final LocalDateTime MAXIMO = LocalDateTime.of(2999, 12, 31, 23, 59, 59);

    private RangoFechas() {
    }

    public static void validar(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
    }

    public static LocalDateTime inicio(LocalDate desde) {
        return desde == null ? MINIMO : desde.atStartOfDay();
    }

    public static LocalDateTime fin(LocalDate hasta) {
        return hasta == null ? MAXIMO : hasta.atTime(23, 59, 59);
    }
}
