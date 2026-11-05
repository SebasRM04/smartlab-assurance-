package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    private String nombre;
    private String apellido;
    private String email;
    private Long rolId;
}