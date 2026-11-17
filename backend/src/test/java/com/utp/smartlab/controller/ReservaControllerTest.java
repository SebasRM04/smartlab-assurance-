package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.dto.ReservaEstadoUpdateDTO;
import com.utp.smartlab.dto.ReservaRequestDTO;
import com.utp.smartlab.entity.EstadoReserva;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservaControllerTest {

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
        categoriaId = api.crearCategoria(token, "Cat-reserva-" + System.nanoTime());
    }

    private long crearEquipoDePrueba() throws Exception {
        return api.crearEquipo(token, "Equipo de prueba", categoriaId, "Lab 1");
    }

    @Test
    void crear_devuelve201_cuandoNoHayConflicto() throws Exception {
        long equipoId = crearEquipoDePrueba();
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        ReservaRequestDTO dto = new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(2));

        mockMvc.perform(api.auth(post("/api/reservas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void crear_devuelve409_cuandoSeSolapaConOtraReservaDelMismoEquipo() throws Exception {

        long equipoId = crearEquipoDePrueba();
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        ReservaRequestDTO primeraReserva = new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(2));

        mockMvc.perform(api.auth(post("/api/reservas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primeraReserva)))
                .andExpect(status().isCreated());

        ReservaRequestDTO segundaReservaConflictiva =
                new ReservaRequestDTO(equipoId, inicio.plusHours(1), inicio.plusHours(3));

        mockMvc.perform(api.auth(post("/api/reservas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(segundaReservaConflictiva)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("se solapa")));
    }

    @Test
    void metricasDeConflicto_reflejanLaSolicitudRechazada_trasUnSolapamiento() throws Exception {

        long equipoId = crearEquipoDePrueba();
        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        ReservaRequestDTO primeraReserva = new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(2));

        mockMvc.perform(api.auth(post("/api/reservas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primeraReserva)))
                .andExpect(status().isCreated());

        ReservaRequestDTO segundaReservaConflictiva =
                new ReservaRequestDTO(equipoId, inicio.plusMinutes(30), inicio.plusHours(3));

        mockMvc.perform(api.auth(post("/api/reservas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(segundaReservaConflictiva)))
                .andExpect(status().isConflict());

        mockMvc.perform(api.auth(get("/api/reservas/reportes/conflictos"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rechazadasPorConflicto").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void estudiante_soloVeYCancelaSusPropiasReservas() throws Exception {
        long equipoId = crearEquipoDePrueba();
        String tokenAna = api.crearUsuarioYToken(token, "Ana Estudiante", unico("ana"), "Demo123!", NombreRol.ESTUDIANTE);
        String tokenBruno = api.crearUsuarioYToken(token, "Bruno Estudiante", unico("bruno"), "Demo123!", NombreRol.ESTUDIANTE);

        LocalDateTime inicio = LocalDateTime.now().plusDays(3).withHour(11).withMinute(0).withSecond(0).withNano(0);
        ReservaRequestDTO dto = new ReservaRequestDTO(equipoId, inicio, inicio.plusHours(2));

        String cuerpo = mockMvc.perform(api.auth(post("/api/reservas"), tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long reservaId = objectMapper.readTree(cuerpo).get("id").asLong();

        mockMvc.perform(api.auth(get("/api/reservas"), tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reservaId));

        mockMvc.perform(api.auth(get("/api/reservas/" + reservaId), tokenBruno))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("propias")));

        mockMvc.perform(api.auth(patch("/api/reservas/" + reservaId + "/estado"), tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservaEstadoUpdateDTO(EstadoReserva.NO_SHOW))))
                .andExpect(status().isConflict());

        mockMvc.perform(api.auth(patch("/api/reservas/" + reservaId + "/estado"), tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservaEstadoUpdateDTO(EstadoReserva.CANCELADA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    private String unico(String prefijo) {
        return prefijo + "." + System.nanoTime() + "@smartlab.com";
    }
}
