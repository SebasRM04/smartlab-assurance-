package com.utp.smartlab.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflight_permiteAlFrontendDeAngular_sinNecesitarToken() throws Exception {
        mockMvc.perform(options("/api/reservas")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void preflight_rechazaUnOrigenQueNoEstaEnLaLista() throws Exception {
        mockMvc.perform(options("/api/reservas")
                        .header("Origin", "http://sitio-desconocido.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laRespuestaDeLogin_incluyeElEncabezadoCors_paraElOrigenPermitido() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://127.0.0.1:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:4200"));
    }
}
