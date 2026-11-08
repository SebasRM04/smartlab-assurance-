package com.utp.smartlab.service;

import com.utp.smartlab.dto.DisponibilidadMetricasDTO;
import com.utp.smartlab.dto.EquipoRecomendacionDTO;
import com.utp.smartlab.dto.EquipoRequestDTO;
import com.utp.smartlab.dto.EquipoResponseDTO;
import com.utp.smartlab.dto.HistorialEstadoResponseDTO;
import com.utp.smartlab.dto.UtilizacionDashboardDTO;
import com.utp.smartlab.dto.UtilizacionEquipoResumenDTO;
import com.utp.smartlab.dto.UtilizacionEquipoResponseDTO;
import com.utp.smartlab.entity.ClasificacionUtilizacion;
import com.utp.smartlab.entity.Categoria;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.EstadoEquipo;
import com.utp.smartlab.entity.EstadoMantenimiento;
import com.utp.smartlab.entity.HistorialEstadoEquipo;
import com.utp.smartlab.entity.Mantenimiento;
import com.utp.smartlab.entity.Prestamo;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.exception.EquipoNoDisponibleException;
import com.utp.smartlab.exception.OperacionNoPermitidaException;
import com.utp.smartlab.exception.RecursoNoEncontradoException;
import com.utp.smartlab.repository.IncidenciaRepository;
import com.utp.smartlab.repository.PrestamoRepository;
import com.utp.smartlab.repository.ReservaRepository;
import com.utp.smartlab.repository.SolicitudRechazadaRepository;
import com.utp.smartlab.repository.CategoriaRepository;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.HistorialEstadoEquipoRepository;
import com.utp.smartlab.repository.MantenimientoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final HistorialEstadoEquipoRepository historialRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final ReservaRepository reservaRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final SolicitudRechazadaRepository solicitudRechazadaRepository;
    private final CategoriaRepository categoriaRepository;
    private final PrestamoRepository prestamoRepository;
    private final UsuarioActualService usuarioActualService;

    public EquipoService(EquipoRepository equipoRepository,
                          HistorialEstadoEquipoRepository historialRepository,
                          MantenimientoRepository mantenimientoRepository,
                          ReservaRepository reservaRepository,
                          IncidenciaRepository incidenciaRepository,
                          SolicitudRechazadaRepository solicitudRechazadaRepository,
                          CategoriaRepository categoriaRepository,
                          PrestamoRepository prestamoRepository,
                          UsuarioActualService usuarioActualService) {
        this.equipoRepository = equipoRepository;
        this.historialRepository = historialRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.reservaRepository = reservaRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.solicitudRechazadaRepository = solicitudRechazadaRepository;
        this.categoriaRepository = categoriaRepository;
        this.prestamoRepository = prestamoRepository;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional
    public EquipoResponseDTO crear(EquipoRequestDTO dto) {
        return crear(dto, usuarioActualService.obtener());
    }

    /**
     * Sobrecarga para el seeder, que corre sin un JWT.
     */
    @Transactional
    public EquipoResponseDTO crear(EquipoRequestDTO dto, Usuario responsable) {
        Equipo equipo = Equipo.builder()
                .nombre(dto.getNombre())
                .categoria(buscarCategoria(dto.getCategoriaId()))
                .ubicacion(dto.getUbicacion())
                .estado(EstadoEquipo.DISPONIBLE)
                .contadorUsoHoras(0)
                .build();

        Equipo guardado = equipoRepository.save(equipo);

        registrarHistorial(guardado, null, EstadoEquipo.DISPONIBLE, responsable, "Alta del equipo");

        return mapearAResponseDTO(guardado);
    }

    @Transactional
    public EquipoResponseDTO actualizar(Long id, EquipoRequestDTO dto) {
        Equipo equipo = buscarEntidadPorId(id);
        equipo.setNombre(dto.getNombre());
        equipo.setCategoria(buscarCategoria(dto.getCategoriaId()));
        equipo.setUbicacion(dto.getUbicacion());
        return mapearAResponseDTO(equipoRepository.save(equipo));
    }

    @Transactional
    public void eliminar(Long id) {
        // Regla de negocio: no se elimina un equipo con trazabilidad
        Equipo equipo = buscarEntidadPorId(id);

        List<String> asociados = new java.util.ArrayList<>();
        long reservas = reservaRepository.contarPorEquipoId(id);
        long mantenimientos = mantenimientoRepository.contarPorEquipoId(id);
        long incidencias = incidenciaRepository.contarPorEquipoId(id);
        long solicitudes = solicitudRechazadaRepository.contarPorEquipoId(id);
        if (reservas > 0) asociados.add(reservas + " reserva(s)");
        if (mantenimientos > 0) asociados.add(mantenimientos + " mantenimiento(s)");
        if (incidencias > 0) asociados.add(incidencias + " incidencia(s)");
        if (solicitudes > 0) asociados.add(solicitudes + " solicitud(es) rechazada(s)");

        if (!asociados.isEmpty()) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar el equipo '" + equipo.getNombre() + "': tiene "
                            + String.join(", ", asociados)
                            + " asociado(s), que se conservan por trazabilidad");
        }

        historialRepository.eliminarPorEquipoId(id);
        equipoRepository.delete(equipo);
    }

    public List<EquipoResponseDTO> listar() {
        return equipoRepository.findAll().stream()
                .map(this::mapearAResponseDTO)
                .toList();
    }

    public EquipoResponseDTO obtenerPorId(Long id) {
        return mapearAResponseDTO(buscarEntidadPorId(id));
    }

    public Equipo obtenerEntidadPorId(Long id) {
        return buscarEntidadPorId(id);
    }

    public EstadoEquipo obtenerDisponibilidad(Long id) {
        return buscarEntidadPorId(id).getEstado();
    }

    // Regla de negocio 1: un equipo en mantenimiento no se reserva
    public Equipo validarDisponibleParaReserva(Long equipoId) {
        Equipo equipo = buscarEntidadPorId(equipoId);

        if (equipo.getEstado() == EstadoEquipo.EN_MANTENIMIENTO) {
            throw new EquipoNoDisponibleException(
                    "El equipo '" + equipo.getNombre() + "' no está disponible (estado actual: " + equipo.getEstado() + ")");
        }
        return equipo;
    }

    // Regla de negocio 5: cada cambio de estado queda en el historial
    @Transactional
    public void actualizarEstado(Long equipoId, EstadoEquipo nuevoEstado, Usuario responsable, String motivo) {
        Equipo equipo = buscarEntidadPorId(equipoId);
        EstadoEquipo estadoAnterior = equipo.getEstado();

        equipo.setEstado(nuevoEstado);
        equipoRepository.save(equipo);

        registrarHistorial(equipo, estadoAnterior, nuevoEstado, responsable, motivo);
    }

    // Indicador: recomendación de mantenimiento por horas de uso
    @Transactional
    public void incrementarContadorUso(Long equipoId, long horasUsadas) {
        Equipo equipo = buscarEntidadPorId(equipoId);
        int actual = equipo.getContadorUsoHoras() == null ? 0 : equipo.getContadorUsoHoras();
        equipo.setContadorUsoHoras((int) (actual + horasUsadas));
        equipoRepository.save(equipo);
    }

    // Regla de negocio 5: historial de estados del equipo
    public List<HistorialEstadoResponseDTO> obtenerHistorial(Long equipoId) {
        buscarEntidadPorId(equipoId);

        return historialRepository.buscarPorEquipoIdOrdenado(equipoId).stream()
                .map(h -> {
                    Usuario responsable = h.getUsuario();
                    return HistorialEstadoResponseDTO.builder()
                            .estadoAnterior(h.getEstadoAnterior())
                            .estadoNuevo(h.getEstadoNuevo())
                            .usuarioId(responsable == null ? null : responsable.getId())
                            .usuarioNombre(responsable == null ? null : responsable.getNombre())
                            .motivo(h.getMotivo())
                            .fechaCambio(h.getFechaCambio())
                            .build();
                })
                .toList();
    }

    public List<UtilizacionEquipoResponseDTO> obtenerHistorialUtilizacion(Long equipoId) {
        buscarEntidadPorId(equipoId);

        return prestamoRepository.buscarPorEquipoId(equipoId).stream()
                .map(this::mapearUtilizacion)
                .toList();
    }

    // Indicador: exactitud de disponibilidad
    public DisponibilidadMetricasDTO obtenerMetricasDisponibilidad() {
        List<Mantenimiento> mantenimientosAbiertos = mantenimientoRepository.buscarPorEstado(EstadoMantenimiento.PROGRAMADO);

        long total = mantenimientosAbiertos.size();
        long correctamenteBloqueados = mantenimientosAbiertos.stream()
                .filter(m -> m.getEquipo().getEstado() == EstadoEquipo.EN_MANTENIMIENTO)
                .count();

        double exactitud = total == 0 ? 100.0 : (correctamenteBloqueados * 100.0) / total;

        return DisponibilidadMetricasDTO.builder()
                .totalEquiposEnMantenimiento(total)
                .equiposCorrectamenteBloqueados(correctamenteBloqueados)
                .exactitudDisponibilidad(exactitud)
                .build();
    }

    // Indicador RF26: recomendación por horas de uso y por preventivo con fecha vencida
    public List<EquipoRecomendacionDTO> obtenerRecomendacionesMantenimiento(int umbralHoras) {
        Map<Long, EquipoRecomendacionDTO> porEquipo = new LinkedHashMap<>();

        for (Equipo equipo : equipoRepository.buscarPorUsoMayorOIgualA(umbralHoras)) {
            porEquipo.put(equipo.getId(), EquipoRecomendacionDTO.builder()
                    .equipoId(equipo.getId())
                    .nombre(equipo.getNombre())
                    .contadorUsoHoras(equipo.getContadorUsoHoras())
                    .motivo("Supera el umbral de uso recomendado (" + umbralHoras + " horas)")
                    .build());
        }

        for (Mantenimiento mantenimiento : mantenimientoRepository.buscarPreventivosProgramadosVencidos(LocalDateTime.now())) {
            Equipo equipo = mantenimiento.getEquipo();
            String motivoFecha = "Mantenimiento preventivo programado vencido (" + mantenimiento.getFechaProgramada() + ")";
            EquipoRecomendacionDTO ya = porEquipo.get(equipo.getId());
            if (ya == null) {
                porEquipo.put(equipo.getId(), EquipoRecomendacionDTO.builder()
                        .equipoId(equipo.getId())
                        .nombre(equipo.getNombre())
                        .contadorUsoHoras(equipo.getContadorUsoHoras())
                        .fechaProgramada(mantenimiento.getFechaProgramada())
                        .motivo(motivoFecha)
                        .build());
            } else if (ya.getFechaProgramada() == null) {
                ya.setFechaProgramada(mantenimiento.getFechaProgramada());
                ya.setMotivo(ya.getMotivo() + ". " + motivoFecha);
            }
        }

        return List.copyOf(porEquipo.values());
    }

    // Indicador RF27: horas de uso, frecuencia de préstamos y equipos sub o sobreutilizados
    public UtilizacionDashboardDTO obtenerDashboardUtilizacion(int umbralSobreutilizacionHoras) {
        Map<Long, Long> frecuencias = new HashMap<>();
        for (Object[] fila : prestamoRepository.contarPorEquipo()) {
            frecuencias.put((Long) fila[0], (Long) fila[1]);
        }

        List<UtilizacionEquipoResumenDTO> equipos = new ArrayList<>();
        long subutilizados = 0;
        long normales = 0;
        long sobreutilizados = 0;

        for (Equipo equipo : equipoRepository.findAll()) {
            int horas = equipo.getContadorUsoHoras() == null ? 0 : equipo.getContadorUsoHoras();
            long frecuencia = frecuencias.getOrDefault(equipo.getId(), 0L);
            ClasificacionUtilizacion clasificacion;
            if (horas >= umbralSobreutilizacionHoras) {
                clasificacion = ClasificacionUtilizacion.SOBREUTILIZADO;
                sobreutilizados++;
            } else if (frecuencia == 0) {
                clasificacion = ClasificacionUtilizacion.SUBUTILIZADO;
                subutilizados++;
            } else {
                clasificacion = ClasificacionUtilizacion.NORMAL;
                normales++;
            }
            equipos.add(UtilizacionEquipoResumenDTO.builder()
                    .equipoId(equipo.getId())
                    .nombre(equipo.getNombre())
                    .horasUso(horas)
                    .frecuenciaPrestamos(frecuencia)
                    .clasificacion(clasificacion)
                    .build());
        }

        return UtilizacionDashboardDTO.builder()
                .umbralSobreutilizacionHoras(umbralSobreutilizacionHoras)
                .totalEquipos(equipos.size())
                .subutilizados(subutilizados)
                .normales(normales)
                .sobreutilizados(sobreutilizados)
                .equipos(equipos)
                .build();
    }

    private void registrarHistorial(Equipo equipo, EstadoEquipo estadoAnterior, EstadoEquipo estadoNuevo,
                                    Usuario responsable, String motivo) {
        HistorialEstadoEquipo registro = HistorialEstadoEquipo.builder()
                .equipo(equipo)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .usuario(responsable)
                .motivo(motivo)
                .fechaCambio(LocalDateTime.now())
                .build();
        historialRepository.save(registro);
    }

    private UtilizacionEquipoResponseDTO mapearUtilizacion(Prestamo prestamo) {
        Long horas = null;
        if (prestamo.getFechaDevolucionReal() != null) {
            horas = Math.max(Duration.between(prestamo.getFechaEntrega(), prestamo.getFechaDevolucionReal()).toHours(), 0);
        }
        return UtilizacionEquipoResponseDTO.builder()
                .prestamoId(prestamo.getId())
                .reservaId(prestamo.getReserva().getId())
                .usuarioNombre(prestamo.getReserva().getUsuario().getNombre())
                .fechaEntrega(prestamo.getFechaEntrega())
                .fechaLimite(prestamo.getFechaLimite())
                .fechaDevolucionReal(prestamo.getFechaDevolucionReal())
                .horasUso(horas)
                .estado(prestamo.getEstado())
                .build();
    }

    private Equipo buscarEntidadPorId(Long id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipo con id " + id));
    }

    private Categoria buscarCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la categoría con id " + categoriaId));
    }

    private EquipoResponseDTO mapearAResponseDTO(Equipo equipo) {
        Categoria categoria = equipo.getCategoria();
        return EquipoResponseDTO.builder()
                .id(equipo.getId())
                .nombre(equipo.getNombre())
                .categoriaId(categoria.getId())
                .categoriaNombre(categoria.getNombre())
                .ubicacion(equipo.getUbicacion())
                .estado(equipo.getEstado())
                .contadorUsoHoras(equipo.getContadorUsoHoras())
                .build();
    }
}
