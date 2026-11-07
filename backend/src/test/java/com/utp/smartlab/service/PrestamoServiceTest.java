package com.utp.smartlab.service;

import com.utp.smartlab.dto.PrestamoCheckoutRequestDTO;
import com.utp.smartlab.dto.PrestamoMetricasDTO;
import com.utp.smartlab.dto.PrestamoResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.EstadoEquipo;
import com.utp.smartlab.entity.EstadoPrestamo;
import com.utp.smartlab.entity.EstadoReserva;
import com.utp.smartlab.entity.Prestamo;
import com.utp.smartlab.entity.Reserva;
import com.utp.smartlab.exception.PrestamoInvalidoException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.PrestamoRepository;
import com.utp.smartlab.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EquipoService equipoService;

    @InjectMocks
    private PrestamoService prestamoService;

    private Equipo equipoMock(Long id) {
        return Equipo.builder().id(id).nombre("Equipo de prueba")
                .estado(EstadoEquipo.DISPONIBLE).contadorUsoHoras(0).build();
    }

    @Test
    void checkout_creaElPrestamo_cuandoLaReservaEstaConfirmada() {
        Equipo equipo = equipoMock(1L);
        Reserva reserva = Reserva.builder()
                .id(1L).equipo(equipo)
                .fechaInicio(LocalDateTime.now()).fechaFin(LocalDateTime.now().plusHours(2))
                .estado(EstadoReserva.CONFIRMADA)
                .build();

        PrestamoCheckoutRequestDTO dto = new PrestamoCheckoutRequestDTO(1L, LocalDateTime.now().plusDays(3));

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(prestamoRepository.buscarPorReservaId(1L)).thenReturn(Optional.empty());
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(inv -> {
            Prestamo p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        PrestamoResponseDTO resultado = prestamoService.checkout(dto);

        assertThat(resultado.getId()).isEqualTo(100L);
        assertThat(resultado.getReservaId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoPrestamo.ACTIVO);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.ATENDIDA);
    }

    @Test
    void checkout_lanzaExcepcion_cuandoLaReservaNoEstaConfirmada() {

        Equipo equipo = equipoMock(1L);
        Reserva reservaCancelada = Reserva.builder()
                .id(2L).equipo(equipo)
                .fechaInicio(LocalDateTime.now()).fechaFin(LocalDateTime.now().plusHours(2))
                .estado(EstadoReserva.CANCELADA)
                .build();

        PrestamoCheckoutRequestDTO dto = new PrestamoCheckoutRequestDTO(2L, LocalDateTime.now().plusDays(3));

        when(reservaRepository.findById(2L)).thenReturn(Optional.of(reservaCancelada));

        assertThatThrownBy(() -> prestamoService.checkout(dto))
                .isInstanceOf(PrestamoInvalidoException.class)
                .hasMessageContaining("CONFIRMADA");
    }

    @Test
    void checkout_lanzaExcepcion_cuandoLaReservaNoExiste() {
        PrestamoCheckoutRequestDTO dto = new PrestamoCheckoutRequestDTO(99L, LocalDateTime.now().plusDays(3));
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prestamoService.checkout(dto))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void checkin_marcaComoMoroso_yActualizaElContadorDeUsoDelEquipo() {
        Equipo equipo = equipoMock(3L);
        Reserva reserva = Reserva.builder().id(1L).equipo(equipo)
                .fechaInicio(LocalDateTime.now().minusDays(6)).fechaFin(LocalDateTime.now().minusDays(5))
                .estado(EstadoReserva.ATENDIDA).build();

        Prestamo prestamo = Prestamo.builder()
                .id(50L).reserva(reserva)
                .fechaEntrega(LocalDateTime.now().minusDays(5))
                .fechaLimite(LocalDateTime.now().minusDays(1))
                .estado(EstadoPrestamo.ACTIVO)
                .build();

        when(prestamoRepository.findById(50L)).thenReturn(Optional.of(prestamo));
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoResponseDTO resultado = prestamoService.checkin(50L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPrestamo.DEVUELTO);
        assertThat(resultado.isMoroso()).isTrue();

        verify(equipoService).incrementarContadorUso(eq(3L), anyLong());
    }

    @Test
    void checkin_lanzaExcepcion_cuandoElPrestamoYaFueDevuelto() {
        Equipo equipo = equipoMock(1L);
        Reserva reserva = Reserva.builder().id(1L).equipo(equipo)
                .fechaInicio(LocalDateTime.now().minusDays(6)).fechaFin(LocalDateTime.now().minusDays(5))
                .estado(EstadoReserva.ATENDIDA).build();

        Prestamo prestamoYaDevuelto = Prestamo.builder()
                .id(51L).reserva(reserva)
                .fechaEntrega(LocalDateTime.now().minusDays(5))
                .fechaLimite(LocalDateTime.now().minusDays(1))
                .fechaDevolucionReal(LocalDateTime.now().minusDays(2))
                .estado(EstadoPrestamo.DEVUELTO)
                .build();

        when(prestamoRepository.findById(51L)).thenReturn(Optional.of(prestamoYaDevuelto));

        assertThatThrownBy(() -> prestamoService.checkin(51L))
                .isInstanceOf(PrestamoInvalidoException.class);
    }

    private Prestamo prestamoDePrueba(Long id, LocalDateTime limite, LocalDateTime devolucionReal, EstadoPrestamo estado) {
        Equipo equipo = equipoMock(1L);
        Reserva reserva = Reserva.builder().id(id).equipo(equipo)
                .fechaInicio(LocalDateTime.now().minusDays(6)).fechaFin(LocalDateTime.now().minusDays(5))
                .estado(EstadoReserva.ATENDIDA).build();
        return Prestamo.builder()
                .id(id).reserva(reserva)
                .fechaEntrega(LocalDateTime.now().minusDays(5))
                .fechaLimite(limite)
                .fechaDevolucionReal(devolucionReal)
                .estado(estado)
                .build();
    }

    @Test
    void obtenerPorId_marcaComoVencido_unPrestamoActivoConLaFechaLimitePasada() {
        Prestamo prestamo = prestamoDePrueba(60L, LocalDateTime.now().minusHours(2), null, EstadoPrestamo.ACTIVO);
        when(prestamoRepository.findById(60L)).thenReturn(Optional.of(prestamo));

        PrestamoResponseDTO resultado = prestamoService.obtenerPorId(60L);

        assertThat(resultado.isVencido()).isTrue();
        assertThat(resultado.isMoroso()).isFalse();
    }

    @Test
    void obtenerPorId_noMarcaComoVencido_unPrestamoActivoDentroDelPlazo() {
        Prestamo prestamo = prestamoDePrueba(61L, LocalDateTime.now().plusDays(2), null, EstadoPrestamo.ACTIVO);
        when(prestamoRepository.findById(61L)).thenReturn(Optional.of(prestamo));

        PrestamoResponseDTO resultado = prestamoService.obtenerPorId(61L);

        assertThat(resultado.isVencido()).isFalse();
        assertThat(resultado.isMoroso()).isFalse();
    }

    @Test
    void obtenerPorId_unPrestamoDevueltoTarde_esMorosoPeroYaNoEstaVencido() {
        Prestamo prestamo = prestamoDePrueba(62L, LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(1), EstadoPrestamo.DEVUELTO);
        when(prestamoRepository.findById(62L)).thenReturn(Optional.of(prestamo));

        PrestamoResponseDTO resultado = prestamoService.obtenerPorId(62L);

        assertThat(resultado.isMoroso()).isTrue();
        assertThat(resultado.isVencido()).isFalse();
    }

    @Test
    void listarVencidos_devuelveLosPrestamosActivosConLimitePasado() {
        Prestamo vencido = prestamoDePrueba(63L, LocalDateTime.now().minusHours(5), null, EstadoPrestamo.ACTIVO);
        when(prestamoRepository.buscarVencidos(any(LocalDateTime.class))).thenReturn(List.of(vencido));

        List<PrestamoResponseDTO> resultado = prestamoService.listarVencidos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(63L);
        assertThat(resultado.get(0).isVencido()).isTrue();
    }

    @Test
    void obtenerMetricas_incluyeLosVencidos_sinAlterarLaTasaDeMora() {
        when(prestamoRepository.count()).thenReturn(6L);
        when(prestamoRepository.contarPorEstado(EstadoPrestamo.ACTIVO)).thenReturn(2L);
        when(prestamoRepository.contarPorEstado(EstadoPrestamo.DEVUELTO)).thenReturn(4L);
        when(prestamoRepository.contarConMora()).thenReturn(1L);
        when(prestamoRepository.contarVencidos(any(LocalDateTime.class))).thenReturn(2L);

        PrestamoMetricasDTO resultado = prestamoService.obtenerMetricas();

        assertThat(resultado.getVencidos()).isEqualTo(2L);
        assertThat(resultado.getTasaMora()).isEqualTo(25.0);
    }
}
