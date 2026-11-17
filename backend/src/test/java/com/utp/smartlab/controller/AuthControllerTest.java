package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.LoginRequestDTO;
import com.utp.smartlab.dto.RegistroPublicoRequestDTO;
import com.utp.smartlab.support.ApiDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ApiDePrueba api;

    @BeforeEach
    void setUp() {
        api = new ApiDePrueba(mockMvc, objectMapper);
    }

    @Test
    void login_devuelveToken_conElAdminSembrado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDTO(ApiDePrueba.ADMIN_EMAIL, ApiDePrueba.ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.email").value(ApiDePrueba.ADMIN_EMAIL))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    void login_devuelve401_cuandoLaContrasenaNoCoincide() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDTO(ApiDePrueba.ADMIN_EMAIL, "clave-incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("incorrectos")));
    }

    @Test
    void registro_creaSiempreEstudiante_aunqueElBodyPidaOtroRol() throws Exception {
        String email = "publico." + System.nanoTime() + "@smartlab.com";
        String json = """
                {"nombre":"Intruso","email":"%s","password":"Secreta1","rol":"ADMINISTRADOR"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(email, "Secreta1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
    }

    @Test
    void registro_devuelve409_cuandoElEmailYaExiste() throws Exception {
        RegistroPublicoRequestDTO dto = new RegistroPublicoRequestDTO(
                "Otra vez", ApiDePrueba.ADMIN_EMAIL, "Secreta1");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void registro_devuelve400_cuandoLaContrasenaEsMuyCorta() throws Exception {
        RegistroPublicoRequestDTO dto = new RegistroPublicoRequestDTO(
                "Corto", "corto." + System.nanoTime() + "@smartlab.com", "123");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rutaProtegida_devuelve401_sinTokenYConTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/equipos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("token")));

        mockMvc.perform(get("/api/equipos").header("Authorization", "Bearer esto-no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_quedaEnLaBitacora_visibleSoloParaElAdministrador() throws Exception {
        String token = api.tokenAdmin();

        mockMvc.perform(api.auth(get("/api/auditoria"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.accion == 'LOGIN')]").isNotEmpty());
    }
}
