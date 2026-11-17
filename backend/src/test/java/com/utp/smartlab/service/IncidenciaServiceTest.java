package com.utp.smartlab.service;

import com.utp.smartlab.dto.IncidenciaRequestDTO;
import com.utp.smartlab.dto.IncidenciaResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.EstadoEquipo;
import com.utp.smartlab.entity.EstadoIncidencia;
import com.utp.smartlab.entity.Incidencia;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.Rol;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.IncidenciaInvalidaException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.IncidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private EquipoService equipoService;

    @Mock
    private UsuarioActualService usuarioActualService;

    @InjectMocks
    private IncidenciaService incidenciaService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Carla Estudiante")
                .email("estudiante@smartlab.com")
                .passwordHash("x")
                .rol(Rol.builder().id(1L).nombre(NombreRol.ESTUDIANTE).build())
                .build();
        lenient().when(usuarioActualService.obtener()).thenReturn(usuario);
    }

    private Equipo equipoMock(Long id) {
        return Equipo.builder().id(id).nombre("Equipo de prueba")
                .estado(EstadoEquipo.DISPONIBLE).contadorUsoHoras(0).build();
    }

    @Test
    void registrar_creaLaIncidencia_enEstadoAbierta() {
        Equipo equipo = equipoMock(1L);
        IncidenciaRequestDTO dto = new IncidenciaRequestDTO(1L, "El microscopio presenta ruido anormal", null);

        when(equipoService.obtenerEntidadPorId(1L)).thenReturn(equipo);
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> {
            Incidencia i = inv.getArgument(0);
            i.setId(200L);
            return i;
        });

        IncidenciaResponseDTO resultado = incidenciaService.registrar(dto);

        assertThat(resultado.getId()).isEqualTo(200L);
        assertThat(resultado.getEquipoId()).isEqualTo(1L);
        assertThat(resultado.getUsuarioId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoIncidencia.ABIERTA);
    }

    @Test
    void registrar_lanzaExcepcion_cuandoElEquipoNoExiste() {
        IncidenciaRequestDTO dto = new IncidenciaRequestDTO(99L, "Daño reportado", null);
        when(equipoService.obtenerEntidadPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el equipo con id 99"));

        assertThatThrownBy(() -> incidenciaService.registrar(dto))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cambiarEstado_actualizaCorrectamente_deAbiertaAEnRevision() {
        Equipo equipo = equipoMock(1L);
        Incidencia incidencia = Incidencia.builder()
                .id(10L).equipo(equipo).usuario(usuario).descripcion("Ruido anormal")
                .estado(EstadoIncidencia.ABIERTA).fechaRegistro(LocalDateTime.now())
                .build();

        when(incidenciaRepository.findById(10L)).thenReturn(Optional.of(incidencia));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(inv -> inv.getArgument(0));

        IncidenciaResponseDTO resultado = incidenciaService.cambiarEstado(10L, EstadoIncidencia.EN_REVISION);

        assertThat(resultado.getEstado()).isEqualTo(EstadoIncidencia.EN_REVISION);
        assertThat(resultado.getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void cambiarEstado_lanzaExcepcion_cuandoYaEstaResuelta() {
        Equipo equipo = equipoMock(1L);
        Incidencia incidenciaResuelta = Incidencia.builder()
                .id(11L).equipo(equipo).usuario(usuario).descripcion("Ya solucionado")
                .estado(EstadoIncidencia.RESUELTA).fechaRegistro(LocalDateTime.now().minusDays(1))
                .build();

        when(incidenciaRepository.findById(11L)).thenReturn(Optional.of(incidenciaResuelta));

        assertThatThrownBy(() -> incidenciaService.cambiarEstado(11L, EstadoIncidencia.EN_REVISION))
                .isInstanceOf(IncidenciaInvalidaException.class);
    }
}
