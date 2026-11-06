package com.utp.smartlab.service;

import com.utp.smartlab.dto.MantenimientoRequestDTO;
import com.utp.smartlab.dto.MantenimientoResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.Mantenimiento;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.MantenimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MantenimientoService {

    @Autowired
    private MantenimientoRepository mantenimientoRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<MantenimientoResponseDTO> listarMantenimientos() {
        return mantenimientoRepository.findAll().stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    public MantenimientoResponseDTO registrarMantenimiento(MantenimientoRequestDTO dto) {
        Equipo equipo = equipoRepository.findById(dto.getEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado con ID: " + dto.getEquipoId()));

        Mantenimiento mantenimiento = new Mantenimiento();
        mantenimiento.setEquipo(equipo);
        mantenimiento.setTipo(dto.getTipoMantenimiento());
        mantenimiento.setDescripcion(dto.getDescripcion());
        mantenimiento.setFechaProgramada(dto.getFechaProgramada());
        mantenimiento.setEstado("PROGRAMADO");

        Mantenimiento guardado = mantenimientoRepository.save(mantenimiento);
        auditoriaService.registrarAccion("REGISTRAR_MANTENIMIENTO", "Se programó mantenimiento para equipo ID: " + equipo.getId());
        return mapearADTO(guardado);
    }

    private MantenimientoResponseDTO mapearADTO(Mantenimiento m) {
        MantenimientoResponseDTO dto = new MantenimientoResponseDTO();
        dto.setId(m.getId());
        dto.setEquipoId(m.getEquipo() != null ? m.getEquipo().getId() : null);
        dto.setEquipoNombre(m.getEquipo() != null ? m.getEquipo().getNombre() : null);
        dto.setTipoMantenimiento(m.getTipo());
        dto.setDescripcion(m.getDescripcion());
        dto.setFechaProgramada(m.getFechaProgramada());
        dto.setEstado(m.getEstado());
        return dto;
    }
}