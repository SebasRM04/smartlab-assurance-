package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.MantenimientoRequestDTO;
import com.utp.smartlab.dto.PrestamoCheckoutRequestDTO;
import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.TipoMantenimiento;
import com.utp.smartlab.support.ApiDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Listados por página (/pagina) de reservas, préstamos y mantenimientos. Cada prueba crea sus propios
 * equipos y filtra por ellos, así que no depende de los datos que dejaron otras pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaginacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ApiDePrueba api;
    private String token;
    private long categoriaId;

    @BeforeEach
    void setUp() throws Exception {
        api = new ApiDePrueba(mockMvc, objectMapper);
        token = api.tokenAdmin();
        categoriaId = api.crearCategoria(token, "Cat-pagina-" + System.nanoTime());
    }

    private JsonNode obtener(String url, String tokenUsuario) throws Exception {
        String cuerpo = mockMvc.perform(api.auth(get(url), tokenUsuario))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo);
    }

    private long reservar(String tokenUsuario, long equipoId, int diasAdelante) throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusDays(diasAdelante).withSecond(0).withNano(0);
        ReservaRequestDTO dto = new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(2));
        String cuerpo = mockMvc.perform(api.auth(post("/api/reservas"), tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).get("id").asLong();
    }

    private long reservarEn(String tokenUsuario, long equipoId, LocalDateTime inicio) throws Exception {
        ReservaRequestDTO dto = new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(1));
        String cuerpo = mockMvc.perform(api.auth(post("/api/reservas"), tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).get("id").asLong();
    }

    @Test
    void reservas_devuelveLaPaginaPedida_conLosTotales() throws Exception {
        long equipoId = api.crearEquipo(token, "Equipo paginado", categoriaId, "Lab 1");
        for (int dia = 1; dia <= 5; dia++) {
            reservar(token, equipoId, dia);
        }

        JsonNode primera = obtener("/api/reservas/pagina?equipoId=" + equipoId + "&pagina=0&tamanio=2", token);
        assertEquals(2, primera.get("contenido").size(), "elementos de la primera página");
        assertEquals(0, primera.get("pagina").asInt(), "número de página");
        assertEquals(2, primera.get("tamanio").asInt(), "tamaño");
        assertEquals(5, primera.get("totalElementos").asLong(), "total de elementos");
        assertEquals(3, primera.get("totalPaginas").asInt(), "total de páginas");
        assertTrue(primera.get("primera").asBoolean(), "es la primera");
        assertTrue(!primera.get("ultima").asBoolean(), "no es la última");

        JsonNode ultima = obtener("/api/reservas/pagina?equipoId=" + equipoId + "&pagina=2&tamanio=2", token);
        assertEquals(1, ultima.get("contenido").size(), "elementos de la última página");
        assertTrue(ultima.get("ultima").asBoolean(), "es la última");
    }

    @Test
    void reservas_ordenaDeLaMasRecienteALaMasAntigua() throws Exception {
        long equipoId = api.crearEquipo(token, "Equipo orden", categoriaId, "Lab 1");
        reservar(token, equipoId, 2);
        reservar(token, equipoId, 6);
        reservar(token, equipoId, 4);

        JsonNode pagina = obtener("/api/reservas/pagina?equipoId=" + equipoId, token);
        String anterior = null;
        for (JsonNode reserva : pagina.get("contenido")) {
            String inicio = reserva.get("fechaInicio").asText();
            if (anterior != null) {
                assertTrue(anterior.compareTo(inicio) > 0, "orden descendente: " + anterior + " antes de " + inicio);
            }
            anterior = inicio;
        }
    }

    @Test
    void reservas_filtraPorEstado() throws Exception {
        long equipoId = api.crearEquipo(token, "Equipo filtro estado", categoriaId, "Lab 1");
        reservar(token, equipoId, 1);
        long aCancelar = reservar(token, equipoId, 3);
        mockMvc.perform(api.auth(patch("/api/reservas/" + aCancelar + "/estado"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk());

        JsonNode canceladas = obtener("/api/reservas/pagina?equipoId=" + equipoId + "&estado=CANCELADA", token);
        assertEquals(1, canceladas.get("totalElementos").asLong(), "canceladas");
        assertEquals("CANCELADA", canceladas.get("contenido").get(0).get("estado").asText(), "estado devuelto");

        JsonNode todas = obtener("/api/reservas/pagina?equipoId=" + equipoId, token);
        assertEquals(2, todas.get("totalElementos").asLong(), "sin filtro de estado");
    }

    @Test
    void reservas_elEstudianteSoloVeLasSuyas() throws Exception {
        long equipoId = api.crearEquipo(token, "Equipo estudiante", categoriaId, "Lab 1");
        String sufijo = String.valueOf(System.nanoTime());
        String tokenEstudiante = api.crearUsuarioYToken(token, "Estudiante Pagina", "est-pag-" + sufijo + "@smartlab.com",
                "Clave123!", NombreRol.ESTUDIANTE);

        reservar(tokenEstudiante, equipoId, 1);
        reservar(tokenEstudiante, equipoId, 2);
        reservar(token, equipoId, 3);

        JsonNode pagina = obtener("/api/reservas/pagina?pagina=0&tamanio=50", tokenEstudiante);
        assertEquals(2, pagina.get("totalElementos").asLong(), "el estudiante ve solo sus 2 reservas");
        assertEquals(2, pagina.get("contenido").size(), "contenido");
    }

    @Test
    void reservas_limitaElTamanioMaximo_yAceptaValoresFueraDeRango() throws Exception {
        JsonNode grande = obtener("/api/reservas/pagina?tamanio=100000", token);
        assertEquals(100, grande.get("tamanio").asInt(), "el tamaño se limita a 100");

        JsonNode negativos = obtener("/api/reservas/pagina?pagina=-3&tamanio=0", token);
        assertEquals(0, negativos.get("pagina").asInt(), "la página mínima es 0");
        assertEquals(1, negativos.get("tamanio").asInt(), "el tamaño mínimo es 1");
    }

    @Test
    void reservas_devuelve400_cuandoElEstadoNoExiste() throws Exception {
        mockMvc.perform(api.auth(get("/api/reservas/pagina?estado=INVENTADO"), token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservas_devuelve401_sinToken() throws Exception {
        mockMvc.perform(get("/api/reservas/pagina"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pendientesEntrega_incluyeLasReservasProximas_yNoLasLejanas() throws Exception {
        long equipoId = api.crearEquipo(token, "Equipo pendientes", categoriaId, "Lab 2");

        LocalDateTime pronto = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        long cercana = reservarEn(token, equipoId, pronto);
        long lejana = reservarEn(token, equipoId, LocalDateTime.now().plusDays(10).withSecond(0).withNano(0));

        JsonNode pendientes = obtener("/api/reservas/pendientes-entrega?horas=6", token);
        boolean incluyeCercana = false;
        for (JsonNode reserva : pendientes) {
            long id = reserva.get("id").asLong();
            assertTrue(id != lejana, "la reserva de dentro de 10 días no debe aparecer");
            if (id == cercana) {
                incluyeCercana = true;
            }
        }
        assertTrue(incluyeCercana, "la reserva de dentro de 2 horas debe aparecer");
    }

    @Test
    void pendientesEntrega_elEstudianteNoPuedeVerlas() throws Exception {
        String tokenEstudiante = api.crearUsuarioYToken(token, "Estudiante Pendientes",
                "est-pend-" + System.nanoTime() + "@smartlab.com", "Clave123!", NombreRol.ESTUDIANTE);
        mockMvc.perform(api.auth(get("/api/reservas/pendientes-entrega"), tokenEstudiante))
                .andExpect(status().isForbidden());
    }

    @Test
    void prestamos_paginaYFiltraPorEstado() throws Exception {
        long equipoId = api.crearEquipo(token, "Equipo préstamo paginado", categoriaId, "Lab 2");
        long reservaId = reservar(token, equipoId, 1);
        PrestamoCheckoutRequestDTO dto = new PrestamoCheckoutRequestDTO(reservaId, LocalDateTime.now().plusDays(5));
        mockMvc.perform(api.auth(post("/api/prestamos/checkout"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        JsonNode activos = obtener("/api/prestamos/pagina?estado=ACTIVO&tamanio=1", token);
        assertEquals(1, activos.get("contenido").size(), "una fila por página");
        assertTrue(activos.get("totalElementos").asLong() >= 1, "al menos el préstamo recién creado");
        assertEquals(activos.get("totalElementos").asLong(), activos.get("totalPaginas").asLong(),
                "con tamaño 1 hay una página por préstamo");
        for (JsonNode prestamo : activos.get("contenido")) {
            assertEquals("ACTIVO", prestamo.get("estado").asText(), "estado");
        }

        JsonNode todos = obtener("/api/prestamos/pagina", token);
        assertTrue(todos.get("totalElementos").asLong() >= activos.get("totalElementos").asLong(),
                "sin filtro hay al menos tantos como los activos");
    }

    @Test
    void prestamos_elEstudianteNoPuedeListarlos() throws Exception {
        String tokenEstudiante = api.crearUsuarioYToken(token, "Estudiante Prestamos",
                "est-prest-" + System.nanoTime() + "@smartlab.com", "Clave123!", NombreRol.ESTUDIANTE);
        mockMvc.perform(api.auth(get("/api/prestamos/pagina"), tokenEstudiante))
                .andExpect(status().isForbidden());
    }

    @Test
    void mantenimientos_paginaYFiltraPorTipoYEquipo() throws Exception {
        long equipoId = api.crearEquipo(token, "Equipo mantenimiento paginado", categoriaId, "Lab 4");
        for (int dia = 1; dia <= 3; dia++) {
            MantenimientoRequestDTO dto = new MantenimientoRequestDTO(
                    equipoId, dia == 1 ? TipoMantenimiento.CORRECTIVO : TipoMantenimiento.PREVENTIVO,
                    LocalDateTime.now().plusDays(dia));
            mockMvc.perform(api.auth(post("/api/mantenimientos"), token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        JsonNode todos = obtener("/api/mantenimientos/pagina?equipoId=" + equipoId + "&tamanio=2", token);
        assertEquals(3, todos.get("totalElementos").asLong(), "mantenimientos del equipo");
        assertEquals(2, todos.get("totalPaginas").asInt(), "páginas");
        assertEquals(2, todos.get("contenido").size(), "primera página");

        JsonNode preventivos = obtener("/api/mantenimientos/pagina?equipoId=" + equipoId + "&tipo=PREVENTIVO", token);
        assertEquals(2, preventivos.get("totalElementos").asLong(), "preventivos");

        JsonNode cerrados = obtener("/api/mantenimientos/pagina?equipoId=" + equipoId + "&estado=CERRADO", token);
        assertEquals(0, cerrados.get("totalElementos").asLong(), "ninguno cerrado todavía");
    }

    @Test
    void mantenimientos_devuelve400_conUnTipoInvalido() throws Exception {
        mockMvc.perform(api.auth(get("/api/mantenimientos/pagina?tipo=OTRO"), token))
                .andExpect(status().isBadRequest());
    }
}
