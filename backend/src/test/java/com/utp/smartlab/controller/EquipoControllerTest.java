package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.EquipoRequestDTO;
import com.utp.smartlab.support.ApiDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EquipoControllerTest {

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
        categoriaId = api.crearCategoria(token, "Cat-equipo-" + System.nanoTime());
    }

    @Test
    void crear_devuelve201_conEstadoDisponiblePorDefecto() throws Exception {
        EquipoRequestDTO dto = new EquipoRequestDTO("Microscopio A", categoriaId, "Laboratorio 3");

        mockMvc.perform(api.auth(post("/api/equipos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Microscopio A"))
                .andExpect(jsonPath("$.categoriaId").value(categoriaId))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void obtenerPorId_devuelve404_cuandoNoExiste() throws Exception {
        mockMvc.perform(api.auth(get("/api/equipos/999999"), token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void crear_devuelve400_cuandoFaltaElNombre() throws Exception {
        EquipoRequestDTO dtoInvalido = new EquipoRequestDTO(null, categoriaId, "Laboratorio 3");

        mockMvc.perform(api.auth(post("/api/equipos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerHistorial_incluyeElEstadoInicial_alCrearUnEquipo() throws Exception {

        long id = api.crearEquipo(token, "Equipo con historial", categoriaId, "Lab 5");

        mockMvc.perform(api.auth(get("/api/equipos/" + id + "/historial"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoAnterior").value(nullValue()))
                .andExpect(jsonPath("$[0].estadoNuevo").value("DISPONIBLE"))
                .andExpect(jsonPath("$[0].motivo").value("Alta del equipo"))
                .andExpect(jsonPath("$[0].usuarioNombre").value("Administrador SmartLab"))
                .andExpect(jsonPath("$[0].fechaCambio").exists());
    }

    @Test
    void obtenerHistorialUtilizacion_devuelveListaVacia_cuandoElEquipoNoTienePrestamos() throws Exception {
        long id = api.crearEquipo(token, "Equipo sin prestamos", categoriaId, "Lab 7");

        mockMvc.perform(api.auth(get("/api/equipos/" + id + "/utilizacion"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void obtenerMetricasDisponibilidad_devuelve200() throws Exception {
        mockMvc.perform(api.auth(get("/api/equipos/reportes/disponibilidad"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exactitudDisponibilidad").exists());
    }

    @Test
    void obtenerRecomendacionesMantenimiento_devuelve200_cuandoNingunEquipoNuevoSuperaElUmbral() throws Exception {

        long id = api.crearEquipo(token, "Equipo sin uso " + System.nanoTime(), categoriaId, "Lab 6");

        mockMvc.perform(api.auth(get("/api/equipos/recomendaciones-mantenimiento"), token)
                        .param("umbralHoras", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.equipoId == " + id + ")]").isEmpty());
    }

    @Test
    void obtenerDashboardUtilizacion_clasificaComoSubutilizado_unEquipoSinPrestamos() throws Exception {
        long id = api.crearEquipo(token, "Equipo ocioso " + System.nanoTime(), categoriaId, "Lab 9");

        mockMvc.perform(api.auth(get("/api/equipos/reportes/utilizacion"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.umbralSobreutilizacionHoras").value(100))
                .andExpect(jsonPath("$.equipos[?(@.equipoId == " + id + ")].clasificacion").value(hasItem("SUBUTILIZADO")))
                .andExpect(jsonPath("$.equipos[?(@.equipoId == " + id + ")].frecuenciaPrestamos").value(hasItem(0)))
                .andExpect(jsonPath("$.equipos[?(@.equipoId == " + id + ")].horasUso").value(hasItem(0)));
    }

    @Test
    void actualizar_cambiaNombreYUbicacion_sinTocarElEstado() throws Exception {
        long id = api.crearEquipo(token, "Equipo original", categoriaId, "Lab 1");
        EquipoRequestDTO dto = new EquipoRequestDTO("Equipo editado", categoriaId, "Lab 8");

        mockMvc.perform(api.auth(put("/api/equipos/" + id), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Equipo editado"))
                .andExpect(jsonPath("$.ubicacion").value("Lab 8"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.contadorUsoHoras").value(0));
    }

    @Test
    void eliminar_devuelve204_cuandoElEquipoNoTieneActividad() throws Exception {
        long id = api.crearEquipo(token, "Equipo descartable", categoriaId, "Lab 1");

        mockMvc.perform(api.auth(delete("/api/equipos/" + id), token))
                .andExpect(status().isNoContent());

        mockMvc.perform(api.auth(get("/api/equipos/" + id), token))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_devuelve409_cuandoElEquipoYaTieneUnaReserva() throws Exception {
        long id = api.crearEquipo(token, "Equipo con reserva", categoriaId, "Lab 1");
        java.time.LocalDateTime inicio = java.time.LocalDateTime.now().plusDays(4).withHour(9).withMinute(0).withSecond(0).withNano(0);
        com.utp.smartlab.dto.ReservaRequestDTO reserva =
                new com.utp.smartlab.dto.ReservaRequestDTO(id, inicio, inicio.plusHours(1));

        mockMvc.perform(api.auth(post("/api/reservas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserva)))
                .andExpect(status().isCreated());

        mockMvc.perform(api.auth(delete("/api/equipos/" + id), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("reserva")));
    }
}
