package com.utp.smartlab.service;

import com.utp.smartlab.dto.ConflictoMetricasDTO;
import com.utp.smartlab.dto.ReservaMetricasDTO;
import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.dto.ReservaResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.EstadoReserva;
import com.utp.smartlab.entity.Reserva;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.ConflictoReservaException;
import com.utp.smartlab.exception.OperacionNoPermitidaException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import com.utp.smartlab.dto.PaginaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final EquipoService equipoService;
    private final SolicitudRechazadaService solicitudRechazadaService;
    private final UsuarioActualService usuarioActualService;

    public ReservaService(ReservaRepository reservaRepository,
                           EquipoService equipoService,
                           SolicitudRechazadaService solicitudRechazadaService,
                           UsuarioActualService usuarioActualService) {
        this.reservaRepository = reservaRepository;
        this.equipoService = equipoService;
        this.solicitudRechazadaService = solicitudRechazadaService;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional
    public ReservaResponseDTO crear(ReservaRequestDTO dto) {
        return crear(dto, usuarioActualService.obtener());
    }

    @Transactional
    public ReservaResponseDTO crear(ReservaRequestDTO dto, Usuario usuario) {

        // Regla de negocio 1: un equipo en mantenimiento no se reserva
        Equipo equipo = equipoService.validarDisponibleParaReserva(dto.getEquipoId());

        // Regla de negocio 2: sin reservas superpuestas del mismo equipo
        validarSinSolapamiento(equipo, dto.getFechaInicio(), dto.getFechaFin());

        Reserva reserva = Reserva.builder()
                .equipo(equipo)
                .usuario(usuario)
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .estado(EstadoReserva.CONFIRMADA)
                .build();

        Reserva guardada = reservaRepository.save(reserva);
        return mapearAResponseDTO(guardada);
    }

    public List<ReservaResponseDTO> listar() {
        // Regla de negocio: el estudiante solo ve sus reservas
        List<Reserva> reservas = usuarioActualService.esEstudiante()
                ? reservaRepository.buscarPorUsuarioId(usuarioActualService.obtener().getId())
                : reservaRepository.findAll();
        return reservas.stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    // Listado por página. El estudiante solo ve las suyas (ignora usuarioId); el personal puede filtrar
    // por equipo y, si el rol lo permite (lo valida el controller), por usuario, solos o combinados.
    public PaginaDTO<ReservaResponseDTO> listarPagina(int pagina, int tamanio, EstadoReserva estado,
                                                      Long equipoId, Long usuarioId,
                                                      LocalDate desde, LocalDate hasta) {
        RangoFechas.validar(desde, hasta);
        Pageable pageable = PaginaDTO.solicitud(pagina, tamanio);
        Collection<EstadoReserva> estados = estado == null ? List.of(EstadoReserva.values()) : List.of(estado);
        LocalDateTime ini = RangoFechas.inicio(desde);
        LocalDateTime fin = RangoFechas.fin(hasta);

        Page<Reserva> resultado;
        if (usuarioActualService.esEstudiante()) {
            resultado = reservaRepository.buscarPaginaPorUsuario(
                    usuarioActualService.obtener().getId(), estados, ini, fin, pageable);
        } else if (equipoId != null && usuarioId != null) {
            resultado = reservaRepository.buscarPaginaPorEquipoYUsuario(equipoId, usuarioId, estados, ini, fin, pageable);
        } else if (equipoId != null) {
            resultado = reservaRepository.buscarPaginaPorEquipo(equipoId, estados, ini, fin, pageable);
        } else if (usuarioId != null) {
            resultado = reservaRepository.buscarPaginaPorUsuario(usuarioId, estados, ini, fin, pageable);
        } else {
            resultado = reservaRepository.buscarPagina(estados, ini, fin, pageable);
        }
        return PaginaDTO.de(resultado, this::mapearAResponseDTO);
    }

    // Cola del técnico: reservas confirmadas que ya empezaron o empiezan en las próximas horas (1 a 168, 24 por defecto)
    public List<ReservaResponseDTO> listarPendientesDeEntrega(int horas) {
        int horasValidas = Math.min(Math.max(horas, 1), 168);
        Page<Reserva> resultado = reservaRepository.buscarPendientesDeEntrega(
                LocalDateTime.now().plusHours(horasValidas), PaginaDTO.solicitud(0, PaginaDTO.TAMANIO_MAXIMO));
        return resultado.getContent().stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    public ReservaResponseDTO obtenerPorId(Long id) {
        Reserva reserva = buscarEntidadPorId(id);
        validarVisibilidadDeEstudiante(reserva);
        return mapearAResponseDTO(reserva);
    }

    @Transactional
    public ReservaResponseDTO cambiarEstado(Long id, EstadoReserva nuevoEstado) {
        Reserva reserva = buscarEntidadPorId(id);
        // Regla de negocio: el estudiante solo cancela su reserva CONFIRMADA
        if (usuarioActualService.esEstudiante()) {
            validarDueno(reserva);
            if (nuevoEstado != EstadoReserva.CANCELADA) {
                throw new OperacionNoPermitidaException(
                        "Un estudiante solo puede cancelar sus reservas, no cambiarlas a " + nuevoEstado);
            }
            if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
                throw new OperacionNoPermitidaException(
                        "Solo se puede cancelar una reserva en estado CONFIRMADA (estado actual: " + reserva.getEstado() + ")");
            }
        }

        if (nuevoEstado == EstadoReserva.NO_SHOW) {
            if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
                throw new OperacionNoPermitidaException(
                        "Solo una reserva CONFIRMADA puede marcarse como NO_SHOW (estado actual: " + reserva.getEstado() + ")");
            }
            if (!LocalDateTime.now().isAfter(reserva.getFechaInicio())) {
                throw new OperacionNoPermitidaException(
                        "Solo se puede marcar NO_SHOW cuando la reserva ya inició. Si aún no empieza, debe cancelarse");
            }
        }

        reserva.setEstado(nuevoEstado);
        return mapearAResponseDTO(reservaRepository.save(reserva));
    }

    // Indicador: tasa de inasistencia (no-show)
    public ReservaMetricasDTO obtenerMetricas() {
        long total = reservaRepository.count();
        long confirmadas = reservaRepository.contarPorEstado(EstadoReserva.CONFIRMADA);
        long atendidas = reservaRepository.contarPorEstado(EstadoReserva.ATENDIDA);
        long noShow = reservaRepository.contarPorEstado(EstadoReserva.NO_SHOW);
        long canceladas = reservaRepository.contarPorEstado(EstadoReserva.CANCELADA);

        // Las confirmadas se conservan: ATENDIDA y NO_SHOW salieron de CONFIRMADA.
        // Una cancelada ya no forma parte de ese total.
        long totalConfirmadas = confirmadas + atendidas + noShow;
        double tasaNoShow = totalConfirmadas == 0 ? 0.0 : (noShow * 100.0) / totalConfirmadas;

        return ReservaMetricasDTO.builder()
                .total(total)
                .confirmadas(confirmadas)
                .atendidas(atendidas)
                .noShow(noShow)
                .canceladas(canceladas)
                .totalConfirmadas(totalConfirmadas)
                .tasaNoShow(tasaNoShow)
                .build();
    }

    // Indicador: tasa de conflicto de reserva
    public ConflictoMetricasDTO obtenerMetricasConflicto() {
        long reservasGuardadas = reservaRepository.count();
        long rechazadas = solicitudRechazadaService.contar();
        long totalSolicitudes = reservasGuardadas + rechazadas;

        double tasaConflicto = totalSolicitudes == 0 ? 0.0 : (rechazadas * 100.0) / totalSolicitudes;

        return ConflictoMetricasDTO.builder()
                .totalSolicitudes(totalSolicitudes)
                .rechazadasPorConflicto(rechazadas)
                .tasaConflicto(tasaConflicto)
                .build();
    }

    private void validarSinSolapamiento(Equipo equipo, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Reserva> solapadas = reservaRepository.buscarSolapadas(equipo.getId(), fechaInicio, fechaFin);

        if (!solapadas.isEmpty()) {
            // Indicador: tasa de conflicto de reserva
            solicitudRechazadaService.registrarIntentoRechazado(equipo.getId(), fechaInicio, fechaFin);

            throw new ConflictoReservaException(
                    "Ya existe una reserva para el equipo " + equipo.getId() + " que se solapa con el horario solicitado");
        }
    }

    private Reserva buscarEntidadPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la reserva con id " + id));
    }

    private void validarVisibilidadDeEstudiante(Reserva reserva) {
        if (usuarioActualService.esEstudiante()) {
            validarDueno(reserva);
        }
    }

    private void validarDueno(Reserva reserva) {
        Usuario actual = usuarioActualService.obtener();
        if (reserva.getUsuario() == null || !actual.getId().equals(reserva.getUsuario().getId())) {
            throw new OperacionNoPermitidaException("Solo puedes consultar o cancelar tus propias reservas");
        }
    }

    private ReservaResponseDTO mapearAResponseDTO(Reserva reserva) {
        return ReservaResponseDTO.builder()
                .id(reserva.getId())
                .equipoId(reserva.getEquipo().getId())
                .usuarioId(reserva.getUsuario().getId())
                .usuarioNombre(reserva.getUsuario().getNombre())
                .fechaInicio(reserva.getFechaInicio())
                .fechaFin(reserva.getFechaFin())
                .estado(reserva.getEstado())
                .build();
    }
}
