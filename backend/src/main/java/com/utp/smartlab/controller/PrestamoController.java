package com.utp.smartlab.controller;

import com.utp.smartlab.dto.PrestamoCheckoutRequestDTO;
import com.utp.smartlab.dto.PrestamoMetricasDTO;
import com.utp.smartlab.dto.PrestamoResponseDTO;
import com.utp.smartlab.service.AuditoriaService;
import com.utp.smartlab.service.PrestamoService;
import jakarta.validation.Valid;
import com.utp.smartlab.dto.PaginaDTO;
import com.utp.smartlab.entity.EstadoPrestamo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;
    private final AuditoriaService auditoriaService;

    public PrestamoController(PrestamoService prestamoService, AuditoriaService auditoriaService) {
        this.prestamoService = prestamoService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('TECNICO', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<PrestamoResponseDTO> checkout(@Valid @RequestBody PrestamoCheckoutRequestDTO dto) {
        PrestamoResponseDTO creado = prestamoService.checkout(dto);
        auditoriaService.registrar("CHECKOUT_PRESTAMO", "Prestamo#" + creado.getId(),
                "Reserva #" + creado.getReservaId() + ", límite " + creado.getFechaLimite());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/{id}/checkin")
    @PreAuthorize("hasAnyRole('TECNICO', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<PrestamoResponseDTO> checkin(@PathVariable Long id) {
        PrestamoResponseDTO devuelto = prestamoService.checkin(id);
        auditoriaService.registrar("CHECKIN_PRESTAMO", "Prestamo#" + id,
                "Estado " + devuelto.getEstado() + ", moroso: " + (devuelto.isMoroso() ? "sí" : "no"));
        return ResponseEntity.ok(devuelto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<PrestamoResponseDTO>> listar() {
        return ResponseEntity.ok(prestamoService.listar());
    }

    @GetMapping("/pagina")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<PaginaDTO<PrestamoResponseDTO>> listarPagina(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            @RequestParam(required = false) EstadoPrestamo estado) {
        return ResponseEntity.ok(prestamoService.listarPagina(pagina, tamanio, estado));
    }

    @GetMapping("/vencidos")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<PrestamoResponseDTO>> listarVencidos() {
        return ResponseEntity.ok(prestamoService.listarVencidos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<PrestamoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.obtenerPorId(id));
    }

    @GetMapping("/reportes/mora")
    @PreAuthorize("hasAnyRole('DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<PrestamoMetricasDTO> obtenerMetricas() {
        return ResponseEntity.ok(prestamoService.obtenerMetricas());
    }
}
