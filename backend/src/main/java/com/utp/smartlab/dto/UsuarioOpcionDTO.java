package com.utp.smartlab.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dato mínimo de un usuario para armar el selector del filtro de reservas: solo id y nombre,
 * sin correo ni rol (la lista completa de usuarios es solo del administrador).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioOpcionDTO {

    private Long id;
    private String nombre;
}
