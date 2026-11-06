package com.utp.smartlab.config;

import com.utp.smartlab.dto.CategoriaRequestDTO;
import com.utp.smartlab.dto.EquipoRequestDTO;
import com.utp.smartlab.dto.EquipoResponseDTO;
import com.utp.smartlab.dto.IncidenciaRequestDTO;
import com.utp.smartlab.dto.MantenimientoRequestDTO;
import com.utp.smartlab.dto.MantenimientoResponseDTO;
import com.utp.smartlab.dto.PrestamoCheckoutRequestDTO;
import com.utp.smartlab.dto.PrestamoResponseDTO;
import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.dto.ReservaResponseDTO;
import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.TipoMantenimiento;
import com.utp.smartlab.entity.Usuario;
import com.utp.smartlab.repository.UsuarioRepository;
import com.utp.smartlab.service.CategoriaService;
import com.utp.smartlab.service.EquipoService;
import com.utp.smartlab.service.IncidenciaService;
import com.utp.smartlab.service.MantenimientoService;
import com.utp.smartlab.service.PrestamoService;
import com.utp.smartlab.service.ReservaService;
import com.utp.smartlab.service.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.datos-demo", havingValue = "true")
public class DatosPruebaSeeder implements CommandLineRunner {

    private static final String PASSWORD_DEMO = "Demo123!";

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final EquipoService equipoService;
    private final CategoriaService categoriaService;
    private final ReservaService reservaService;
    private final PrestamoService prestamoService;
    private final MantenimientoService mantenimientoService;
    private final IncidenciaService incidenciaService;

    public DatosPruebaSeeder(UsuarioService usuarioService,
                             UsuarioRepository usuarioRepository,
                             EquipoService equipoService,
                             CategoriaService categoriaService,
                             ReservaService reservaService,
                             PrestamoService prestamoService,
                             MantenimientoService mantenimientoService,
                             IncidenciaService incidenciaService) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.equipoService = equipoService;
        this.categoriaService = categoriaService;
        this.reservaService = reservaService;
        this.prestamoService = prestamoService;
        this.mantenimientoService = mantenimientoService;
        this.incidenciaService = incidenciaService;
    }

    @Override
    public void run(String... args) {
        sembrarUsuarios();
        if (equipoService.listar().isEmpty()) {
            sembrarEscenario();
        }
    }

    private void sembrarUsuarios() {
        crearUsuarioSiNoExiste("Carla Estudiante", "estudiante@smartlab.com", NombreRol.ESTUDIANTE);
        crearUsuarioSiNoExiste("Tomás Técnico", "tecnico@smartlab.com", NombreRol.TECNICO);
        crearUsuarioSiNoExiste("Diana Docente", "docente@smartlab.com", NombreRol.DOCENTE);
        crearUsuarioSiNoExiste("Camilo Coordinador", "coordinador@smartlab.com", NombreRol.COORDINADOR);
    }

    private void crearUsuarioSiNoExiste(String nombre, String email, NombreRol rol) {
        if (!usuarioRepository.existsByEmail(email)) {
            usuarioService.registrar(new UsuarioRequestDTO(nombre, email, PASSWORD_DEMO, rol));
        }
    }

    private void sembrarEscenario() {
        Usuario estudiante = buscarUsuario("estudiante@smartlab.com");
        Usuario tecnico = buscarUsuario("tecnico@smartlab.com");
        Usuario coordinador = buscarUsuario("coordinador@smartlab.com");

        Long optico = categoria("Óptico");
        Long electronico = categoria("Electrónico");
        Long fabricacion = categoria("Fabricación");
        Long quimica = categoria("Química");

        EquipoResponseDTO microscopio = crearEquipo("Microscopio óptico A", optico, "Laboratorio 1", coordinador);
        EquipoResponseDTO osciloscopio = crearEquipo("Osciloscopio digital", electronico, "Laboratorio 2", coordinador);
        EquipoResponseDTO impresora3d = crearEquipo("Impresora 3D Prusa", fabricacion, "Taller 1", coordinador);
        EquipoResponseDTO centrifuga = crearEquipo("Centrífuga de mesa", quimica, "Laboratorio 3", coordinador);
        EquipoResponseDTO multimetro = crearEquipo("Multímetro de precisión", electronico, "Laboratorio 2", coordinador);
        crearEquipo("Espectrómetro UV", optico, "Laboratorio 1", coordinador);
        crearEquipo("Fuente de poder programable", electronico, "Laboratorio 2", coordinador);
        crearEquipo("Balanza analítica", quimica, "Laboratorio 3", coordinador);

        equipoService.incrementarContadorUso(impresora3d.getId(), 120);

        LocalDateTime manana = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        ReservaResponseDTO reservaA = reservaService.crear(
                new ReservaRequestDTO(microscopio.getId(), manana, manana.plusHours(2)), estudiante);
        ReservaResponseDTO reservaB = reservaService.crear(
                new ReservaRequestDTO(osciloscopio.getId(), manana.plusHours(5), manana.plusHours(7)), estudiante);
        reservaService.crear(
                new ReservaRequestDTO(microscopio.getId(), manana.plusDays(1), manana.plusDays(1).plusHours(2)), estudiante);

        PrestamoResponseDTO prestamoMoroso = prestamoService.checkout(
                new PrestamoCheckoutRequestDTO(reservaA.getId(), LocalDateTime.now().minusHours(1)));
        prestamoService.checkin(prestamoMoroso.getId());

        prestamoService.checkout(
                new PrestamoCheckoutRequestDTO(reservaB.getId(), LocalDateTime.now().plusHours(4)));

        mantenimientoService.programar(new MantenimientoRequestDTO(
                centrifuga.getId(), TipoMantenimiento.CORRECTIVO, LocalDateTime.now().plusDays(1)), tecnico);

        MantenimientoResponseDTO preventivo = mantenimientoService.programar(new MantenimientoRequestDTO(
                multimetro.getId(), TipoMantenimiento.PREVENTIVO, LocalDateTime.now().minusDays(1)), tecnico);
        mantenimientoService.cerrar(preventivo.getId(), tecnico);

        incidenciaService.registrar(new IncidenciaRequestDTO(
                osciloscopio.getId(), "La pantalla presenta líneas verticales al encender", null), estudiante);
        incidenciaService.registrar(new IncidenciaRequestDTO(
                centrifuga.getId(), "Vibración excesiva y ruido anormal durante el giro", null), estudiante);
    }

    private Long categoria(String nombre) {
        return categoriaService.crear(new CategoriaRequestDTO(nombre)).getId();
    }

    private EquipoResponseDTO crearEquipo(String nombre, Long categoriaId, String ubicacion, Usuario responsable) {
        return equipoService.crear(new EquipoRequestDTO(nombre, categoriaId, ubicacion), responsable);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException("No existe el usuario de prueba " + email));
    }
}
