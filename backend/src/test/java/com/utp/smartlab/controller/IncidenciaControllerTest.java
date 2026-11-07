package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.IncidenciaEstadoUpdateDTO;
import com.utp.smartlab.dto.IncidenciaRequestDTO;
import com.utp.smartlab.entity.EstadoIncidencia;
import com.utp.smartlab.support.ApiDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncidenciaControllerTest {

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
        categoriaId = api.crearCategoria(token, "Cat-incidencia-" + System.nanoTime());
    }

    private long crearEquipoDePrueba() throws Exception {
        return api.crearEquipo(token, "Equipo incidencia", categoriaId, "Lab 7");
    }

    @Test
    void registrar_devuelve201_enEstadoAbierta() throws Exception {
        long equipoId = crearEquipoDePrueba();
        IncidenciaRequestDTO dto = new IncidenciaRequestDTO(equipoId, "Cable dañado", "foto1.jpg");

        mockMvc.perform(api.auth(post("/api/incidencias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.equipoId").value(equipoId));
    }

    @Test
    void registrar_devuelve404_cuandoElEquipoNoExiste() throws Exception {
        IncidenciaRequestDTO dto = new IncidenciaRequestDTO(999999L, "Daño reportado", null);

        mockMvc.perform(api.auth(post("/api/incidencias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarEstado_devuelve409_cuandoYaEstaResuelta() throws Exception {
        long equipoId = crearEquipoDePrueba();
        IncidenciaRequestDTO dto = new IncidenciaRequestDTO(equipoId, "Pantalla rota", null);

        String respuesta = mockMvc.perform(api.auth(post("/api/incidencias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long incidenciaId = objectMapper.readTree(respuesta).get("id").asLong();

        IncidenciaEstadoUpdateDTO aResuelta = new IncidenciaEstadoUpdateDTO(EstadoIncidencia.RESUELTA);
        mockMvc.perform(api.auth(patch("/api/incidencias/" + incidenciaId + "/estado"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aResuelta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTA"));

        IncidenciaEstadoUpdateDTO aEnRevision = new IncidenciaEstadoUpdateDTO(EstadoIncidencia.EN_REVISION);
        mockMvc.perform(api.auth(patch("/api/incidencias/" + incidenciaId + "/estado"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aEnRevision)))
                .andExpect(status().isConflict());
    }

    @Test
    void listarPorEquipo_devuelveSoloLasIncidenciasDeEseEquipo() throws Exception {
        long equipoId = crearEquipoDePrueba();
        IncidenciaRequestDTO dto = new IncidenciaRequestDTO(equipoId, "Incidencia de prueba", null);

        mockMvc.perform(api.auth(post("/api/incidencias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        mockMvc.perform(api.auth(get("/api/incidencias/equipo/" + equipoId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].equipoId").value(equipoId));
    }
}
