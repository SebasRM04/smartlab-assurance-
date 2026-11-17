package com.utp.smartlab.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.repository.EquipoRepository;
import com.utp.smartlab.repository.HistorialEstadoEquipoRepository;
import com.utp.smartlab.repository.IncidenciaRepository;
import com.utp.smartlab.repository.MantenimientoRepository;
import com.utp.smartlab.repository.PrestamoRepository;
import com.utp.smartlab.repository.ReservaRepository;
import com.utp.smartlab.repository.SolicitudRechazadaRepository;
import com.utp.smartlab.repository.UsuarioRepository;
import com.utp.smartlab.support.ApiDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Arranca la aplicación con la carga masiva activada, sobre su propia base en memoria
 * (distinta de la que usan las demás pruebas), y revisa lo que quedó guardado.
 */
@SpringBootTest(properties = {
        "app.seed.datos-masivos=true",
        "spring.datasource.url=jdbc:h2:mem:smartlabdb-masivo;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class DatosMasivosSeederTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatosMasivosSeeder seeder;

    @Autowired
    private EquipoRepository equipoRepository;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private PrestamoRepository prestamoRepository;
    @Autowired
    private MantenimientoRepository mantenimientoRepository;
    @Autowired
    private SolicitudRechazadaRepository solicitudRechazadaRepository;
    @Autowired
    private IncidenciaRepository incidenciaRepository;
    @Autowired
    private HistorialEstadoEquipoRepository historialRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private JsonNode obtener(String url, String token) throws Exception {
        String cuerpo = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo);
    }

    private String tokenAdmin() throws Exception {
        return new ApiDePrueba(mockMvc, objectMapper).tokenAdmin();
    }

    @Test
    void cargaLos180EquiposYMasDe10000Registros() {
        assertEquals(180, equipoRepository.count(), "equipos");
        assertTrue(reservaRepository.count() >= 5000, "reservas: " + reservaRepository.count());

        long total = equipoRepository.count() + reservaRepository.count() + prestamoRepository.count()
                + mantenimientoRepository.count() + solicitudRechazadaRepository.count()
                + incidenciaRepository.count() + historialRepository.count() + usuarioRepository.count();
        assertTrue(total >= 10000, "registros en total: " + total);
    }

    @Test
    void siSeEjecutaDeNuevo_noDuplicaNada() {
        long equipos = equipoRepository.count();
        long reservas = reservaRepository.count();

        seeder.run();

        assertEquals(equipos, equipoRepository.count(), "equipos");
        assertEquals(reservas, reservaRepository.count(), "reservas");
    }

    @Test
    void losIndicadoresDanValoresCoherentes() throws Exception {
        String token = tokenAdmin();

        double noShow = obtener("/api/reservas/reportes/no-show", token).get("tasaNoShow").asDouble();
        assertTrue(noShow > 5 && noShow < 15, "no-show " + noShow);

        double conflicto = obtener("/api/reservas/reportes/conflictos", token).get("tasaConflicto").asDouble();
        assertTrue(conflicto > 4 && conflicto < 10, "conflicto " + conflicto);

        JsonNode mora = obtener("/api/prestamos/reportes/mora", token);
        assertTrue(mora.get("tasaMora").asDouble() > 8 && mora.get("tasaMora").asDouble() < 20,
                "mora " + mora.get("tasaMora").asDouble());
        assertTrue(mora.get("vencidos").asLong() >= 5, "vencidos " + mora.get("vencidos").asLong());

        double exactitud = obtener("/api/equipos/reportes/disponibilidad", token)
                .get("exactitudDisponibilidad").asDouble();
        assertEquals(100.0, exactitud, "exactitud de disponibilidad");

        double tardio = obtener("/api/mantenimientos/reportes/mantenimiento", token).get("tasaTardio").asDouble();
        assertTrue(tardio > 15 && tardio < 35, "mantenimiento tardío " + tardio);

        JsonNode utilizacion = obtener("/api/equipos/reportes/utilizacion", token);
        assertEquals(180, utilizacion.get("totalEquipos").asInt(), "equipos en el dashboard");
        assertTrue(utilizacion.get("sobreutilizados").asLong() >= 10, "sobreutilizados");
        assertTrue(utilizacion.get("subutilizados").asLong() >= 5, "subutilizados");

        JsonNode recomendaciones = obtener("/api/equipos/recomendaciones-mantenimiento", token);
        assertTrue(recomendaciones.size() >= 10, "recomendaciones de mantenimiento: " + recomendaciones.size());
    }

    @Test
    void laPaginacionFuncionaConMilesDeRegistros() throws Exception {
        String token = tokenAdmin();

        JsonNode reservas = obtener("/api/reservas/pagina?pagina=0&tamanio=20", token);
        assertEquals(20, reservas.get("contenido").size(), "filas de la página");
        assertTrue(reservas.get("totalElementos").asLong() >= 5000, "total de reservas");
        assertTrue(reservas.get("totalPaginas").asInt() >= 250, "total de páginas");

        JsonNode prestamos = obtener("/api/prestamos/pagina?estado=DEVUELTO&tamanio=50", token);
        assertEquals(50, prestamos.get("contenido").size(), "filas de préstamos");
        assertTrue(prestamos.get("totalElementos").asLong() >= 3000, "total de préstamos devueltos");

        JsonNode mantenimientos = obtener("/api/mantenimientos/pagina?estado=PROGRAMADO&tamanio=100", token);
        assertTrue(mantenimientos.get("totalElementos").asLong() >= 10, "mantenimientos abiertos");
    }

    @Test
    void losUsuariosGeneradosPuedenIniciarSesion_yElEstudianteSoloVeSusReservas() throws Exception {
        ApiDePrueba api = new ApiDePrueba(mockMvc, objectMapper);
        api.login("tecnico@smartlab.com", "Demo123!");
        api.login("docente@smartlab.com", "Demo123!");
        api.login("coordinador@smartlab.com", "Demo123!");
        String tokenEstudiante = api.login("estudiante05@smartlab.com", "Demo123!");

        JsonNode pagina = obtener("/api/reservas/pagina?tamanio=100", tokenEstudiante);
        assertTrue(pagina.get("totalElementos").asLong() >= 20, "reservas del estudiante");

        long usuarioId = pagina.get("contenido").get(0).get("usuarioId").asLong();
        for (JsonNode reserva : pagina.get("contenido")) {
            assertEquals(usuarioId, reserva.get("usuarioId").asLong(), "todas son del mismo estudiante");
        }
    }
}
