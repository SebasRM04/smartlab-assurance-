package com.utp.smartlab.service;

import com.utp.smartlab.dto.CategoriaRequestDTO;
import com.utp.smartlab.dto.CategoriaResponseDTO;
import com.utp.smartlab.entity.Categoria;
import com.utp.smartlab.exception.OperacionNoPermitidaException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.CategoriaRepository;
import com.utp.smartlab.repository.EquipoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private EquipoRepository equipoRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    void crear_recortaElNombreYLoGuarda() {
        when(categoriaRepository.existsByNombreIgnoreCase("Óptico")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria categoria = inv.getArgument(0);
            categoria.setId(4L);
            return categoria;
        });

        CategoriaResponseDTO resultado = categoriaService.crear(new CategoriaRequestDTO("  Óptico  "));

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Óptico");
        assertThat(resultado.getId()).isEqualTo(4L);
        assertThat(resultado.getNombre()).isEqualTo("Óptico");
    }

    @Test
    void crear_lanzaExcepcion_cuandoElNombreYaExiste() {
        when(categoriaRepository.existsByNombreIgnoreCase("Química")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crear(new CategoriaRequestDTO("Química")))
                .isInstanceOf(OperacionNoPermitidaException.class);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void actualizar_rechazaUnNombreQueYaUsaOtraCategoria() {
        Categoria actual = Categoria.builder().id(1L).nombre("Óptico").build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(actual));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Química", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.actualizar(1L, new CategoriaRequestDTO("Química")))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void eliminar_lanzaExcepcion_cuandoTieneEquipos() {
        Categoria actual = Categoria.builder().id(1L).nombre("Óptico").build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(actual));
        when(equipoRepository.contarPorCategoriaId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> categoriaService.eliminar(1L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("equipo");
        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void eliminar_borraLaCategoria_cuandoNoTieneEquipos() {
        Categoria actual = Categoria.builder().id(1L).nombre("Óptico").build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(actual));
        when(equipoRepository.contarPorCategoriaId(1L)).thenReturn(0L);

        categoriaService.eliminar(1L);

        verify(categoriaRepository).delete(actual);
    }

    @Test
    void obtenerPorId_lanzaExcepcion_cuandoNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.obtenerPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
