package com.utp.smartlab.service;

import com.utp.smartlab.dto.CategoriaRequestDTO;
import com.utp.smartlab.dto.CategoriaResponseDTO;
import com.utp.smartlab.entity.Categoria;
import com.utp.smartlab.exception.OperacionNoPermitidaException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.CategoriaRepository;
import com.utp.smartlab.repository.EquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final EquipoRepository equipoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, EquipoRepository equipoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.equipoRepository = equipoRepository;
    }

    @Transactional
    public CategoriaResponseDTO crear(CategoriaRequestDTO dto) {
        String nombre = dto.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new OperacionNoPermitidaException("Ya existe una categoría con el nombre '" + nombre + "'");
        }
        Categoria guardada = categoriaRepository.save(Categoria.builder().nombre(nombre).build());
        return mapear(guardada);
    }

    public List<CategoriaResponseDTO> listar() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
                .map(this::mapear)
                .toList();
    }

    public CategoriaResponseDTO obtenerPorId(Long id) {
        return mapear(buscarEntidadPorId(id));
    }

    @Transactional
    public CategoriaResponseDTO actualizar(Long id, CategoriaRequestDTO dto) {
        Categoria categoria = buscarEntidadPorId(id);
        String nombre = dto.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new OperacionNoPermitidaException("Ya existe una categoría con el nombre '" + nombre + "'");
        }
        categoria.setNombre(nombre);
        return mapear(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarEntidadPorId(id);
        // Regla de negocio: no se elimina una categoría que ya tiene equipos
        long equipos = equipoRepository.contarPorCategoriaId(id);
        if (equipos > 0) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar la categoría '" + categoria.getNombre() + "': tiene "
                            + equipos + " equipo(s) asociado(s)");
        }
        categoriaRepository.delete(categoria);
    }

    public Categoria buscarEntidadPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la categoría con id " + id));
    }

    private CategoriaResponseDTO mapear(Categoria categoria) {
        return CategoriaResponseDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .build();
    }
}
