package com.utp.smartlab.config;

import com.utp.smartlab.config.masivo.PlanDatosMasivos;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.EquipoPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.HistorialPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.IncidenciaPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.MantenimientoPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.Plan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.PrestamoPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.RechazoPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.ReservaPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.UsuarioPlan;
import com.utp.smartlab.entity.Categoria;
import com.utp.smartlab.entity.Equipo;
import com.utp.smartlab.entity.HistorialEstadoEquipo;
import com.utp.smartlab.entity.Incidencia;
import com.utp.smartlab.entity.Mantenimiento;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.Prestamo;
import com.utp.smartlab.entity.Reserva;
import com.utp.smartlab.entity.Rol;
import com.utp.smartlab.entity.SolicitudRechazada;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.repository.CategoriaRepository;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.HistorialEstadoEquipoRepository;
import com.utp.smartlab.repository.IncidenciaRepository;
import com.utp.smartlab.repository.MantenimientoRepository;
import com.utp.smartlab.repository.PrestamoRepository;
import com.utp.smartlab.repository.ReservaRepository;
import com.utp.smartlab.repository.RolRepository;
import com.utp.smartlab.repository.SolicitudRechazadaRepository;
import com.utp.smartlab.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Carga un escenario grande (más de 10 000 registros) para probar el frontend y medir los indicadores.
 * Se activa con {@code app.seed.datos-masivos=true} y se ejecuta una sola vez: si ya hay 180 equipos o más,
 * no hace nada. Escribe directo en los repositorios (no pasa por los servicios), así que no deja auditoría.
 * Todos los usuarios generados usan la contraseña {@code Demo123!}.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "app.seed.datos-masivos", havingValue = "true")
