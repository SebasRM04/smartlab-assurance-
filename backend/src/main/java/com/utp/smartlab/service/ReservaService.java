package com.utp.smartlab.service;

import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.dto.ReservaResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.Reserva;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.EquipoNoDisponibleException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private UsuarioActualService usuarioActualService;

    @Autowired
    private SolicitudRechazadaService solicitudRechazadaService;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<ReservaResponseDTO> listarReservas() {
        return reservaRepository.findAll().stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    public ReservaResponseDTO obtenerPorId(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con ID: " + id));
        return mapearADTO(reserva);
    }

    public ReservaResponseDTO crearReserva(ReservaRequestDTO dto) {
        Usuario usuario = usuarioActualService.obtenerUsuarioActual();
        Equipo equipo = equipoRepository.findById(dto.getEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado con ID: " + dto.getEquipoId()));

        // Validación básica de disponibilidad por solapamiento de fechas
        boolean ocupado = reservaRepository.existsByEquipoIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                equipo.getId(), dto.getFechaFin(), dto.getFechaInicio());

        if (ocupado) {
            solicitudRechazadaService.registrarRechazo("RESERVA", "Equipo no disponible en el rango de fechas seleccionado", usuario.getId());
            throw new EquipoNoDisponibleException("El equipo ya se encuentra reservado en el horario indicado.");
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setEquipo(equipo);
        reserva.setFechaInicio(dto.getFechaInicio());
        reserva.setFechaFin(dto.getFechaFin());
        reserva.setEstado("PENDIENTE");

        Reserva guardada = reservaRepository.save(reserva);
        auditoriaService.registrarAccion("CREAR_RESERVA", "Se creó la reserva ID: " + guardada.getId());
        return mapearADTO(guardada);
    }

    public ReservaResponseDTO actualizarEstadoReserva(Long id, String nuevoEstado) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con ID: " + id));
        reserva.setEstado(nuevoEstado);
        Reserva actualizada = reservaRepository.save(reserva);
        auditoriaService.registrarAccion("ACTUALIZAR_RESERVA", "Se actualizó el estado de la reserva ID: " + id + " a " + nuevoEstado);
        return mapearADTO(actualizada);
    }

    private ReservaResponseDTO mapearADTO(Reserva r) {
        ReservaResponseDTO dto = new ReservaResponseDTO();
        dto.setId(r.getId());
        dto.setUsuarioId(r.getUsuario() != null ? r.getUsuario().getId() : null);
        dto.setUsuarioNombre(r.getUsuario() != null ? r.getUsuario().getNombre() + " " + r.getUsuario().getApellido() : null);
        dto.setEquipoId(r.getEquipo() != null ? r.getEquipo().getId() : null);
        dto.setEquipoNombre(r.getEquipo() != null ? r.getEquipo().getNombre() : null);
        dto.setFechaInicio(r.getFechaInicio());
        dto.setFechaFin(r.getFechaFin());
        dto.setEstado(r.getEstado());
        return dto;
    }
}