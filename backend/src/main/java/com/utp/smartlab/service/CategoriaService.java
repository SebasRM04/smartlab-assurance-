package com.utp.smartlab.service;

import com.utp.smartlab.dto.CategoriaRequestDTO;
import com.utp.smartlab.dto.CategoriaResponseDTO;
import com.utp.smartlab.entity.Categoria;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<CategoriaResponseDTO> listarCategorias() {
        return categoriaRepository.findAll().stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    public CategoriaResponseDTO obtenerPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
        return mapearADTO(categoria);
    }

    public CategoriaResponseDTO crearCategoria(CategoriaRequestDTO dto) {
        Categoria categoria = new Categoria();
        categoria.setNombre(dto.getNombre());
        categoria.setdetalle(dto.getDescripcion());
        Categoria guardada = categoriaRepository.save(categoria);
        auditoriaService.registrarAccion("CREAR_CATEGORIA", "Se creó la categoría: " + guardada.getNombre());
        return mapearADTO(guardada);
    }

    public CategoriaResponseDTO actualizarCategoria(Long id, CategoriaRequestDTO dto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id));
        categoria.setNombre(dto.getNombre());
        categoria.setdetalle(dto.getDescripcion());
        Categoria actualizada = categoriaRepository.save(categoria);
        auditoriaService.registrarAccion("ACTUALIZAR_CATEGORIA", "Se actualizó la categoría ID: " + id);
        return mapearADTO(actualizada);
    }

    public void eliminarCategoria(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Categoría no encontrada con ID: " + id);
        }
        categoriaRepository.deleteById(id);
        auditoriaService.registrarAccion("ELIMINAR_CATEGORIA", "Se eliminó la categoría ID: " + id);
    }

    private CategoriaResponseDTO mapearADTO(Categoria c) {
        CategoriaResponseDTO dto = new CategoriaResponseDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setDescripcion(c.getdetalle());
        return dto;
    }
}