package com.utp.smartlab.controller;

import com.utp.smartlab.dto.JwtResponseDTO;
import com.utp.smartlab.dto.LoginRequestDTO;
import com.utp.smartlab.dto.RegistroPublicoRequestDTO;
import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.dto.UsuarioResponseDTO;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.service.AuditoriaService;
import com.utp.smartlab.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public AuthController(UsuarioService usuarioService, AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroPublicoRequestDTO dto) {
        // Regla de negocio: el registro público siempre crea ESTUDIANTE
        UsuarioRequestDTO conRol = new UsuarioRequestDTO(
                dto.getNombre(), dto.getEmail(), dto.getPassword(), NombreRol.ESTUDIANTE);
        UsuarioResponseDTO creado = usuarioService.registrar(conRol);

        auditoriaService.registrarParaEmail(
                creado.getEmail(),
                "REGISTRO",
                "Usuario#" + creado.getId(),
                "Registro público con rol " + creado.getRol());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        JwtResponseDTO respuesta = usuarioService.login(dto);

        auditoriaService.registrarParaEmail(
                respuesta.getEmail(),
                "LOGIN",
                "Usuario#" + respuesta.getUsuarioId(),
                "Inicio de sesión exitoso con rol " + respuesta.getRol());
        return ResponseEntity.ok(respuesta);
    }
}
