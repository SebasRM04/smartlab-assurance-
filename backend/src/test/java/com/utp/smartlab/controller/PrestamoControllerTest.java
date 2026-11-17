package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.PrestamoCheckoutRequestDTO;
import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.service.PrestamoService;
import com.utp.smartlab.support.ApiDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PrestamoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrestamoService prestamoService;

    private ApiDePrueba api;
    private String token;
    private long categoriaId;

    @BeforeEach
    void setUp() throws Exception {
        api = new ApiDePrueba(mockMvc, objectMapper);
        token = api.tokenAdmin();
        categoriaId = api.crearCategoria(token, "Cat-prestamo-" + System.nanoTime());
    }

    private long crearReservaConfirmadaDePrueba() throws Exception {
        long equipoId = api.crearEquipo(token, "Equipo préstamo", categoriaId, "Lab 2");
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        ReservaRequestDTO reservaDto = new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(2));
        String respuestaReserva = mockMvc.perform(api.auth(post("/api/reservas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservaDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuestaReserva).get("id").asLong();
    }

    @Test
    void checkout_devuelve201_cuandoLaReservaEsValida() throws Exception {
        long reservaId = crearReservaConfirmadaDePrueba();
        PrestamoCheckoutRequestDTO dto = new PrestamoCheckoutRequestDTO(reservaId, LocalDateTime.now().plusDays(5));

        mockMvc.perform(api.auth(post("/api/prestamos/checkout"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.moroso").value(false));
    }

    @Test
    void checkout_devuelve404_cuandoLaReservaNoExiste() throws Exception {
        PrestamoCheckoutRequestDTO dto = new PrestamoCheckoutRequestDTO(999999L, LocalDateTime.now().plusDays(5));

        mockMvc.perform(api.auth(post("/api/prestamos/checkout"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void prestamoActivoConLimitePasado_apareceComoVencido() throws Exception {
        long reservaId = crearReservaConfirmadaDePrueba();
        // La API exige fecha límite futura; se llama al servicio para simular que el plazo ya venció
        long prestamoId = prestamoService
                .checkout(new PrestamoCheckoutRequestDTO(reservaId, LocalDateTime.now().minusHours(2)))
                .getId();

        mockMvc.perform(api.auth(get("/api/prestamos/" + prestamoId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.vencido").value(true))
                .andExpect(jsonPath("$.moroso").value(false));

        mockMvc.perform(api.auth(get("/api/prestamos/vencidos"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + prestamoId + ")]").isNotEmpty());

        mockMvc.perform(api.auth(post("/api/prestamos/" + prestamoId + "/checkin"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vencido").value(false))
                .andExpect(jsonPath("$.moroso").value(true));

        mockMvc.perform(api.auth(get("/api/prestamos/vencidos"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + prestamoId + ")]").isEmpty());
    }

    @Test
    void prestamoDentroDelPlazo_noEstaVencido() throws Exception {
        long reservaId = crearReservaConfirmadaDePrueba();
        PrestamoCheckoutRequestDTO dto = new PrestamoCheckoutRequestDTO(reservaId, LocalDateTime.now().plusDays(5));

        String cuerpo = mockMvc.perform(api.auth(post("/api/prestamos/checkout"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vencido").value(false))
                .andReturn().getResponse().getContentAsString();
        long prestamoId = objectMapper.readTree(cuerpo).get("id").asLong();

        mockMvc.perform(api.auth(get("/api/prestamos/vencidos"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + prestamoId + ")]").isEmpty());
    }
}
