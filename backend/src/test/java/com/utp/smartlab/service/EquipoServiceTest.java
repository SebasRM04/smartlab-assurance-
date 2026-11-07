package com.utp.smartlab.service;

import com.utp.smartlab.dto.DisponibilidadMetricasDTO;
import com.utp.smartlab.dto.EquipoRecomendacionDTO;
import com.utp.smartlab.dto.HistorialEstadoResponseDTO;
import com.utp.smartlab.entity.*;
import com.utp.smartlab.exception.EquipoNoDisponibleException;
import com.utp.smartlab.exception.OperacionNoPermitidaException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.dto.UtilizacionDashboardDTO;
import com.utp.smartlab.dto.UtilizacionEquipoResponseDTO;
import com.utp.smartlab.repository.CategoriaRepository;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.HistorialEstadoEquipoRepository;
import com.utp.smartlab.repository.IncidenciaRepository;
import com.utp.smartlab.repository.MantenimientoRepository;
import com.utp.smartlab.repository.PrestamoRepository;
import com.utp.smartlab.repository.ReservaRepository;
import com.utp.smartlab.repository.SolicitudRechazadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private EquipoRepository equipoRepository;

    @Mock
    private HistorialEstadoEquipoRepository historialRepository;

    @Mock
    private MantenimientoRepository mantenimientoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private SolicitudRechazadaRepository solicitudRechazadaRepository;

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private UsuarioActualService usuarioActualService;

    @InjectMocks
    private EquipoService equipoService;

    private Equipo equipoDisponible;
    private Equipo equipoEnMantenimiento;

    @BeforeEach
    void setUp() {
        equipoDisponible = Equipo.builder()
                .id(1L).nombre("Microscopio A").estado(EstadoEquipo.DISPONIBLE).contadorUsoHoras(0).build();

        equipoEnMantenimiento = Equipo.builder()
                .id(2L).nombre("Osciloscopio B").estado(EstadoEquipo.EN_MANTENIMIENTO).contadorUsoHoras(0).build();
    }

    @Test
    void validarDisponibleParaReserva_noLanzaExcepcion_cuandoEquipoEstaDisponible() {
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoDisponible));

        Equipo resultado = equipoService.validarDisponibleParaReserva(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEquipo.DISPONIBLE);
    }

    @Test
    void validarDisponibleParaReserva_lanzaExcepcion_cuandoEquipoEstaEnMantenimiento() {
        when(equipoRepository.findById(2L)).thenReturn(Optional.of(equipoEnMantenimiento));

        assertThatThrownBy(() -> equipoService.validarDisponibleParaReserva(2L))
                .isInstanceOf(EquipoNoDisponibleException.class)
                .hasMessageContaining("no está disponible");
    }

    @Test
    void validarDisponibleParaReserva_lanzaExcepcion_cuandoEquipoNoExiste() {
        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipoService.validarDisponibleParaReserva(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crear_registraElEstadoInicialEnElHistorial() {
        when(equipoRepository.save(any(Equipo.class))).thenAnswer(inv -> {
            Equipo e = inv.getArgument(0);
            e.setId(5L);
            return e;
        });

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(
                Categoria.builder().id(1L).nombre("Analítica").build()));
        Usuario admin = Usuario.builder().id(1L).nombre("Administrador SmartLab").email("admin@smartlab.com").build();
        when(usuarioActualService.obtener()).thenReturn(admin);

        equipoService.crear(new com.utp.smartlab.dto.EquipoRequestDTO("Balanza", 1L, "Lab 1"));

        ArgumentCaptor<HistorialEstadoEquipo> captor = ArgumentCaptor.forClass(HistorialEstadoEquipo.class);
        verify(historialRepository).save(captor.capture());

        assertThat(captor.getValue().getEstadoAnterior()).isNull();
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoEquipo.DISPONIBLE);
        assertThat(captor.getValue().getUsuario().getNombre()).isEqualTo("Administrador SmartLab");
        assertThat(captor.getValue().getMotivo()).isEqualTo("Alta del equipo");
        assertThat(captor.getValue().getFechaCambio()).isNotNull();
    }

    @Test
    void actualizarEstado_registraEstadoAnteriorYNuevo_enElHistorial() {

        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoDisponible));
        Usuario tecnico = Usuario.builder().id(3L).nombre("Tomás Técnico").build();

        equipoService.actualizarEstado(1L, EstadoEquipo.EN_MANTENIMIENTO, tecnico, "Mantenimiento PREVENTIVO programado");

        ArgumentCaptor<HistorialEstadoEquipo> captor = ArgumentCaptor.forClass(HistorialEstadoEquipo.class);
        verify(historialRepository).save(captor.capture());

        assertThat(captor.getValue().getEstadoAnterior()).isEqualTo(EstadoEquipo.DISPONIBLE);
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoEquipo.EN_MANTENIMIENTO);
        assertThat(captor.getValue().getEquipo().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getUsuario().getId()).isEqualTo(3L);
        assertThat(captor.getValue().getMotivo()).isEqualTo("Mantenimiento PREVENTIVO programado");
        assertThat(captor.getValue().getFechaCambio()).isNotNull();
    }

    @Test
    void obtenerHistorial_conservaTodosLosCambiosAnteriores_noSoloElUltimo() {

        Equipo equipo = Equipo.builder().id(3L).nombre("PC Lab").estado(EstadoEquipo.DISPONIBLE).contadorUsoHoras(0).build();
        when(equipoRepository.findById(3L)).thenReturn(Optional.of(equipo));

        List<HistorialEstadoEquipo> historialAcumulado = List.of(
                HistorialEstadoEquipo.builder().equipo(equipo).estadoAnterior(null).estadoNuevo(EstadoEquipo.DISPONIBLE)
                        .fechaCambio(LocalDateTime.now().minusDays(2)).build(),
                HistorialEstadoEquipo.builder().equipo(equipo).estadoAnterior(EstadoEquipo.DISPONIBLE).estadoNuevo(EstadoEquipo.EN_MANTENIMIENTO)
                        .fechaCambio(LocalDateTime.now().minusDays(1)).build(),
                HistorialEstadoEquipo.builder().equipo(equipo).estadoAnterior(EstadoEquipo.EN_MANTENIMIENTO).estadoNuevo(EstadoEquipo.DISPONIBLE)
                        .fechaCambio(LocalDateTime.now()).build()
        );
        when(historialRepository.buscarPorEquipoIdOrdenado(3L)).thenReturn(historialAcumulado);

        List<HistorialEstadoResponseDTO> resultado = equipoService.obtenerHistorial(3L);

        assertThat(resultado).hasSize(3);
        assertThat(resultado.get(0).getEstadoAnterior()).isNull();
        assertThat(resultado.get(2).getEstadoNuevo()).isEqualTo(EstadoEquipo.DISPONIBLE);
    }

    @Test
    void obtenerHistorialUtilizacion_listaLosPrestamosDelEquipoConSusHoras() {
        Equipo equipo = Equipo.builder().id(8L).nombre("Taladro").estado(EstadoEquipo.DISPONIBLE).build();
        Usuario alumno = Usuario.builder().id(4L).nombre("Carla Estudiante").build();
        LocalDateTime entrega = LocalDateTime.now().minusHours(3);
        LocalDateTime devolucion = entrega.plusHours(2);
        Reserva reserva = Reserva.builder().id(20L).equipo(equipo).usuario(alumno)
                .fechaInicio(entrega).fechaFin(entrega.plusHours(2)).estado(EstadoReserva.ATENDIDA).build();
        Prestamo prestamo = Prestamo.builder().id(7L).reserva(reserva).fechaEntrega(entrega)
                .fechaLimite(entrega.plusHours(4)).fechaDevolucionReal(devolucion).estado(EstadoPrestamo.DEVUELTO).build();

        when(equipoRepository.findById(8L)).thenReturn(Optional.of(equipo));
        when(prestamoRepository.buscarPorEquipoId(8L)).thenReturn(List.of(prestamo));

        List<UtilizacionEquipoResponseDTO> resultado = equipoService.obtenerHistorialUtilizacion(8L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPrestamoId()).isEqualTo(7L);
        assertThat(resultado.get(0).getUsuarioNombre()).isEqualTo("Carla Estudiante");
        assertThat(resultado.get(0).getHorasUso()).isEqualTo(2L);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoPrestamo.DEVUELTO);
    }

    @Test
    void obtenerMetricasDisponibilidad_calculaExactitud_cuandoTodosLosEquiposEstanCorrectamenteBloqueados() {

        Mantenimiento mantenimientoAbierto = Mantenimiento.builder()
                .id(1L).equipo(equipoEnMantenimiento).tipo(TipoMantenimiento.PREVENTIVO)
                .fechaProgramada(LocalDateTime.now()).estado(EstadoMantenimiento.PROGRAMADO)
                .build();

        when(mantenimientoRepository.buscarPorEstado(EstadoMantenimiento.PROGRAMADO)).thenReturn(List.of(mantenimientoAbierto));

        DisponibilidadMetricasDTO resultado = equipoService.obtenerMetricasDisponibilidad();

        assertThat(resultado.getTotalEquiposEnMantenimiento()).isEqualTo(1);
        assertThat(resultado.getEquiposCorrectamenteBloqueados()).isEqualTo(1);
        assertThat(resultado.getExactitudDisponibilidad()).isEqualTo(100.0);
    }

    @Test
    void incrementarContadorUso_sumaLasHorasAlContadorExistente() {

        Equipo equipo = Equipo.builder().id(4L).nombre("Taladro")
                .estado(EstadoEquipo.DISPONIBLE).contadorUsoHoras(10).build();
        when(equipoRepository.findById(4L)).thenReturn(Optional.of(equipo));

        equipoService.incrementarContadorUso(4L, 5L);

        ArgumentCaptor<Equipo> captor = ArgumentCaptor.forClass(Equipo.class);
        verify(equipoRepository).save(captor.capture());
        assertThat(captor.getValue().getContadorUsoHoras()).isEqualTo(15);
    }

    @Test
    void obtenerRecomendacionesMantenimiento_devuelveEquiposQueSuperanElUmbral() {
        Equipo equipoDesgastado = Equipo.builder().id(6L).nombre("Microscopio C")
                .estado(EstadoEquipo.DISPONIBLE).contadorUsoHoras(150).build();
        when(equipoRepository.buscarPorUsoMayorOIgualA(100)).thenReturn(List.of(equipoDesgastado));
        when(mantenimientoRepository.buscarPreventivosProgramadosVencidos(any())).thenReturn(List.of());

        List<EquipoRecomendacionDTO> resultado = equipoService.obtenerRecomendacionesMantenimiento(100);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEquipoId()).isEqualTo(6L);
        assertThat(resultado.get(0).getContadorUsoHoras()).isEqualTo(150);
    }

    @Test
    void obtenerRecomendacionesMantenimiento_incluyePreventivoConFechaVencida_aunqueNoSupereLasHoras() {
        Equipo equipo = Equipo.builder().id(3L).nombre("Balanza").estado(EstadoEquipo.DISPONIBLE).contadorUsoHoras(4).build();
        Mantenimiento vencido = Mantenimiento.builder()
                .id(1L).equipo(equipo).tipo(TipoMantenimiento.PREVENTIVO)
                .fechaProgramada(LocalDateTime.now().minusDays(2))
                .estado(EstadoMantenimiento.PROGRAMADO)
                .build();
        when(equipoRepository.buscarPorUsoMayorOIgualA(100)).thenReturn(List.of());
        when(mantenimientoRepository.buscarPreventivosProgramadosVencidos(any())).thenReturn(List.of(vencido));

        List<EquipoRecomendacionDTO> resultado = equipoService.obtenerRecomendacionesMantenimiento(100);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEquipoId()).isEqualTo(3L);
        assertThat(resultado.get(0).getFechaProgramada()).isEqualTo(vencido.getFechaProgramada());
        assertThat(resultado.get(0).getMotivo()).contains("vencido");
    }

    @Test
    void obtenerDashboardUtilizacion_clasificaPorHorasYFrecuencia() {
        Equipo sinUso = Equipo.builder().id(1L).nombre("Nuevo").contadorUsoHoras(0).build();
        Equipo usado = Equipo.builder().id(2L).nombre("En uso").contadorUsoHoras(12).build();
        Equipo gastado = Equipo.builder().id(3L).nombre("Gastado").contadorUsoHoras(140).build();
        when(equipoRepository.findAll()).thenReturn(List.of(sinUso, usado, gastado));
        when(prestamoRepository.contarPorEquipo()).thenReturn(List.<Object[]>of(new Object[]{2L, 3L}, new Object[]{3L, 8L}));

        UtilizacionDashboardDTO resultado = equipoService.obtenerDashboardUtilizacion(100);

        assertThat(resultado.getTotalEquipos()).isEqualTo(3);
        assertThat(resultado.getSubutilizados()).isEqualTo(1);
        assertThat(resultado.getNormales()).isEqualTo(1);
        assertThat(resultado.getSobreutilizados()).isEqualTo(1);
        assertThat(resultado.getEquipos()).extracting("clasificacion")
                .containsExactly(
                        ClasificacionUtilizacion.SUBUTILIZADO,
                        ClasificacionUtilizacion.NORMAL,
                        ClasificacionUtilizacion.SOBREUTILIZADO);
    }

    @Test
    void actualizar_cambiaDatosDescriptivos_sinTocarEstadoNiContadorNiHistorial() {
        equipoDisponible.setContadorUsoHoras(40);
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoDisponible));
        when(categoriaRepository.findById(9L)).thenReturn(Optional.of(
                Categoria.builder().id(9L).nombre("Química").build()));
        when(equipoRepository.save(any(Equipo.class))).thenAnswer(inv -> inv.getArgument(0));

        var resultado = equipoService.actualizar(1L,
                new com.utp.smartlab.dto.EquipoRequestDTO("Microscopio editado", 9L, "Lab 9"));

        assertThat(resultado.getNombre()).isEqualTo("Microscopio editado");
        assertThat(resultado.getCategoriaId()).isEqualTo(9L);
        assertThat(resultado.getUbicacion()).isEqualTo("Lab 9");
        assertThat(resultado.getEstado()).isEqualTo(EstadoEquipo.DISPONIBLE);
        assertThat(resultado.getContadorUsoHoras()).isEqualTo(40);
        verify(historialRepository, never()).save(any());
    }

    @Test
    void eliminar_borraElEquipoYSuHistorial_cuandoNoTieneActividad() {
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoDisponible));

        equipoService.eliminar(1L);

        verify(historialRepository).eliminarPorEquipoId(1L);
        verify(equipoRepository).delete(equipoDisponible);
    }

    @Test
    void eliminar_lanzaExcepcion_cuandoTieneReservas() {
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoDisponible));
        when(reservaRepository.contarPorEquipoId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> equipoService.eliminar(1L))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("reserva");
        verify(equipoRepository, never()).delete(any());
    }
}
