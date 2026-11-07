package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.CategoriaRequestDTO;
import com.utp.smartlab.support.ApiDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ApiDePrueba api;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        api = new ApiDePrueba(mockMvc, objectMapper);
        token = api.tokenAdmin();
    }

    @Test
    void crud_creaConsultaRenombraYElimina() throws Exception {
        String nombre = "Optica " + System.nanoTime();
        String cuerpo = mockMvc.perform(api.auth(post("/api/categorias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoriaRequestDTO("  " + nombre + "  "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(cuerpo).get("id").asLong();

        mockMvc.perform(api.auth(get("/api/categorias/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(nombre));

        String renombre = nombre + " plus";
        mockMvc.perform(api.auth(put("/api/categorias/" + id), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoriaRequestDTO(renombre))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(renombre));

        mockMvc.perform(api.auth(delete("/api/categorias/" + id), token))
                .andExpect(status().isNoContent());

        mockMvc.perform(api.auth(get("/api/categorias/" + id), token))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_devuelve409_cuandoElNombreYaExisteIgnorandoMayusculas() throws Exception {
        String nombre = "Quimica " + System.nanoTime();
        api.crearCategoria(token, nombre);

        mockMvc.perform(api.auth(post("/api/categorias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoriaRequestDTO(nombre.toUpperCase()))))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminar_devuelve409_cuandoTieneEquipos() throws Exception {
        long categoriaId = api.crearCategoria(token, "Con equipos " + System.nanoTime());
        api.crearEquipo(token, "Microscopio", categoriaId, "Lab 1");

        mockMvc.perform(api.auth(delete("/api/categorias/" + categoriaId), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("equipo")));
    }

    @Test
    void obtenerPorId_devuelve404_cuandoNoExiste() throws Exception {
        mockMvc.perform(api.auth(get("/api/categorias/999999"), token))
                .andExpect(status().isNotFound());
    }
}