public class DatosMasivosSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosMasivosSeeder.class);

    private static final String PASSWORD_DEMO = "Demo123!";
    private static final long SEMILLA = 2026L;
    // El caso describe un laboratorio con 180 equipos
    static final int TOTAL_EQUIPOS = 180;

    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EquipoRepository equipoRepository;
    private final HistorialEstadoEquipoRepository historialRepository;
    private final ReservaRepository reservaRepository;
    private final PrestamoRepository prestamoRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final SolicitudRechazadaRepository solicitudRechazadaRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    public DatosMasivosSeeder(CategoriaRepository categoriaRepository,
                              UsuarioRepository usuarioRepository,
                              RolRepository rolRepository,
                              EquipoRepository equipoRepository,
                              HistorialEstadoEquipoRepository historialRepository,
                              ReservaRepository reservaRepository,
                              PrestamoRepository prestamoRepository,
                              MantenimientoRepository mantenimientoRepository,
                              SolicitudRechazadaRepository solicitudRechazadaRepository,
                              IncidenciaRepository incidenciaRepository,
                              PasswordEncoder passwordEncoder,
                              PlatformTransactionManager transactionManager) {
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.equipoRepository = equipoRepository;
        this.historialRepository = historialRepository;
        this.reservaRepository = reservaRepository;
        this.prestamoRepository = prestamoRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.solicitudRechazadaRepository = solicitudRechazadaRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionManager = transactionManager;
    }

    @Override
    public void run(String... args) {
        long existentes = equipoRepository.count();
        if (existentes >= TOTAL_EQUIPOS) {
            log.info("Datos masivos: ya hay {} equipos, no se vuelven a cargar", existentes);
            return;
        }

        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        Plan plan = PlanDatosMasivos.generar(ahora, SEMILLA, TOTAL_EQUIPOS - (int) existentes);

        log.info("Datos masivos: cargando {} registros, puede tardar unos segundos...", plan.totalRegistros());
        long inicio = System.currentTimeMillis();
        // Todo en una sola transacción: si algo falla, no queda una carga a medias
        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> cargar(plan));
        log.info("Datos masivos: listo en {} ms", System.currentTimeMillis() - inicio);
    }

    private void cargar(Plan plan) {
        List<Categoria> categorias = cargarCategorias(plan);
        List<Usuario> usuarios = cargarUsuarios(plan);
        List<Equipo> equipos = cargarEquipos(plan, categorias);

        List<HistorialEstadoEquipo> historiales = new ArrayList<>();
        for (HistorialPlan h : plan.historiales()) {
            historiales.add(HistorialEstadoEquipo.builder()
                    .equipo(equipos.get(h.equipo()))
                    .estadoAnterior(h.anterior())
                    .estadoNuevo(h.nuevo())
                    .usuario(usuarios.get(h.usuario()))
                    .motivo(h.motivo())
                    .fechaCambio(h.fecha())
                    .build());
        }
        historialRepository.saveAll(historiales);

        List<Reserva> reservas = new ArrayList<>();
        for (ReservaPlan r : plan.reservas()) {
            reservas.add(Reserva.builder()
                    .equipo(equipos.get(r.equipo()))
                    .usuario(usuarios.get(r.usuario()))
                    .fechaInicio(r.inicio())
                    .fechaFin(r.fin())
                    .estado(r.estado())
                    .build());
        }
        reservas = reservaRepository.saveAll(reservas);

        List<Prestamo> prestamos = new ArrayList<>();
        for (PrestamoPlan p : plan.prestamos()) {
            prestamos.add(Prestamo.builder()
                    .reserva(reservas.get(p.reserva()))
                    .fechaEntrega(p.entrega())
                    .fechaLimite(p.limite())
                    .fechaDevolucionReal(p.devolucion())
                    .estado(p.estado())
                    .build());
        }
        prestamoRepository.saveAll(prestamos);

        List<Mantenimiento> mantenimientos = new ArrayList<>();
        for (MantenimientoPlan m : plan.mantenimientos()) {
            mantenimientos.add(Mantenimiento.builder()
                    .equipo(equipos.get(m.equipo()))
                    .usuario(usuarios.get(m.usuario()))
                    .tipo(m.tipo())
                    .fechaProgramada(m.programada())
                    .fechaEjecucion(m.ejecucion())
                    .estado(m.estado())
                    .build());
        }
        mantenimientoRepository.saveAll(mantenimientos);

        List<SolicitudRechazada> rechazos = new ArrayList<>();
        for (RechazoPlan x : plan.rechazos()) {
            rechazos.add(SolicitudRechazada.builder()
                    .equipo(equipos.get(x.equipo()))
                    .fechaInicioSolicitada(x.inicio())
                    .fechaFinSolicitada(x.fin())
                    .fechaIntento(x.intento())
                    .build());
        }
        solicitudRechazadaRepository.saveAll(rechazos);

        List<Incidencia> incidencias = new ArrayList<>();
        for (IncidenciaPlan i : plan.incidencias()) {
            incidencias.add(Incidencia.builder()
                    .equipo(equipos.get(i.equipo()))
                    .usuario(usuarios.get(i.usuario()))
                    .descripcion(i.descripcion())
                    .estado(i.estado())
                    .fechaRegistro(i.fecha())
                    .build());
        }
        incidenciaRepository.saveAll(incidencias);
    }

    private List<Categoria> cargarCategorias(Plan plan) {
        List<Categoria> categorias = new ArrayList<>();
        for (String nombre : plan.categorias()) {
            categorias.add(categoriaRepository.findByNombreIgnoreCase(nombre)
                    .orElseGet(() -> categoriaRepository.save(Categoria.builder().nombre(nombre).build())));
        }
        return categorias;
    }

    private List<Usuario> cargarUsuarios(Plan plan) {
        Map<NombreRol, Rol> roles = new EnumMap<>(NombreRol.class);
        for (NombreRol nombre : NombreRol.values()) {
            roles.put(nombre, rolRepository.findByNombre(nombre)
                    .orElseThrow(() -> new IllegalStateException("No existe el rol " + nombre)));
        }

        // Un solo hash para todos: BCrypt es lento a propósito y no hace falta repetirlo 50 veces
        String hash = passwordEncoder.encode(PASSWORD_DEMO);

        List<Usuario> usuarios = new ArrayList<>();
        for (UsuarioPlan u : plan.usuarios()) {
            usuarios.add(usuarioRepository.buscarPorEmail(u.email())
                    .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                            .nombre(u.nombre())
                            .email(u.email())
                            .passwordHash(hash)
                            .rol(roles.get(u.rol()))
                            .build())));
        }
        return usuarios;
    }

    private List<Equipo> cargarEquipos(Plan plan, List<Categoria> categorias) {
        List<Equipo> equipos = new ArrayList<>();
        for (EquipoPlan e : plan.equipos()) {
            equipos.add(Equipo.builder()
                    .nombre(e.nombre())
                    .categoria(categorias.get(e.categoria()))
                    .ubicacion(e.ubicacion())
                    .estado(e.estado())
                    .contadorUsoHoras(e.horasUso())
                    .build());
        }
        return equipoRepository.saveAll(equipos);
    }
}
