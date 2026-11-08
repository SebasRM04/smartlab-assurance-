package com.utp.smartlab.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados. {@code pagina} empieza en 0.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginaDTO<T> {

    public static final int TAMANIO_MAXIMO = 100;

    private List<T> contenido;
    private int pagina;
    private int tamanio;
    private long totalElementos;
    private int totalPaginas;
    private boolean primera;
    private boolean ultima;

    public static <E, T> PaginaDTO<T> de(Page<E> resultado, Function<E, T> conversion) {
        List<T> contenido = resultado.getContent().stream().map(conversion).toList();
        return new PaginaDTO<>(contenido,
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.isFirst(),
                resultado.isLast());
    }

    /**
     * Arma la solicitud de página. La página mínima es 0 y el tamaño va de 1 a {@link #TAMANIO_MAXIMO},
     * para que un cliente no pida miles de filas de una vez. El orden lo fija cada consulta.
     */
    public static Pageable solicitud(int pagina, int tamanio) {
        int paginaValida = Math.max(pagina, 0);
        int tamanioValido = Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO);
        return PageRequest.of(paginaValida, tamanioValido);
    }
}
