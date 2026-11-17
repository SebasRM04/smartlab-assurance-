package com.utp.smartlab.controller;

import com.utp.smartlab.dto.MantenimientoMetricasDTO;
import com.utp.smartlab.dto.MantenimientoRequestDTO;
import com.utp.smartlab.dto.MantenimientoResponseDTO;
import com.utp.smartlab.service.AuditoriaService;
import com.utp.smartlab.service.MantenimientoService;
import jakarta.validation.Valid;
import com.utp.smartlab.dto.PaginaDTO;
import com.utp.smartlab.entity.EstadoMantenimiento;
import com.utp.smartlab.entity.TipoMantenimiento;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;
    private final AuditoriaService auditoriaService;

    public MantenimientoController(MantenimientoService mantenimientoService, AuditoriaService auditoriaService) {
        this.mantenimientoService = mantenimientoService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<MantenimientoResponseDTO> programar(@Valid @RequestBody MantenimientoRequestDTO dto) {
        MantenimientoResponseDTO creado = mantenimientoService.programar(dto);
        auditoriaService.registrar("PROGRAMAR_MANTENIMIENTO", "Mantenimiento#" + creado.getId(),
                "Equipo #" + creado.getEquipoId() + ", tipo " + creado.getTipo()
                        + ", programado " + creado.getFechaProgramada());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('TECNICO', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<MantenimientoResponseDTO> cerrar(@PathVariable Long id) {
        MantenimientoResponseDTO cerrado = mantenimientoService.cerrar(id);
        auditoriaService.registrar(
                "CERRAR_MANTENIMIENTO",
                "Mantenimiento#" + cerrado.getId(),
                "Equipo #" + cerrado.getEquipoId() + ", tardío: " + (cerrado.isTardio() ? "sí" : "no"));
        return ResponseEntity.ok(cerrado);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<MantenimientoResponseDTO>> listar() {
        return ResponseEntity.ok(mantenimientoService.listar());
    }

    @GetMapping("/pagina")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<PaginaDTO<MantenimientoResponseDTO>> listarPagina(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            @RequestParam(required = false) EstadoMantenimiento estado,
            @RequestParam(required = false) TipoMantenimiento tipo,
            @RequestParam(required = false) Long equipoId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        return ResponseEntity.ok(mantenimientoService.listarPagina(pagina, tamanio, estado, tipo, equipoId, desde, hasta));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<MantenimientoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(mantenimientoService.obtenerPorId(id));
    }

    @GetMapping("/reportes/mantenimiento")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<MantenimientoMetricasDTO> obtenerMetricas() {
        return ResponseEntity.ok(mantenimientoService.obtenerMetricas());
    }
}
