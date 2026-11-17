package com.utp.smartlab.service;

import com.utp.smartlab.dto.ConflictoMetricasDTO;
import com.utp.smartlab.dto.ReservaMetricasDTO;
import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.dto.ReservaResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.EstadoEquipo;
import com.utp.smartlab.entity.EstadoReserva;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.Reserva;
import com.utp.smartlab.entity.Rol;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.ConflictoReservaException;
import com.utp.smartlab.exception.EquipoNoDisponibleException;
import com.utp.smartlab.exception.OperacionNoPermitidaException;
import com.utp.smartlab.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EquipoService equipoService;

    @Mock
    private SolicitudRechazadaService solicitudRechazadaService;

    @Mock
    private UsuarioActualService usuarioActualService;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = usuarioMock();
        lenient().when(usuarioActualService.obtener()).thenReturn(usuario);
    }

    private Equipo equipoMock(Long id) {
        return Equipo.builder().id(id).nombre("Equipo de prueba")
                .estado(EstadoEquipo.DISPONIBLE).contadorUsoHoras(0).build();
    }

    private Usuario usuarioMock() {
        return Usuario.builder()
                .id(1L)
                .nombre("Carla Estudiante")
                .email("estudiante@smartlab.com")
                .passwordHash("x")
                .rol(Rol.builder().id(1L).nombre(NombreRol.ESTUDIANTE).build())
                .build();
    }

    @Test
    void crear_guardaLaReserva_cuandoEquipoDisponibleYSinSolapamiento() {
        Long equipoId = 1L;
        Equipo equipo = equipoMock(equipoId);
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = inicio.plusHours(2);

        ReservaRequestDTO dto = new ReservaRequestDTO(equipoId, inicio, fin);

        when(equipoService.validarDisponibleParaReserva(equipoId)).thenReturn(equipo);
        when(reservaRepository.buscarSolapadas(equipoId, inicio, fin)).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> {
            Reserva r = invocacion.getArgument(0);
            r.setId(10L);
            return r;
        });

        ReservaResponseDTO resultado = reservaService.crear(dto);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getEquipoId()).isEqualTo(equipoId);
        assertThat(resultado.getUsuarioId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(solicitudRechazadaService, never()).registrarIntentoRechazado(any(), any(), any());
    }

    @Test
    void crear_lanzaExcepcion_cuandoElEquipoNoEstaDisponible() {
        Long equipoId = 2L;
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = inicio.plusHours(2);
        ReservaRequestDTO dto = new ReservaRequestDTO(equipoId, inicio, fin);

        doThrow(new EquipoNoDisponibleException("El equipo no está disponible"))
                .when(equipoService).validarDisponibleParaReserva(equipoId);

        assertThatThrownBy(() -> reservaService.crear(dto))
                .isInstanceOf(EquipoNoDisponibleException.class);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crear_lanzaExcepcionYRegistraLaSolicitudRechazada_cuandoHaySolapamientoDeHorario() {

        Long equipoId = 1L;
        Equipo equipo = equipoMock(equipoId);

        LocalDateTime inicioExistente = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime finExistente = inicioExistente.plusHours(2);

        Reserva reservaExistente = Reserva.builder()
                .id(5L).equipo(equipo).usuario(usuario)
                .fechaInicio(inicioExistente).fechaFin(finExistente)
                .estado(EstadoReserva.CONFIRMADA)
                .build();

        LocalDateTime inicioNueva = inicioExistente.plusHours(1);
        LocalDateTime finNueva = finExistente.plusHours(1);
        ReservaRequestDTO dto = new ReservaRequestDTO(equipoId, inicioNueva, finNueva);

        when(equipoService.validarDisponibleParaReserva(equipoId)).thenReturn(equipo);
        when(reservaRepository.buscarSolapadas(equipoId, inicioNueva, finNueva)).thenReturn(List.of(reservaExistente));

        assertThatThrownBy(() -> reservaService.crear(dto))
                .isInstanceOf(ConflictoReservaException.class)
                .hasMessageContaining("se solapa");

        verify(reservaRepository, never()).save(any());
        verify(solicitudRechazadaService).registrarIntentoRechazado(eq(equipoId), eq(inicioNueva), eq(finNueva));
    }

    @Test
    void crear_permiteLaReserva_cuandoLosHorariosNoSeSolapan() {
        Long equipoId = 1L;
        Equipo equipo = equipoMock(equipoId);
        LocalDateTime inicioNueva = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);
        LocalDateTime finNueva = inicioNueva.plusHours(1);

        ReservaRequestDTO dto = new ReservaRequestDTO(equipoId, inicioNueva, finNueva);

        when(equipoService.validarDisponibleParaReserva(equipoId)).thenReturn(equipo);
        when(reservaRepository.buscarSolapadas(equipoId, inicioNueva, finNueva)).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ReservaResponseDTO resultado = reservaService.crear(dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(solicitudRechazadaService, never()).registrarIntentoRechazado(any(), any(), any());
    }

    @Test
    void cambiarEstado_marcaComoNoShow_correctamente() {
        Equipo equipo = equipoMock(1L);
        Reserva reserva = Reserva.builder()
                .id(7L).equipo(equipo).usuario(usuario)
                .fechaInicio(LocalDateTime.now().minusMinutes(30)).fechaFin(LocalDateTime.now().plusHours(1))
                .estado(EstadoReserva.CONFIRMADA)
                .build();

        when(reservaRepository.findById(7L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ReservaResponseDTO resultado = reservaService.cambiarEstado(7L, EstadoReserva.NO_SHOW);

        assertThat(resultado.getEstado()).isEqualTo(EstadoReserva.NO_SHOW);
        assertThat(resultado.getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void listar_delEstudiante_soloDevuelveLasSuyas() {
        when(usuarioActualService.esEstudiante()).thenReturn(true);
        when(reservaRepository.buscarPorUsuarioId(1L)).thenReturn(List.of());

        reservaService.listar();

        verify(reservaRepository).buscarPorUsuarioId(1L);
        verify(reservaRepository, never()).findAll();
    }

    @Test
    void obtenerPorId_delEstudiante_rechazaReservaAjena() {
        Usuario otro = Usuario.builder().id(9L).nombre("Diana").email("docente@smartlab.com")
                .rol(Rol.builder().nombre(NombreRol.DOCENTE).build()).build();
        Reserva ajena = Reserva.builder().id(4L).equipo(equipoMock(1L)).usuario(otro)
                .fechaInicio(LocalDateTime.now()).fechaFin(LocalDateTime.now().plusHours(1))
                .estado(EstadoReserva.CONFIRMADA).build();

        when(usuarioActualService.esEstudiante()).thenReturn(true);
        when(reservaRepository.findById(4L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> reservaService.obtenerPorId(4L))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void cambiarEstado_estudianteSoloPuedeCancelarLaSuyaConfirmada() {
        Reserva propia = Reserva.builder()
                .id(4L).equipo(equipoMock(1L)).usuario(usuario)
                .fechaInicio(LocalDateTime.now().plusDays(1)).fechaFin(LocalDateTime.now().plusDays(1).plusHours(2))
                .estado(EstadoReserva.CONFIRMADA).build();

        when(usuarioActualService.esEstudiante()).thenReturn(true);
        when(reservaRepository.findById(4L)).thenReturn(Optional.of(propia));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ReservaResponseDTO resultado = reservaService.cambiarEstado(4L, EstadoReserva.CANCELADA);

        assertThat(resultado.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void cambiarEstado_estudianteNoPuedeMarcarNoShow() {
        Reserva propia = Reserva.builder()
                .id(4L).equipo(equipoMock(1L)).usuario(usuario)
                .fechaInicio(LocalDateTime.now().plusDays(1)).fechaFin(LocalDateTime.now().plusDays(1).plusHours(2))
                .estado(EstadoReserva.CONFIRMADA).build();

        when(usuarioActualService.esEstudiante()).thenReturn(true);
        when(reservaRepository.findById(4L)).thenReturn(Optional.of(propia));

        assertThatThrownBy(() -> reservaService.cambiarEstado(4L, EstadoReserva.NO_SHOW))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void cambiarEstado_rechazaNoShow_cuandoLaReservaAunNoInicia() {
        Reserva futura = Reserva.builder()
                .id(8L).equipo(equipoMock(1L)).usuario(usuario)
                .fechaInicio(LocalDateTime.now().plusDays(1)).fechaFin(LocalDateTime.now().plusDays(1).plusHours(2))
                .estado(EstadoReserva.CONFIRMADA).build();

        when(reservaRepository.findById(8L)).thenReturn(Optional.of(futura));

        assertThatThrownBy(() -> reservaService.cambiarEstado(8L, EstadoReserva.NO_SHOW))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("ya inició");
    }

    @Test
    void cambiarEstado_rechazaNoShow_cuandoNoEstaConfirmada() {
        Reserva atendida = Reserva.builder()
                .id(9L).equipo(equipoMock(1L)).usuario(usuario)
                .fechaInicio(LocalDateTime.now().minusHours(2)).fechaFin(LocalDateTime.now().minusHours(1))
                .estado(EstadoReserva.ATENDIDA).build();

        when(reservaRepository.findById(9L)).thenReturn(Optional.of(atendida));

        assertThatThrownBy(() -> reservaService.cambiarEstado(9L, EstadoReserva.NO_SHOW))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("CONFIRMADA");
    }

    @Test
    void obtenerMetricas_calculaLaTasaDeNoShowCorrectamente() {
        when(reservaRepository.count()).thenReturn(10L);
        when(reservaRepository.contarPorEstado(EstadoReserva.CONFIRMADA)).thenReturn(4L);
        when(reservaRepository.contarPorEstado(EstadoReserva.ATENDIDA)).thenReturn(3L);
        when(reservaRepository.contarPorEstado(EstadoReserva.NO_SHOW)).thenReturn(2L);
        when(reservaRepository.contarPorEstado(EstadoReserva.CANCELADA)).thenReturn(1L);

        ReservaMetricasDTO resultado = reservaService.obtenerMetricas();

        assertThat(resultado.getTotal()).isEqualTo(10L);
        assertThat(resultado.getNoShow()).isEqualTo(2L);
        assertThat(resultado.getTotalConfirmadas()).isEqualTo(9L);
        assertThat(resultado.getTasaNoShow()).isEqualTo((2 * 100.0) / 9);
    }

    @Test
    void obtenerMetricasConflicto_calculaLaTasaCorrectamente() {
        when(reservaRepository.count()).thenReturn(8L);
        when(solicitudRechazadaService.contar()).thenReturn(2L);

        ConflictoMetricasDTO resultado = reservaService.obtenerMetricasConflicto();

        assertThat(resultado.getTotalSolicitudes()).isEqualTo(10L);
        assertThat(resultado.getRechazadasPorConflicto()).isEqualTo(2L);
        assertThat(resultado.getTasaConflicto()).isEqualTo(20.0);
    }

    @Test
    void listarPagina_delPersonalConUsuarioId_filtraPorEseUsuario() {
        when(usuarioActualService.esEstudiante()).thenReturn(false);
        when(reservaRepository.buscarPaginaPorUsuario(eq(7L), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

        reservaService.listarPagina(0, 20, null, null, 7L, null, null);

        verify(reservaRepository).buscarPaginaPorUsuario(eq(7L), any(), any(), any(), any(Pageable.class));
        verify(reservaRepository, never()).buscarPagina(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void listarPagina_delPersonalConUsuarioIdYEquipoId_combinaAmbosFiltros() {
        when(usuarioActualService.esEstudiante()).thenReturn(false);
        when(reservaRepository.buscarPaginaPorEquipoYUsuario(eq(3L), eq(7L), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        reservaService.listarPagina(0, 20, EstadoReserva.CONFIRMADA, 3L, 7L, null, null);

        verify(reservaRepository).buscarPaginaPorEquipoYUsuario(eq(3L), eq(7L), any(), any(), any(), any(Pageable.class));
        verify(reservaRepository, never()).buscarPaginaPorEquipo(any(), any(), any(), any(), any(Pageable.class));
        verify(reservaRepository, never()).buscarPaginaPorUsuario(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void listarPagina_delEstudiante_ignoraElUsuarioIdYUsaElSuyo() {
        when(usuarioActualService.esEstudiante()).thenReturn(true);
        when(reservaRepository.buscarPaginaPorUsuario(eq(1L), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

        reservaService.listarPagina(0, 20, null, null, 99L, null, null);

        verify(reservaRepository).buscarPaginaPorUsuario(eq(1L), any(), any(), any(), any(Pageable.class));
        verify(reservaRepository, never()).buscarPaginaPorUsuario(eq(99L), any(), any(), any(), any(Pageable.class));
    }
}
