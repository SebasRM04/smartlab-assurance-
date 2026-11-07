package com.utp.smartlab.service;

import com.utp.smartlab.dto.MantenimientoMetricasDTO;
import com.utp.smartlab.dto.MantenimientoRequestDTO;
import com.utp.smartlab.dto.MantenimientoResponseDTO;
import com.utp.smartlab.entity.*;
import com.utp.smartlab.exception.MantenimientoInvalidoException;
import com.utp.smartlab.repository.MantenimientoRepository;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MantenimientoServiceTest {

    @Mock
    private MantenimientoRepository mantenimientoRepository;

    @Mock
    private EquipoService equipoService;

    @Mock
    private UsuarioActualService usuarioActualService;

    @InjectMocks
    private MantenimientoService mantenimientoService;

    private Usuario tecnico;

    @BeforeEach
    void setUp() {
        tecnico = Usuario.builder()
                .id(3L)
                .nombre("Tomás Técnico")
                .email("tecnico@smartlab.com")
                .passwordHash("x")
                .rol(Rol.builder().id(2L).nombre(NombreRol.TECNICO).build())
                .build();
        lenient().when(usuarioActualService.obtener()).thenReturn(tecnico);
    }

    private Equipo equipoMock(Long id) {
        return Equipo.builder().id(id).nombre("Equipo de prueba")
                .estado(EstadoEquipo.DISPONIBLE).contadorUsoHoras(0).build();
    }

    @Test
    void programar_marcaElEquipoComoEnMantenimiento() {
        Equipo equipo = equipoMock(1L);
        MantenimientoRequestDTO dto = new MantenimientoRequestDTO(
                1L, TipoMantenimiento.PREVENTIVO, LocalDateTime.now().plusDays(1));

        when(equipoService.obtenerEntidadPorId(1L)).thenReturn(equipo);
        when(mantenimientoRepository.save(any(Mantenimiento.class))).thenAnswer(inv -> {
            Mantenimiento m = inv.getArgument(0);
            m.setId(30L);
            return m;
        });

        MantenimientoResponseDTO resultado = mantenimientoService.programar(dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoMantenimiento.PROGRAMADO);
        assertThat(resultado.getEquipoId()).isEqualTo(1L);
        assertThat(resultado.getUsuarioId()).isEqualTo(3L);
        verify(equipoService).actualizarEstado(eq(1L), eq(EstadoEquipo.EN_MANTENIMIENTO), eq(tecnico), contains("programado"));
    }

    @Test
    void cerrar_actualizaElEquipoADisponible() {
        Equipo equipo = equipoMock(1L);
        Mantenimiento mantenimiento = Mantenimiento.builder()
                .id(30L).equipo(equipo).usuario(tecnico)
                .tipo(TipoMantenimiento.PREVENTIVO)
                .fechaProgramada(LocalDateTime.now().minusHours(1))
                .estado(EstadoMantenimiento.PROGRAMADO)
                .build();

        when(mantenimientoRepository.findById(30L)).thenReturn(Optional.of(mantenimiento));
        when(mantenimientoRepository.save(any(Mantenimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        MantenimientoResponseDTO resultado = mantenimientoService.cerrar(30L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoMantenimiento.CERRADO);
        assertThat(resultado.getUsuarioId()).isEqualTo(3L);
        verify(equipoService).actualizarEstado(eq(1L), eq(EstadoEquipo.DISPONIBLE), eq(tecnico), eq("Mantenimiento cerrado"));
    }

    @Test
    void cerrar_lanzaExcepcion_cuandoYaEstaCerrado() {
        Equipo equipo = equipoMock(1L);
        Mantenimiento mantenimientoCerrado = Mantenimiento.builder()
                .id(31L).equipo(equipo).usuario(tecnico)
                .tipo(TipoMantenimiento.CORRECTIVO)
                .fechaProgramada(LocalDateTime.now().minusDays(2))
                .fechaEjecucion(LocalDateTime.now().minusDays(1))
                .estado(EstadoMantenimiento.CERRADO)
                .build();

        when(mantenimientoRepository.findById(31L)).thenReturn(Optional.of(mantenimientoCerrado));

        assertThatThrownBy(() -> mantenimientoService.cerrar(31L))
                .isInstanceOf(MantenimientoInvalidoException.class);

        verify(equipoService, never()).actualizarEstado(any(), any(), any(), any());
    }

    @Test
    void cerrar_marcaComoTardio_cuandoLaEjecucionEsPosteriorALaFechaProgramada() {
        Equipo equipo = equipoMock(1L);
        Mantenimiento mantenimiento = Mantenimiento.builder()
                .id(32L).equipo(equipo).usuario(tecnico)
                .tipo(TipoMantenimiento.PREVENTIVO)
                .fechaProgramada(LocalDateTime.now().minusDays(3))
                .estado(EstadoMantenimiento.PROGRAMADO)
                .build();

        when(mantenimientoRepository.findById(32L)).thenReturn(Optional.of(mantenimiento));
        when(mantenimientoRepository.save(any(Mantenimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        MantenimientoResponseDTO resultado = mantenimientoService.cerrar(32L);

        assertThat(resultado.isTardio()).isTrue();
    }

    @Test
    void obtenerMetricas_calculaLaTasaDeTardiosCorrectamente() {
        when(mantenimientoRepository.count()).thenReturn(10L);
        when(mantenimientoRepository.contarPorEstado(EstadoMantenimiento.CERRADO)).thenReturn(8L);
        when(mantenimientoRepository.contarTardios()).thenReturn(2L);

        MantenimientoMetricasDTO resultado = mantenimientoService.obtenerMetricas();

        assertThat(resultado.getTotal()).isEqualTo(10L);
        assertThat(resultado.getCerrados()).isEqualTo(8L);
        assertThat(resultado.getTardios()).isEqualTo(2L);
        assertThat(resultado.getTasaTardio()).isEqualTo(25.0);
    }

    @Test
    void cerrar_noLiberaElEquipo_cuandoTieneOtroMantenimientoProgramado() {
        Equipo equipo = equipoMock(1L);
        Mantenimiento mantenimiento = Mantenimiento.builder()
                .id(33L).equipo(equipo).usuario(tecnico)
                .tipo(TipoMantenimiento.PREVENTIVO)
                .fechaProgramada(LocalDateTime.now().minusHours(1))
                .estado(EstadoMantenimiento.PROGRAMADO)
                .build();

        when(mantenimientoRepository.findById(33L)).thenReturn(Optional.of(mantenimiento));
        when(mantenimientoRepository.save(any(Mantenimiento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mantenimientoRepository.contarProgramadosDelEquipoExcepto(1L, 33L)).thenReturn(1L);

        MantenimientoResponseDTO resultado = mantenimientoService.cerrar(33L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoMantenimiento.CERRADO);
        verify(equipoService, never()).actualizarEstado(any(), any(), any(), any());
    }
}
