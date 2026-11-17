package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.entity.NombreRol;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtro de reservas por usuario (usuarioId) y endpoint /api/usuarios/opciones. Solo técnico, coordinador y
 * administrador pueden usarlos. Cada prueba crea sus propios usuarios y equipos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FiltroReservasPorUsuarioControllerTest {

    private static final String PASSWORD = "Demo123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ApiDePrueba api;
    private String tokenAdmin;
    private long categoriaId;
    private String sufijo;

    @BeforeEach
    void setUp() throws Exception {
        api = new ApiDePrueba(mockMvc, objectMapper);
        tokenAdmin = api.tokenAdmin();
        sufijo = String.valueOf(System.nanoTime());
        categoriaId = api.crearCategoria(tokenAdmin, "Cat-filtro-usuario-" + sufijo);
    }

    private JsonNode obtener(String url, String token) throws Exception {
        String cuerpo = mockMvc.perform(api.auth(get(url), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo);
    }

    private void reservar(String token, long equipoId, int diasAdelante) throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusDays(diasAdelante).withSecond(0).withNano(0);
        ReservaRequestDTO dto = new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(2));
        mockMvc.perform(api.auth(post("/api/reservas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    private String email(String nombre) {
        return nombre + "-" + sufijo + "@smartlab.com";
    }

    @Test
    void elPersonal_filtraLasReservasDeUnUsuario_solo_conEquipo_yConEstado() throws Exception {
        long equipoA = api.crearEquipo(tokenAdmin, "Equipo A " + sufijo, categoriaId, "Lab 1");
        long equipoB = api.crearEquipo(tokenAdmin, "Equipo B " + sufijo, categoriaId, "Lab 1");
        long idAna = api.crearUsuario(tokenAdmin, "Ana Filtro", email("ana"), PASSWORD, NombreRol.ESTUDIANTE);
        api.crearUsuario(tokenAdmin, "Beto Filtro", email("beto"), PASSWORD, NombreRol.ESTUDIANTE);
        String tokenAna = api.login(email("ana"), PASSWORD);
        String tokenBeto = api.login(email("beto"), PASSWORD);

        reservar(tokenAna, equipoA, 20);
        reservar(tokenAna, equipoB, 21);
        reservar(tokenBeto, equipoA, 22);

        // Solo usuario: las dos de Ana, ninguna de Beto
        JsonNode soloUsuario = obtener("/api/reservas/pagina?usuarioId=" + idAna, tokenAdmin);
        assertEquals(2, soloUsuario.get("totalElementos").asInt());
        for (JsonNode reserva : soloUsuario.get("contenido")) {
            assertEquals(idAna, reserva.get("usuarioId").asLong());
        }

        // Usuario + equipo: una sola
        JsonNode conEquipo = obtener("/api/reservas/pagina?usuarioId=" + idAna + "&equipoId=" + equipoA, tokenAdmin);
        assertEquals(1, conEquipo.get("totalElementos").asInt());
        assertEquals(equipoA, conEquipo.get("contenido").get(0).get("equipoId").asLong());

        // Usuario + estado
        assertEquals(2, obtener("/api/reservas/pagina?usuarioId=" + idAna + "&estado=CONFIRMADA", tokenAdmin)
                .get("totalElementos").asInt());
        assertEquals(0, obtener("/api/reservas/pagina?usuarioId=" + idAna + "&estado=CANCELADA", tokenAdmin)
                .get("totalElementos").asInt());

        // Sin usuarioId, el equipo A muestra las de los dos usuarios (el filtro de equipo sigue igual)
        assertEquals(2, obtener("/api/reservas/pagina?equipoId=" + equipoA, tokenAdmin).get("totalElementos").asInt());
    }

    @Test
    void tecnicoYCoordinador_puedenFiltrarPorUsuario() throws Exception {
        long idAna = api.crearUsuario(tokenAdmin, "Ana Filtro", email("ana"), PASSWORD, NombreRol.ESTUDIANTE);
        String tokenTecnico = api.crearUsuarioYToken(tokenAdmin, "Tito Filtro", email("tito"), PASSWORD, NombreRol.TECNICO);
        String tokenCoordinador = api.crearUsuarioYToken(tokenAdmin, "Cora Filtro", email("cora"), PASSWORD, NombreRol.COORDINADOR);

        mockMvc.perform(api.auth(get("/api/reservas/pagina?usuarioId=" + idAna), tokenTecnico))
                .andExpect(status().isOk());
        mockMvc.perform(api.auth(get("/api/reservas/pagina?usuarioId=" + idAna), tokenCoordinador))
                .andExpect(status().isOk());
    }

    @Test
    void docenteYEstudiante_noPuedenFiltrarPorUsuario_peroSiListar() throws Exception {
        long idAna = api.crearUsuario(tokenAdmin, "Ana Filtro", email("ana"), PASSWORD, NombreRol.ESTUDIANTE);
        String tokenDocente = api.crearUsuarioYToken(tokenAdmin, "Dora Filtro", email("dora"), PASSWORD, NombreRol.DOCENTE);
        String tokenEstudiante = api.login(email("ana"), PASSWORD);

        mockMvc.perform(api.auth(get("/api/reservas/pagina?usuarioId=" + idAna), tokenDocente))
                .andExpect(status().isForbidden());
        mockMvc.perform(api.auth(get("/api/reservas/pagina?usuarioId=" + idAna), tokenEstudiante))
                .andExpect(status().isForbidden());

        // Sin el parámetro siguen listando como siempre
        mockMvc.perform(api.auth(get("/api/reservas/pagina"), tokenDocente)).andExpect(status().isOk());
        mockMvc.perform(api.auth(get("/api/reservas/pagina"), tokenEstudiante)).andExpect(status().isOk());
    }

    @Test
    void opciones_devuelveSoloIdYNombre_yNoLoVenDocenteNiEstudiante() throws Exception {
        api.crearUsuario(tokenAdmin, "Zoe Filtro", email("zoe"), PASSWORD, NombreRol.ESTUDIANTE);
        String tokenTecnico = api.crearUsuarioYToken(tokenAdmin, "Tito Filtro", email("tito"), PASSWORD, NombreRol.TECNICO);
        String tokenDocente = api.crearUsuarioYToken(tokenAdmin, "Dora Filtro", email("dora"), PASSWORD, NombreRol.DOCENTE);
        String tokenEstudiante = api.login(email("zoe"), PASSWORD);

        JsonNode opciones = obtener("/api/usuarios/opciones", tokenTecnico);
        assertTrue(opciones.size() >= 3);
        boolean estaZoe = false;
        for (JsonNode opcion : opciones) {
            assertTrue(opcion.has("id") && opcion.has("nombre"));
            assertFalse(opcion.has("email"), "las opciones no deben exponer el correo");
            assertFalse(opcion.has("rol"), "las opciones no deben exponer el rol");
            estaZoe = estaZoe || "Zoe Filtro".equals(opcion.get("nombre").asText());
        }
        assertTrue(estaZoe);

        // El administrador también puede; docente y estudiante no
        obtener("/api/usuarios/opciones", tokenAdmin);
        mockMvc.perform(api.auth(get("/api/usuarios/opciones"), tokenDocente)).andExpect(status().isForbidden());
        mockMvc.perform(api.auth(get("/api/usuarios/opciones"), tokenEstudiante)).andExpect(status().isForbidden());
    }
}
