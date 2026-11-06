package com.utp.smartlab.service;

import com.utp.smartlab.dto.IncidenciaRequestDTO;
import com.utp.smartlab.dto.IncidenciaResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.Incidencia;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.IncidenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidenciaService {

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private UsuarioActualService usuarioActualService;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<IncidenciaResponseDTO> listarIncidencias() {
        return incidenciaRepository.findAll().stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    public IncidenciaResponseDTO registrarIncidencia(IncidenciaRequestDTO dto) {
        Usuario usuario = usuarioActualService.obtenerUsuarioActual();
        Equipo equipo = equipoRepository.findById(dto.getEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado con ID: " + dto.getEquipoId()));

        Incidencia incidencia = new Incidencia();
        incidencia.setEquipo(equipo);
        incidencia.setUsuario(usuario);
        incidencia.setTitulo(dto.getTitulo());
        incidencia.setDescripcion(dto.getDescripcion());
        incidencia.setEstado("ABIERTA");
        incidencia.setFechaCreacion(LocalDateTime.now());

        Incidencia guardada = incidenciaRepository.save(incidencia);
        auditoriaService.registrarAccion("REGISTRAR_INCIDENCIA", "Se registró incidencia para equipo ID: " + equipo.getId());
        return mapearADTO(guardada);
    }

    private IncidenciaResponseDTO mapearADTO(Incidencia i) {
        IncidenciaResponseDTO dto = new IncidenciaResponseDTO();
        dto.setId(i.getId());
        dto.setEquipoId(i.getEquipo() != null ? i.getEquipo().getId() : null);
        dto.setEquipoNombre(i.getEquipo() != null ? i.getEquipo().getNombre() : null);
        dto.setUsuarioId(i.getUsuario() != null ? i.getUsuario().getId() : null);
        dto.setUsuarioNombre(i.getUsuario() != null ? i.getUsuario().getNombres() + " " + i.getUsuario().getApellidos() : null);
        dto.setTitulo(i.getTitulo());
        dto.setDescripcion(i.getDescripcion());
        dto.setEstado(i.getEstado());
        dto.setFechaRegistro(i.getFechaCreacion());
        return dto;
    }
}