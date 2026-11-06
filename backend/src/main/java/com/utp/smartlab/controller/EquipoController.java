package com.utp.smartlab.controller;

import com.utp.smartlab.dto.DisponibilidadMetricasDTO;
import com.utp.smartlab.dto.EquipoRecomendacionDTO;
import com.utp.smartlab.dto.EquipoRequestDTO;
import com.utp.smartlab.dto.EquipoResponseDTO;
import com.utp.smartlab.dto.HistorialEstadoResponseDTO;
import com.utp.smartlab.dto.UtilizacionDashboardDTO;
import com.utp.smartlab.dto.UtilizacionEquipoResponseDTO;
import com.utp.smartlab.entity.EstadoEquipo;
import com.utp.smartlab.service.AuditoriaService;
import com.utp.smartlab.service.EquipoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService equipoService;
    private final AuditoriaService auditoriaService;

    public EquipoController(EquipoService equipoService, AuditoriaService auditoriaService) {
        this.equipoService = equipoService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<EquipoResponseDTO> crear(@Valid @RequestBody EquipoRequestDTO dto) {
        EquipoResponseDTO creado = equipoService.crear(dto);
        auditoriaService.registrar("CREAR_EQUIPO", "Equipo#" + creado.getId(),
                "Equipo creado: " + creado.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<EquipoResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody EquipoRequestDTO dto) {
        EquipoResponseDTO actualizado = equipoService.actualizar(id, dto);
        auditoriaService.registrar("ACTUALIZAR_EQUIPO", "Equipo#" + id, "Datos actualizados: " + actualizado.getNombre());
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        EquipoResponseDTO equipo = equipoService.obtenerPorId(id);
        equipoService.eliminar(id);
        auditoriaService.registrar("ELIMINAR_EQUIPO", "Equipo#" + id, "Equipo eliminado: " + equipo.getNombre());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquipoResponseDTO>> listar() {
        return ResponseEntity.ok(equipoService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EquipoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(equipoService.obtenerPorId(id));
    }

    @GetMapping("/{id}/disponibilidad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, EstadoEquipo>> obtenerDisponibilidad(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("estado", equipoService.obtenerDisponibilidad(id)));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<HistorialEstadoResponseDTO>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(equipoService.obtenerHistorial(id));
    }

    @GetMapping("/{id}/utilizacion")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<UtilizacionEquipoResponseDTO>> obtenerHistorialUtilizacion(@PathVariable Long id) {
        return ResponseEntity.ok(equipoService.obtenerHistorialUtilizacion(id));
    }

    @GetMapping("/reportes/disponibilidad")
    @PreAuthorize("hasAnyRole('DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<DisponibilidadMetricasDTO> obtenerMetricasDisponibilidad() {
        return ResponseEntity.ok(equipoService.obtenerMetricasDisponibilidad());
    }

    @GetMapping("/reportes/utilizacion")
    @PreAuthorize("hasAnyRole('TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<UtilizacionDashboardDTO> obtenerDashboardUtilizacion(
            @RequestParam(defaultValue = "100") int umbralSobreutilizacionHoras) {
        return ResponseEntity.ok(equipoService.obtenerDashboardUtilizacion(umbralSobreutilizacionHoras));
    }

    @GetMapping("/recomendaciones-mantenimiento")
    @PreAuthorize("hasAnyRole('TECNICO', 'COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<EquipoRecomendacionDTO>> obtenerRecomendacionesMantenimiento(
            @RequestParam(defaultValue = "100") int umbralHoras) {
        return ResponseEntity.ok(equipoService.obtenerRecomendacionesMantenimiento(umbralHoras));
    }
}
