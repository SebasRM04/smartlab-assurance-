package com.utp.smartlab.service;

import com.utp.smartlab.dto.PrestamoRequestDTO;
import com.utp.smartlab.dto.PrestamoResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.Prestamo;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.EquipoNoDisponibleException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.PrestamoRepository;
import com.utp.smartlab.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrestamoService {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudRechazadaService solicitudRechazadaService;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<PrestamoResponseDTO> listarPrestamos() {
        return prestamoRepository.findAll().stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    public PrestamoResponseDTO obtenerPorId(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado con ID: " + id));
        return mapearADTO(prestamo);
    }

    public PrestamoResponseDTO crearPrestamo(PrestamoRequestDTO dto) {
        Equipo equipo = equipoRepository.findById(dto.getEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado con ID: " + dto.getEquipoId()));
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + dto.getUsuarioId()));

        boolean enCurso = prestamoRepository.findAll().stream().anyMatch(p ->
            p.getEquipo() != null && p.getEquipo().getId().equals(equipo.getId()) && "ACTIVO".equals(p.getEstado())
        );

        if (enCurso) {
            solicitudRechazadaService.registrarRechazo("PRESTAMO", "El equipo ya se encuentra en préstamo activo", usuario.getId());
            throw new EquipoNoDisponibleException("El equipo ya cuenta con un préstamo activo en este momento.");
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setEquipo(equipo);
        prestamo.setUsuario(usuario);
        prestamo.setFechaPrestamo(dto.getFechaPrestamo() != null ? dto.getFechaPrestamo() : LocalDateTime.now());
        prestamo.setFechaDevolucionEstimada(dto.getFechaDevolucionEstimada());
        prestamo.setEstado("ACTIVO");

        Prestamo guardado = prestamoRepository.save(prestamo);
        auditoriaService.registrarAccion("CREAR_PRESTAMO", "Se generó el préstamo ID: " + guardado.getId());
        return mapearADTO(guardado);
    }

    public PrestamoResponseDTO registrarDevolucion(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado con ID: " + id));

        prestamo.setEstado("DEVUELTO");
        prestamo.setFechaDevolucionReal(LocalDateTime.now());

        Prestamo actualizado = prestamoRepository.save(prestamo);
        auditoriaService.registrarAccion("DEVOLUCION_PRESTAMO", "Se registró la devolución del préstamo ID: " + id);
        return mapearADTO(actualizado);
    }

    private PrestamoResponseDTO mapearADTO(Prestamo p) {
        PrestamoResponseDTO dto = new PrestamoResponseDTO();
        dto.setId(p.getId());
        dto.setEquipoId(p.getEquipo() != null ? p.getEquipo().getId() : null);
        dto.setEquipoNombre(p.getEquipo() != null ? p.getEquipo().getNombre() : null);
        dto.setUsuarioId(p.getUsuario() != null ? p.getUsuario().getId() : null);
        dto.setUsuarioNombre(p.getUsuario() != null ? p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos() : null);
        dto.setFechaPrestamo(p.getFechaPrestamo());
        dto.setFechaDevolucionEstimada(p.getFechaDevolucionEstimada());
        dto.setFechaDevolucionReal(p.getFechaDevolucionReal());
        dto.setEstado(p.getEstado());
        return dto;
    }
}