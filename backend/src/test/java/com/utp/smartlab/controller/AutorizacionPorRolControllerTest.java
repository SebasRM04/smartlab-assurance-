package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.EquipoRequestDTO;
import com.utp.smartlab.dto.MantenimientoRequestDTO;
import com.utp.smartlab.dto.PrestamoCheckoutRequestDTO;
import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.TipoMantenimiento;
import com.utp.smartlab.support.ApiDePrueba;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutorizacionPorRolControllerTest {

    private static final String PASSWORD = "Demo123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ApiDePrueba api;
    private String tokenAdmin;
    private String tokenEstudiante;
    private String tokenTecnico;
    private String tokenDocente;
    private String tokenCoordinador;
    private long equipoId;
    private long categoriaVaciaId;
    private long reservaId;

    @BeforeAll
    void preparar() throws Exception {
        api = new ApiDePrueba(mockMvc, objectMapper);
        tokenAdmin = api.tokenAdmin();
        long categoriaId = api.crearCategoria(tokenAdmin, "Cat-roles-" + System.nanoTime());
        categoriaVaciaId = api.crearCategoria(tokenAdmin, "Cat-vacia-" + System.nanoTime());
        equipoId = api.crearEquipo(tokenAdmin, "Equipo roles", categoriaId, "Lab roles");

        tokenEstudiante = api.crearUsuarioYToken(tokenAdmin, "Est Rol", unico("est"), PASSWORD, NombreRol.ESTUDIANTE);
        tokenTecnico = api.crearUsuarioYToken(tokenAdmin, "Tec Rol", unico("tec"), PASSWORD, NombreRol.TECNICO);
        tokenDocente = api.crearUsuarioYToken(tokenAdmin, "Doc Rol", unico("doc"), PASSWORD, NombreRol.DOCENTE);
        tokenCoordinador = api.crearUsuarioYToken(tokenAdmin, "Coord Rol", unico("coord"), PASSWORD, NombreRol.COORDINADOR);

        LocalDateTime inicio = LocalDateTime.now().plusDays(6).withHour(8).withMinute(0).withSecond(0).withNano(0);
        String cuerpo = mockMvc.perform(api.auth(post("/api/reservas"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(1)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        reservaId = objectMapper.readTree(cuerpo).get("id").asLong();
    }

    @Test
    void estudiante_noCreaEquipos() throws Exception {
        mockMvc.perform(api.auth(post("/api/equipos"), tokenEstudiante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EquipoRequestDTO("No permitido", categoriaVaciaId, "Lab"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void estudiante_noHaceCheckout() throws Exception {
        PrestamoCheckoutRequestDTO dto = new PrestamoCheckoutRequestDTO(reservaId, LocalDateTime.now().plusDays(8));

        mockMvc.perform(api.auth(post("/api/prestamos/checkout"), tokenEstudiante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void estudiante_noVeLaAuditoriaNiLasCategorias() throws Exception {
        mockMvc.perform(api.auth(get("/api/auditoria"), tokenEstudiante))
                .andExpect(status().isForbidden());
        mockMvc.perform(api.auth(get("/api/categorias"), tokenEstudiante))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinador_listaCategorias_peroNoLasCrea() throws Exception {
        mockMvc.perform(api.auth(get("/api/categorias"), tokenCoordinador))
                .andExpect(status().isOk());

        mockMvc.perform(api.auth(post("/api/categorias"), tokenCoordinador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"No permitida\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void estudiante_noVeLosPrestamosVencidos() throws Exception {
        mockMvc.perform(api.auth(get("/api/prestamos/vencidos"), tokenEstudiante))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnico_noCreaReservasNiUsuarios() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusDays(7).withHour(15).withMinute(0).withSecond(0).withNano(0);
        mockMvc.perform(api.auth(post("/api/reservas"), tokenTecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(1)))))
                .andExpect(status().isForbidden());

        mockMvc.perform(api.auth(post("/api/usuarios"), tokenTecnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UsuarioRequestDTO("X", unico("x"), PASSWORD, NombreRol.ESTUDIANTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    void docente_noProgramaMantenimiento() throws Exception {
        MantenimientoRequestDTO dto = new MantenimientoRequestDTO(
                equipoId, TipoMantenimiento.PREVENTIVO, LocalDateTime.now().plusDays(2));

        mockMvc.perform(api.auth(post("/api/mantenimientos"), tokenDocente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinador_noEliminaEquiposNiCategorias() throws Exception {
        mockMvc.perform(api.auth(delete("/api/equipos/" + equipoId), tokenCoordinador))
                .andExpect(status().isForbidden());
        mockMvc.perform(api.auth(delete("/api/categorias/" + categoriaVaciaId), tokenCoordinador))
                .andExpect(status().isForbidden());
    }

    @Test
    void estudiante_siPuedeReservar() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusDays(9).withHour(10).withMinute(0).withSecond(0).withNano(0);

        mockMvc.perform(api.auth(post("/api/reservas"), tokenEstudiante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    private String unico(String prefijo) {
        return prefijo + "." + System.nanoTime() + "@smartlab.com";
    }
}
