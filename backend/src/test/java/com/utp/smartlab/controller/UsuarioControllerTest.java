package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.LoginRequestDTO;
import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.dto.UsuarioUpdateDTO;
import com.utp.smartlab.entity.NombreRol;
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
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ApiDePrueba api;
    private String token;
    private long adminId;

    @BeforeEach
    void setUp() throws Exception {
        api = new ApiDePrueba(mockMvc, objectMapper);
        JsonNode login = api.loginCompleto(ApiDePrueba.ADMIN_EMAIL, ApiDePrueba.ADMIN_PASSWORD);
        token = login.get("token").asText();
        adminId = login.get("usuarioId").asLong();
    }

    @Test
    void crud_creaListaEditaRolYElimina_siNoTieneAuditoria() throws Exception {
        String email = unico("tecnico");
        long id = api.crearUsuario(token, "Tomás", email, "Demo123!", NombreRol.TECNICO);

        mockMvc.perform(api.auth(get("/api/usuarios/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("TECNICO"))
                .andExpect(jsonPath("$.email").value(email));

        UsuarioUpdateDTO cambio = new UsuarioUpdateDTO("Tomás Técnico", email, NombreRol.DOCENTE, null);
        mockMvc.perform(api.auth(put("/api/usuarios/" + id), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("DOCENTE"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(email, "Demo123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("DOCENTE"));

        mockMvc.perform(api.auth(delete("/api/usuarios/" + id), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("bitácora")));
    }

    @Test
    void eliminar_devuelve204_cuandoElUsuarioNuncaInicioSesion() throws Exception {
        String email = unico("nuevo");
        long id = api.crearUsuario(token, "Sin login", email, "Demo123!", NombreRol.ESTUDIANTE);

        mockMvc.perform(api.auth(delete("/api/usuarios/" + id), token))
                .andExpect(status().isNoContent());

        mockMvc.perform(api.auth(get("/api/usuarios/" + id), token))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_conservaLaContrasena_cuandoElPasswordVieneNulo() throws Exception {
        String email = unico("clave");
        long id = api.crearUsuario(token, "Clave", email, "Demo123!", NombreRol.ESTUDIANTE);

        UsuarioUpdateDTO dto = new UsuarioUpdateDTO("Clave Nueva", email, NombreRol.ESTUDIANTE, null);
        mockMvc.perform(api.auth(put("/api/usuarios/" + id), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Clave Nueva"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(email, "Demo123!"))))
                .andExpect(status().isOk());
    }

    @Test
    void administrador_noPuedeCambiarseElRolNiEliminarse() throws Exception {
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO(
                "Administrador SmartLab", ApiDePrueba.ADMIN_EMAIL, NombreRol.DOCENTE, null);

        mockMvc.perform(api.auth(put("/api/usuarios/" + adminId), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("propio rol")));

        mockMvc.perform(api.auth(delete("/api/usuarios/" + adminId), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("propio usuario")));
    }

    @Test
    void crear_devuelve409_cuandoElEmailYaExiste() throws Exception {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                "Otro admin", ApiDePrueba.ADMIN_EMAIL, "Demo123!", NombreRol.DOCENTE);

        mockMvc.perform(api.auth(post("/api/usuarios"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    private String unico(String prefijo) {
        return prefijo + "." + System.nanoTime() + "@smartlab.com";
    }
}
