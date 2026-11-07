package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.MantenimientoRequestDTO;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MantenimientoControllerTest {

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
        categoriaId = api.crearCategoria(token, "Cat-mant-" + System.nanoTime());
    }

    private long crearEquipoDePrueba() throws Exception {
        return api.crearEquipo(token, "Equipo mantenimiento", categoriaId, "Lab 4");
    }

    @Test
    void programar_bloqueaLaDisponibilidadDelEquipo() throws Exception {
        long equipoId = crearEquipoDePrueba();
        MantenimientoRequestDTO dto = new MantenimientoRequestDTO(
                equipoId, TipoMantenimiento.PREVENTIVO, LocalDateTime.now().plusDays(1));

        mockMvc.perform(api.auth(post("/api/mantenimientos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"));

        mockMvc.perform(api.auth(get("/api/equipos/" + equipoId + "/disponibilidad"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_MANTENIMIENTO"));
    }

    @Test
    void cerrar_devuelve409_cuandoElMantenimientoYaEstaCerrado() throws Exception {
        long equipoId = crearEquipoDePrueba();
        MantenimientoRequestDTO dto = new MantenimientoRequestDTO(
                equipoId, TipoMantenimiento.CORRECTIVO, LocalDateTime.now().minusHours(1));

        String respuesta = mockMvc.perform(api.auth(post("/api/mantenimientos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long mantenimientoId = objectMapper.readTree(respuesta).get("id").asLong();

        mockMvc.perform(api.auth(patch("/api/mantenimientos/" + mantenimientoId + "/cerrar"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));

        mockMvc.perform(api.auth(patch("/api/mantenimientos/" + mantenimientoId + "/cerrar"), token))
                .andExpect(status().isConflict());
    }

    @Test
    void cerrar_mantieneElEquipoEnMantenimiento_mientrasQuedeOtroProgramado() throws Exception {
        long equipoId = crearEquipoDePrueba();

        long primero = programar(equipoId, TipoMantenimiento.PREVENTIVO, LocalDateTime.now().plusDays(1));
        long segundo = programar(equipoId, TipoMantenimiento.CORRECTIVO, LocalDateTime.now().plusDays(2));

        mockMvc.perform(api.auth(patch("/api/mantenimientos/" + primero + "/cerrar"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));

        mockMvc.perform(api.auth(get("/api/equipos/" + equipoId + "/disponibilidad"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_MANTENIMIENTO"));

        mockMvc.perform(api.auth(patch("/api/mantenimientos/" + segundo + "/cerrar"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));

        mockMvc.perform(api.auth(get("/api/equipos/" + equipoId + "/disponibilidad"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    private long programar(long equipoId, TipoMantenimiento tipo, LocalDateTime fecha) throws Exception {
        MantenimientoRequestDTO dto = new MantenimientoRequestDTO(equipoId, tipo, fecha);
        String respuesta = mockMvc.perform(api.auth(post("/api/mantenimientos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asLong();
    }
}
