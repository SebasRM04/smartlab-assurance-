package com.utp.smartlab.service;

import com.utp.smartlab.dto.MantenimientoMetricasDTO;
import com.utp.smartlab.dto.MantenimientoRequestDTO;
import com.utp.smartlab.dto.MantenimientoResponseDTO;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.EstadoEquipo;
import com.utp.smartlab.entity.EstadoMantenimiento;
import com.utp.smartlab.entity.Mantenimiento;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.MantenimientoInvalidoException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.MantenimientoRepository;
import org.springframework.stereotype.Service;
import com.utp.smartlab.dto.PaginaDTO;
import com.utp.smartlab.entity.TipoMantenimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MantenimientoService {

    private final MantenimientoRepository mantenimientoRepository;
    private final EquipoService equipoService;
    private final UsuarioActualService usuarioActualService;

    public MantenimientoService(MantenimientoRepository mantenimientoRepository,
                                EquipoService equipoService,
                                UsuarioActualService usuarioActualService) {
        this.mantenimientoRepository = mantenimientoRepository;
        this.equipoService = equipoService;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional
    public MantenimientoResponseDTO programar(MantenimientoRequestDTO dto) {
        return programar(dto, usuarioActualService.obtener());
    }

    @Transactional
    public MantenimientoResponseDTO programar(MantenimientoRequestDTO dto, Usuario usuario) {
        Equipo equipo = equipoService.obtenerEntidadPorId(dto.getEquipoId());

        Mantenimiento mantenimiento = Mantenimiento.builder()
                .equipo(equipo)
                .usuario(usuario)
                .tipo(dto.getTipo())
                .fechaProgramada(dto.getFechaProgramada())
                .estado(EstadoMantenimiento.PROGRAMADO)
                .build();

        Mantenimiento guardado = mantenimientoRepository.save(mantenimiento);

        // Regla de negocio 1: programar mantenimiento bloquea el equipo
        equipoService.actualizarEstado(equipo.getId(), EstadoEquipo.EN_MANTENIMIENTO, usuario,
                "Mantenimiento " + dto.getTipo() + " programado");

        return mapearAResponseDTO(guardado);
    }

    @Transactional
    public MantenimientoResponseDTO cerrar(Long mantenimientoId) {
        return cerrar(mantenimientoId, usuarioActualService.obtener());
    }

    /**
     * Sobrecarga para el seeder, que corre sin un JWT.
     */
    @Transactional
    public MantenimientoResponseDTO cerrar(Long mantenimientoId, Usuario responsable) {
        Mantenimiento mantenimiento = buscarEntidadPorId(mantenimientoId);

        if (mantenimiento.getEstado() == EstadoMantenimiento.CERRADO) {
            throw new MantenimientoInvalidoException("El mantenimiento " + mantenimientoId + " ya fue cerrado");
        }

        mantenimiento.setFechaEjecucion(LocalDateTime.now());
        mantenimiento.setEstado(EstadoMantenimiento.CERRADO);
        Mantenimiento guardado = mantenimientoRepository.save(mantenimiento);

        // Si el equipo aún tiene otro mantenimiento PROGRAMADO, sigue bloqueado: liberarlo
        // dejaría un estado que no coincide con la situación real (exactitud de disponibilidad).
        Long equipoId = mantenimiento.getEquipo().getId();
        long otrosAbiertos = mantenimientoRepository.contarProgramadosDelEquipoExcepto(equipoId, mantenimientoId);
        if (otrosAbiertos == 0) {
            equipoService.actualizarEstado(equipoId, EstadoEquipo.DISPONIBLE, responsable,
                    "Mantenimiento cerrado");
        }

        return mapearAResponseDTO(guardado);
    }

    public List<MantenimientoResponseDTO> listar() {
        return mantenimientoRepository.findAll().stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    // Listado por página, filtrable por estado, tipo y equipo
    public PaginaDTO<MantenimientoResponseDTO> listarPagina(int pagina, int tamanio, EstadoMantenimiento estado,
                                                            TipoMantenimiento tipo, Long equipoId) {
        Pageable pageable = PaginaDTO.solicitud(pagina, tamanio);
        Collection<EstadoMantenimiento> estados =
                estado == null ? List.of(EstadoMantenimiento.values()) : List.of(estado);
        Collection<TipoMantenimiento> tipos = tipo == null ? List.of(TipoMantenimiento.values()) : List.of(tipo);

        Page<Mantenimiento> resultado = equipoId == null
                ? mantenimientoRepository.buscarPagina(estados, tipos, pageable)
                : mantenimientoRepository.buscarPaginaPorEquipo(equipoId, estados, tipos, pageable);
        return PaginaDTO.de(resultado, this::mapearAResponseDTO);
    }

    public MantenimientoResponseDTO obtenerPorId(Long id) {
        return mapearAResponseDTO(buscarEntidadPorId(id));
    }

    // Indicador: tasa de mantenimiento tardío
    public MantenimientoMetricasDTO obtenerMetricas() {
        long total = mantenimientoRepository.count();
        long cerrados = mantenimientoRepository.contarPorEstado(EstadoMantenimiento.CERRADO);
        long tardios = mantenimientoRepository.contarTardios();

        double tasaTardio = cerrados == 0 ? 0.0 : (tardios * 100.0) / cerrados;

        return MantenimientoMetricasDTO.builder()
                .total(total)
                .cerrados(cerrados)
                .tardios(tardios)
                .tasaTardio(tasaTardio)
                .build();
    }

    private Mantenimiento buscarEntidadPorId(Long id) {
        return mantenimientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el mantenimiento con id " + id));
    }

    private boolean esTardio(Mantenimiento mantenimiento) {
        return mantenimiento.getFechaEjecucion() != null
                && mantenimiento.getFechaEjecucion().isAfter(mantenimiento.getFechaProgramada());
    }

    private MantenimientoResponseDTO mapearAResponseDTO(Mantenimiento mantenimiento) {
        return MantenimientoResponseDTO.builder()
                .id(mantenimiento.getId())
                .equipoId(mantenimiento.getEquipo().getId())
                .usuarioId(mantenimiento.getUsuario().getId())
                .usuarioNombre(mantenimiento.getUsuario().getNombre())
                .tipo(mantenimiento.getTipo())
                .fechaProgramada(mantenimiento.getFechaProgramada())
                .fechaEjecucion(mantenimiento.getFechaEjecucion())
                .estado(mantenimiento.getEstado())
                .tardio(esTardio(mantenimiento))
                .build();
    }
}
