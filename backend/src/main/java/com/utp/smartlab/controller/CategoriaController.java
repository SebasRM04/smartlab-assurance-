package com.utp.smartlab.controller;

import com.utp.smartlab.dto.CategoriaRequestDTO;
import com.utp.smartlab.dto.CategoriaResponseDTO;
import com.utp.smartlab.service.AuditoriaService;
import com.utp.smartlab.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final AuditoriaService auditoriaService;

    public CategoriaController(CategoriaService categoriaService, AuditoriaService auditoriaService) {
        this.categoriaService = categoriaService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoriaResponseDTO> crear(@Valid @RequestBody CategoriaRequestDTO dto) {
        CategoriaResponseDTO creada = categoriaService.crear(dto);
        auditoriaService.registrar("CREAR_CATEGORIA", "Categoria#" + creada.getId(),
                "Categoría creada: " + creada.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<CategoriaResponseDTO>> listar() {
        return ResponseEntity.ok(categoriaService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    public ResponseEntity<CategoriaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoriaResponseDTO> actualizar(@PathVariable Long id,
                                                            @Valid @RequestBody CategoriaRequestDTO dto) {
        CategoriaResponseDTO actualizada = categoriaService.actualizar(id, dto);
        auditoriaService.registrar("ACTUALIZAR_CATEGORIA", "Categoria#" + id,
                "Categoría actualizada: " + actualizada.getNombre());
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        CategoriaResponseDTO categoria = categoriaService.obtenerPorId(id);
        categoriaService.eliminar(id);
        auditoriaService.registrar("ELIMINAR_CATEGORIA", "Categoria#" + id,
                "Categoría eliminada: " + categoria.getNombre());
        return ResponseEntity.noContent().build();
    }
}
