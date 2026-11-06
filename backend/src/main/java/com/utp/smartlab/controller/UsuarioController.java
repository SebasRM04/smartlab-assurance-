package com.utp.smartlab.controller;

import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.dto.UsuarioResponseDTO;
import com.utp.smartlab.dto.UsuarioUpdateDTO;
import com.utp.smartlab.service.AuditoriaService;
import com.utp.smartlab.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public UsuarioController(UsuarioService usuarioService, AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO creado = usuarioService.registrar(dto);
        auditoriaService.registrar("CREAR_USUARIO", "Usuario#" + creado.getId(),
                "Usuario " + creado.getEmail() + " creado con rol " + creado.getRol());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody UsuarioUpdateDTO dto,
                                                          Authentication authentication) {
        UsuarioResponseDTO actualizado = usuarioService.actualizar(id, dto, authentication.getName());
        auditoriaService.registrar("ACTUALIZAR_USUARIO", "Usuario#" + id,
                "Usuario " + actualizado.getEmail() + " actualizado (rol " + actualizado.getRol() + ")");
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        UsuarioResponseDTO objetivo = usuarioService.obtenerPorId(id);
        usuarioService.eliminar(id, authentication.getName());
        auditoriaService.registrar("ELIMINAR_USUARIO", "Usuario#" + id, "Usuario eliminado: " + objetivo.getEmail());
        return ResponseEntity.noContent().build();
    }
}
