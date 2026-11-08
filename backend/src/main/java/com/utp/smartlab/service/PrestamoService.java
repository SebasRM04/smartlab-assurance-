package com.utp.smartlab.service;

import com.utp.smartlab.dto.PrestamoCheckoutRequestDTO;
import com.utp.smartlab.dto.PrestamoMetricasDTO;
import com.utp.smartlab.dto.PrestamoResponseDTO;
import com.utp.smartlab.entity.EstadoPrestamo;
import com.utp.smartlab.entity.EstadoReserva;
import com.utp.smartlab.entity.Prestamo;
import com.utp.smartlab.entity.Reserva;
import com.utp.smartlab.exception.PrestamoInvalidoException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.PrestamoRepository;
import com.utp.smartlab.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import com.utp.smartlab.dto.PaginaDTO;
import org.springframework.data.domain.Page;
import java.util.Collection;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;
    private final EquipoService equipoService;

    public PrestamoService(PrestamoRepository prestamoRepository,
                            ReservaRepository reservaRepository,
                            EquipoService equipoService) {
        this.prestamoRepository = prestamoRepository;
        this.reservaRepository = reservaRepository;
        this.equipoService = equipoService;
    }

    // Regla de negocio 3: préstamo solo sobre una reserva CONFIRMADA y sin otro préstamo
    @Transactional
    public PrestamoResponseDTO checkout(PrestamoCheckoutRequestDTO dto) {
        Reserva reserva = reservaRepository.findById(dto.getReservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la reserva con id " + dto.getReservaId()));

        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new PrestamoInvalidoException(
                    "No se puede iniciar el préstamo: la reserva " + reserva.getId() + " no está en estado CONFIRMADA (estado actual: " + reserva.getEstado() + ")");
        }

        prestamoRepository.buscarPorReservaId(dto.getReservaId()).ifPresent(p -> {
            throw new PrestamoInvalidoException("La reserva " + dto.getReservaId() + " ya tiene un préstamo asociado");
        });

        Prestamo prestamo = Prestamo.builder()
                .reserva(reserva)
                .fechaEntrega(LocalDateTime.now())
                .fechaLimite(dto.getFechaLimite())
                .estado(EstadoPrestamo.ACTIVO)
                .build();

        Prestamo guardado = prestamoRepository.save(prestamo);

        reserva.setEstado(EstadoReserva.ATENDIDA);
        reservaRepository.save(reserva);

        return mapearAResponseDTO(guardado);
    }

    @Transactional
    public PrestamoResponseDTO checkin(Long prestamoId) {
        Prestamo prestamo = buscarEntidadPorId(prestamoId);

        if (prestamo.getEstado() != EstadoPrestamo.ACTIVO) {
            throw new PrestamoInvalidoException("El préstamo " + prestamoId + " ya fue devuelto");
        }

        LocalDateTime ahora = LocalDateTime.now();
        prestamo.setFechaDevolucionReal(ahora);
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        Prestamo guardado = prestamoRepository.save(prestamo);

        // Indicador: contador de uso por horas
        long horasUsadas = Duration.between(prestamo.getFechaEntrega(), ahora).toHours();
        equipoService.incrementarContadorUso(prestamo.getReserva().getEquipo().getId(), Math.max(horasUsadas, 0));

        return mapearAResponseDTO(guardado);
    }

    public List<PrestamoResponseDTO> listar() {
        return prestamoRepository.findAll().stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    // Listado por página, del préstamo más reciente al más antiguo
    public PaginaDTO<PrestamoResponseDTO> listarPagina(int pagina, int tamanio, EstadoPrestamo estado) {
        Collection<EstadoPrestamo> estados = estado == null ? List.of(EstadoPrestamo.values()) : List.of(estado);
        Page<Prestamo> resultado = prestamoRepository.buscarPagina(estados, PaginaDTO.solicitud(pagina, tamanio));
        return PaginaDTO.de(resultado, this::mapearAResponseDTO);
    }

    public PrestamoResponseDTO obtenerPorId(Long id) {
        return mapearAResponseDTO(buscarEntidadPorId(id));
    }

    // Préstamos ACTIVOS cuya fecha límite ya pasó, del más atrasado al más reciente
    public List<PrestamoResponseDTO> listarVencidos() {
        return prestamoRepository.buscarVencidos(LocalDateTime.now()).stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    // Indicador: tasa de mora en devolución
    public PrestamoMetricasDTO obtenerMetricas() {
        long total = prestamoRepository.count();
        long activos = prestamoRepository.contarPorEstado(EstadoPrestamo.ACTIVO);
        long devueltos = prestamoRepository.contarPorEstado(EstadoPrestamo.DEVUELTO);
        long conMora = prestamoRepository.contarConMora();
        long vencidos = prestamoRepository.contarVencidos(LocalDateTime.now());

        double tasaMora = devueltos == 0 ? 0.0 : (conMora * 100.0) / devueltos;

        return PrestamoMetricasDTO.builder()
                .total(total)
                .activos(activos)
                .devueltos(devueltos)
                .conMora(conMora)
                .vencidos(vencidos)
                .tasaMora(tasaMora)
                .build();
    }

    private boolean esMoroso(Prestamo prestamo) {
        return prestamo.getFechaDevolucionReal() != null
                && prestamo.getFechaDevolucionReal().isAfter(prestamo.getFechaLimite());
    }

    private boolean estaVencido(Prestamo prestamo) {
        return prestamo.getEstado() == EstadoPrestamo.ACTIVO
                && prestamo.getFechaLimite().isBefore(LocalDateTime.now());
    }

    private Prestamo buscarEntidadPorId(Long id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el préstamo con id " + id));
    }

    private PrestamoResponseDTO mapearAResponseDTO(Prestamo prestamo) {
        return PrestamoResponseDTO.builder()
                .id(prestamo.getId())
                .reservaId(prestamo.getReserva().getId())
                .fechaEntrega(prestamo.getFechaEntrega())
                .fechaLimite(prestamo.getFechaLimite())
                .fechaDevolucionReal(prestamo.getFechaDevolucionReal())
                .estado(prestamo.getEstado())
                .moroso(esMoroso(prestamo))
                .vencido(estaVencido(prestamo))
                .build();
    }
}
