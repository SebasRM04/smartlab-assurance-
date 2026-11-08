package com.utp.smartlab.service;

import com.utp.smartlab.dto.IncidenciaRequestDTO;
import com.utp.smartlab.dto.IncidenciaResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.EstadoIncidencia;
import com.utp.smartlab.entity.Incidencia;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.IncidenciaInvalidaException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.IncidenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final EquipoService equipoService;
    private final UsuarioActualService usuarioActualService;

    public IncidenciaService(IncidenciaRepository incidenciaRepository,
                             EquipoService equipoService,
                             UsuarioActualService usuarioActualService) {
        this.incidenciaRepository = incidenciaRepository;
        this.equipoService = equipoService;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional
    public IncidenciaResponseDTO registrar(IncidenciaRequestDTO dto) {
        return registrar(dto, usuarioActualService.obtener());
    }

    @Transactional
    public IncidenciaResponseDTO registrar(IncidenciaRequestDTO dto, Usuario usuario) {
        Equipo equipo = equipoService.obtenerEntidadPorId(dto.getEquipoId());

        Incidencia incidencia = Incidencia.builder()
                .equipo(equipo)
                .usuario(usuario)
                .descripcion(dto.getDescripcion())
                .evidenciaUrl(dto.getEvidenciaUrl())
                .estado(EstadoIncidencia.ABIERTA)
                .fechaRegistro(LocalDateTime.now())
                .build();

        return mapearAResponseDTO(incidenciaRepository.save(incidencia));
    }

    public List<IncidenciaResponseDTO> listar() {
        return incidenciaRepository.findAll().stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    public List<IncidenciaResponseDTO> listarPorEquipo(Long equipoId) {
        equipoService.obtenerEntidadPorId(equipoId);
        return incidenciaRepository.buscarPorEquipoId(equipoId).stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    public IncidenciaResponseDTO obtenerPorId(Long id) {
        return mapearAResponseDTO(buscarEntidadPorId(id));
    }

    @Transactional
    public IncidenciaResponseDTO cambiarEstado(Long id, EstadoIncidencia nuevoEstado) {
        Incidencia incidencia = buscarEntidadPorId(id);
        // Regla de negocio: una incidencia RESUELTA no cambia de estado
        if (incidencia.getEstado() == EstadoIncidencia.RESUELTA) {
            throw new IncidenciaInvalidaException("La incidencia " + id + " ya está resuelta y no admite más cambios de estado");
        }

        incidencia.setEstado(nuevoEstado);
        return mapearAResponseDTO(incidenciaRepository.save(incidencia));
    }

    private Incidencia buscarEntidadPorId(Long id) {
        return incidenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la incidencia con id " + id));
    }

    private IncidenciaResponseDTO mapearAResponseDTO(Incidencia incidencia) {
        return IncidenciaResponseDTO.builder()
                .id(incidencia.getId())
                .equipoId(incidencia.getEquipo().getId())
                .usuarioId(incidencia.getUsuario().getId())
                .usuarioNombre(incidencia.getUsuario().getNombre())
                .descripcion(incidencia.getDescripcion())
                .evidenciaUrl(incidencia.getEvidenciaUrl())
                .estado(incidencia.getEstado())
                .fechaRegistro(incidencia.getFechaRegistro())
                .build();
    }
}
