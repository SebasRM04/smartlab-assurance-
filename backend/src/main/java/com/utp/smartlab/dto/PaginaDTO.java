package com.utp.smartlab.dto;
import java.util.List;
import lombok.Data;

@Data
public class PaginaDTO<T> {
    private List<T> contenido;
    private int paginaActual;
    private int totalPaginas;
    private long totalElementos;
}