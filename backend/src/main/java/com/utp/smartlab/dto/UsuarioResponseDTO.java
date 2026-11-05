package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class UsuarioResponseDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String rol;
    private Boolean activo;
}