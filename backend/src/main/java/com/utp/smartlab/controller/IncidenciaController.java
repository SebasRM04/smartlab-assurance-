package com.utp.smartlab.controller;

import com.utp.smartlab.dto.IncidenciaEstadoUpdateDTO;
import com.utp.smartlab.dto.IncidenciaRequestDTO;
import com.utp.smartlab.dto.IncidenciaResponseDTO;
import com.utp.smartlab.service.AuditoriaService;
import com.utp.smartlab.service.IncidenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;
    private final AuditoriaService auditoriaService;

    public IncidenciaController(IncidenciaService incidenciaService, AuditoriaService auditoriaService) {
        this.incidenciaService = incidenciaService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IncidenciaResponseDTO> registrar(@Valid @RequestBody IncidenciaRequestDTO dto) {
        IncidenciaResponseDTO creada = incidenciaService.registrar(dto);
        auditoriaService.registrar("REGISTRAR_INCIDENCIA", "Incidencia#" + creada.getId(),
                "Equipo #" + creada.getEquipoId() + ": " + creada.getDescripcion());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<IncidenciaResponseDTO>> listar() {
        return ResponseEntity.ok(incidenciaService.listar());
    }

    @GetMapping("/equipo/{equipoId}")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<IncidenciaResponseDTO>> listarPorEquipo(@PathVariable Long equipoId) {
        return ResponseEntity.ok(incidenciaService.listarPorEquipo(equipoId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<IncidenciaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(incidenciaService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('TECNICO', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<IncidenciaResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody IncidenciaEstadoUpdateDTO dto) {
        IncidenciaResponseDTO actualizada = incidenciaService.cambiarEstado(id, dto.getEstado());
        auditoriaService.registrar("CAMBIAR_ESTADO_INCIDENCIA", "Incidencia#" + id,
                "Nuevo estado: " + actualizada.getEstado());
        return ResponseEntity.ok(actualizada);
    }
}
