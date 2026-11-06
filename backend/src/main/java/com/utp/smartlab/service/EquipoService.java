package com.utp.smartlab.service;

import com.utp.smartlab.dto.EquipoRecomendacionDTO;
import com.utp.smartlab.dto.EquipoRequestDTO;
import com.utp.smartlab.dto.EquipoResponseDTO;
import com.utp.smartlab.dto.HistorialEstadoResponseDTO;
import com.utp.smartlab.entity.*;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoService {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EstadoEquipoRepository estadoEquipoRepository;

    @Autowired
    private HistorialEstadoEquipoRepository historialRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<EquipoResponseDTO> listarEquipos() {
        return equipoRepository.findAll().stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    public EquipoResponseDTO obtenerPorId(Long id) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado con ID: " + id));
        return mapearADTO(equipo);
    }

    public EquipoResponseDTO crearEquipo(EquipoRequestDTO dto) {
        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + dto.getCategoriaId()));
        EstadoEquipo estado = estadoEquipoRepository.findById(dto.getEstadoEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado de equipo no encontrado con ID: " + dto.getEstadoEquipoId()));

        Equipo equipo = new Equipo();
        equipo.setCodigo(dto.getCodigo());
        equipo.setNombre(dto.getNombre());
        equipo.setdetalle(dto.getDescripcion());
        equipo.setCategoria(categoria);
        equipo.setEstadoEquipo(estado);

        Equipo guardado = equipoRepository.save(equipo);

        registrarHistorial(guardado, null, estado != null ? estado.getNombre() : "ESTADO", "Creación inicial del equipo");
        auditoriaService.registrarAccion("CREAR_EQUIPO", "Se creó el equipo: " + guardado.getCodigo());

        return mapearADTO(guardado);
    }

    public EquipoResponseDTO actualizarEquipo(Long id, EquipoRequestDTO dto) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado con ID: " + id));

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + dto.getCategoriaId()));
        EstadoEquipo nuevoEstado = estadoEquipoRepository.findById(dto.getEstadoEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado de equipo no encontrado con ID: " + dto.getEstadoEquipoId()));

        String estadoAnterior = equipo.getEstadoEquipo() != null ? equipo.getEstadoEquipo().getNombre() : "NINGUNO";
        String estadoNuevo = nuevoEstado != null ? nuevoEstado.getNombre() : "NUEVO";

        equipo.setCodigo(dto.getCodigo());
        equipo.setNombre(dto.getNombre());
        equipo.setdetalle(dto.getDescripcion());
        equipo.setCategoria(categoria);
        equipo.setEstadoEquipo(nuevoEstado);

        Equipo actualizado = equipoRepository.save(equipo);

        if (!estadoAnterior.equals(estadoNuevo)) {
            registrarHistorial(actualizado, estadoAnterior, estadoNuevo, "Actualización de estado de equipo");
        }

        auditoriaService.registrarAccion("ACTUALIZAR_EQUIPO", "Se actualizó el equipo ID: " + id);
        return mapearADTO(actualizado);
    }

    public List<HistorialEstadoResponseDTO> obtenerHistorialEquipo(Long equipoId) {
        if (!equipoRepository.existsById(equipoId)) {
            throw new RecursoNoEncontradoException("Equipo no encontrado con ID: " + equipoId);
        }
        return historialRepository.findAll().stream()
                .filter(h -> h.getEquipo() != null && h.getEquipo().getId().equals(equipoId))
                .map(h -> {
                    HistorialEstadoResponseDTO dto = new HistorialEstadoResponseDTO();
                    dto.setId(h.getId());
                    dto.setEquipoId(h.getEquipo().getId());
                    dto.setEstadoAnterior(h.getEstadoAnterior());
                    dto.setEstadoNuevo(h.getEstadoNuevo());
                    dto.setFechaCambio(h.getFechaCreacion());
                    dto.setMotivo(h.getMotivo());
                    return dto;
                }).collect(Collectors.toList());
    }

    public List<EquipoRecomendacionDTO> recomendarEquipos() {
        return equipoRepository.findAll().stream().limit(5).map(e -> {
            EquipoRecomendacionDTO dto = new EquipoRecomendacionDTO();
            dto.setId(e.getId());
            dto.setNombre(e.getNombre());
            dto.setCodigo(e.getCodigo());
            dto.setMotivo("Recomendado por disponibilidad general en el laboratorio.");
            return dto;
        }).collect(Collectors.toList());
    }

    private void registrarHistorial(Equipo equipo, String anterior, String nuevo, String motivo) {
        HistorialEstadoEquipo historial = new HistorialEstadoEquipo();
        historial.setEquipo(equipo);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setFechaCreacion(LocalDateTime.now());
        historial.setMotivo(motivo);
        historialRepository.save(historial);
    }

    private EquipoResponseDTO mapearADTO(Equipo e) {
        EquipoResponseDTO dto = new EquipoResponseDTO();
        dto.setId(e.getId());
        dto.setCodigo(e.getCodigo());
        dto.setNombre(e.getNombre());
        dto.setDescripcion(e.getdetalle());
        dto.setCategoriaNombre(e.getCategoria() != null ? e.getCategoria().getNombre() : null);
        dto.setEstadoEquipoNombre(e.getEstadoEquipo() != null ? e.getEstadoEquipo().getNombre() : null);
        return dto;
    }
}