package com.utp.smartlab.dto;
import lombok.Data;

@Data
public class RegistroPublicoRequestDTO {
    private String nombre;
    private String apellido;
    private String email;
    private String password;
}