package com.utp.smartlab.controller;

import com.utp.smartlab.dto.ConflictoMetricasDTO;
import com.utp.smartlab.dto.ReservaEstadoUpdateDTO;
import com.utp.smartlab.dto.ReservaMetricasDTO;
import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.dto.ReservaResponseDTO;
import com.utp.smartlab.service.AuditoriaService;
import com.utp.smartlab.service.ReservaService;
import jakarta.validation.Valid;
import com.utp.smartlab.dto.PaginaDTO;
import com.utp.smartlab.entity.EstadoReserva;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final AuditoriaService auditoriaService;

    public ReservaController(ReservaService reservaService, AuditoriaService auditoriaService) {
        this.reservaService = reservaService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ReservaResponseDTO> crear(@Valid @RequestBody ReservaRequestDTO dto) {
        ReservaResponseDTO creada = reservaService.crear(dto);
        auditoriaService.registrar(
                "CREAR_RESERVA",
                "Reserva#" + creada.getId(),
                "Equipo #" + creada.getEquipoId() + ", del " + creada.getFechaInicio() + " al " + creada.getFechaFin());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaResponseDTO>> listar() {
        return ResponseEntity.ok(reservaService.listar());
    }

    // Cualquier usuario autenticado lista (el estudiante solo ve las suyas). El filtro por usuario
    // (usuarioId) solo lo pueden usar técnico, coordinador y administrador: con otro rol responde 403.
    @GetMapping("/pagina")
    @PreAuthorize("isAuthenticated() and (#usuarioId == null or hasAnyRole('TECNICO', 'COORDINADOR', 'ADMINISTRADOR'))")
    public ResponseEntity<PaginaDTO<ReservaResponseDTO>> listarPagina(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) Long equipoId,
            @P("usuarioId") @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        return ResponseEntity.ok(reservaService.listarPagina(pagina, tamanio, estado, equipoId, usuarioId, desde, hasta));
    }

    @GetMapping("/pendientes-entrega")
    @PreAuthorize("hasAnyRole('TECNICO', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<ReservaResponseDTO>> listarPendientesDeEntrega(
            @RequestParam(defaultValue = "24") int horas) {
        return ResponseEntity.ok(reservaService.listarPendientesDeEntrega(horas));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DOCENTE', 'TECNICO', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ReservaResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ReservaEstadoUpdateDTO dto) {
        ReservaResponseDTO actualizada = reservaService.cambiarEstado(id, dto.getEstado());
        auditoriaService.registrar("CAMBIAR_ESTADO_RESERVA", "Reserva#" + id,
                "Nuevo estado: " + actualizada.getEstado());
        return ResponseEntity.ok(actualizada);
    }

    @GetMapping("/reportes/no-show")
    @PreAuthorize("hasAnyRole('DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ReservaMetricasDTO> obtenerMetricas() {
        return ResponseEntity.ok(reservaService.obtenerMetricas());
    }

    @GetMapping("/reportes/conflictos")
    @PreAuthorize("hasAnyRole('DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ConflictoMetricasDTO> obtenerMetricasConflicto() {
        return ResponseEntity.ok(reservaService.obtenerMetricasConflicto());
    }
}
