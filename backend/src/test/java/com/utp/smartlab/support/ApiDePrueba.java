package com.utp.smartlab.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.CategoriaRequestDTO;
import com.utp.smartlab.dto.EquipoRequestDTO;
import com.utp.smartlab.dto.LoginRequestDTO;
import com.utp.smartlab.dto.UsuarioRequestDTO;
import com.utp.smartlab.entity.NombreRol;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ApiDePrueba {

    public static final String ADMIN_EMAIL = "admin@smartlab.com";
    public static final String ADMIN_PASSWORD = "Admin123!";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public ApiDePrueba(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    public JsonNode loginCompleto(String email, String password) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo);
    }

    public String login(String email, String password) throws Exception {
        return loginCompleto(email, password).get("token").asText();
    }

    public String tokenAdmin() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    public long crearCategoria(String token, String nombre) throws Exception {
        String cuerpo = mockMvc.perform(auth(post("/api/categorias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoriaRequestDTO(nombre))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).get("id").asLong();
    }

    public long crearEquipo(String token, String nombre, long categoriaId, String ubicacion) throws Exception {
        String cuerpo = mockMvc.perform(auth(post("/api/equipos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EquipoRequestDTO(nombre, categoriaId, ubicacion))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).get("id").asLong();
    }

    public long crearUsuario(String tokenAdmin, String nombre, String email, String password, NombreRol rol) throws Exception {
        String cuerpo = mockMvc.perform(auth(post("/api/usuarios"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsuarioRequestDTO(nombre, email, password, rol))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).get("id").asLong();
    }

    public String crearUsuarioYToken(String tokenAdmin, String nombre, String email, String password, NombreRol rol)
            throws Exception {
        crearUsuario(tokenAdmin, nombre, email, password, rol);
        return login(email, password);
    }
}
