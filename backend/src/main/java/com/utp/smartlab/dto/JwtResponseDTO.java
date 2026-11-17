package com.utp.smartlab.dto;

import com.utp.smartlab.entity.NombreRol;
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
public class JwtResponseDTO {

    private String token;
    private String tipo;
    private Long usuarioId;
    private String nombre;
    private String email;
    private NombreRol rol;
}
