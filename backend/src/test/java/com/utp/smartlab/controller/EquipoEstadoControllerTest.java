package com.utp.smartlab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.smartlab.entity.*;
import com.utp.smartlab.repository.*;
import com.utp.smartlab.support.ApiDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.time.LocalDateTime;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EquipoEstadoControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EquipoRepository equipos;
    @Autowired ReservaRepository reservas;
    @Autowired PrestamoRepository prestamos;
    @Autowired MantenimientoRepository mantenimientos;
    @Autowired HistorialEstadoEquipoRepository historial;
    @Autowired SolicitudRechazadaRepository rechazadas;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired com.utp.smartlab.service.PrestamoService prestamoService;
    @Autowired com.utp.smartlab.service.ReservaService reservaService;
    ApiDePrueba api;
    String admin;
    long equipoId;

    @BeforeEach
    void preparar() throws Exception {
        api = new ApiDePrueba(mvc, json);
        admin = api.tokenAdmin();
        long categoria = api.crearCategoria(admin, "Estado-" + System.nanoTime());
        equipoId = api.crearEquipo(admin, "Equipo estado", categoria, "Lab");
    }

    ResultActions cambiar(String token, String estado, String motivo) throws Exception {
        return mvc.perform(api.auth(patch("/api/equipos/" + equipoId + "/estado"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("estado", estado, "motivo", motivo))));
    }
    long reservar() throws Exception {
        String body = mvc.perform(api.auth(post("/api/reservas"), admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("equipoId", equipoId,
                        "fechaInicio", LocalDateTime.now().plusDays(2), "fechaFin", LocalDateTime.now().plusDays(2).plusHours(2)))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
    ResultActions checkout(long reserva) throws Exception {
        return mvc.perform(api.auth(post("/api/prestamos/checkout"), admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("reservaId", reserva, "fechaLimite", LocalDateTime.now().plusDays(3)))));
    }
    long programar() throws Exception {
        String body = mantenimiento().andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
    ResultActions mantenimiento() throws Exception {
        return mvc.perform(api.auth(post("/api/mantenimientos"), admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("equipoId", equipoId, "tipo", "PREVENTIVO",
                        "fechaProgramada", LocalDateTime.now().plusDays(1)))));
    }

    @Test
    void bajaYReactivacion_persistenHistorialYAuditoria() throws Exception {
        assertThat(equipos.findById(equipoId).orElseThrow().getEstado()).isEqualTo(EstadoEquipo.DISPONIBLE);
        cambiar(admin, "FUERA_DE_SERVICIO", "  Avería irreparable  ").andExpect(status().isOk());
        assertThat(equipos.findById(equipoId).orElseThrow().getEstado()).isEqualTo(EstadoEquipo.FUERA_DE_SERVICIO);
        mvc.perform(api.auth(get("/api/equipos/" + equipoId + "/historial"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].estadoAnterior").value("DISPONIBLE"))
                .andExpect(jsonPath("$[1].estadoNuevo").value("FUERA_DE_SERVICIO"))
                .andExpect(jsonPath("$[1].motivo").value("Avería irreparable"))
                .andExpect(jsonPath("$[1].usuarioId").isNumber())
                .andExpect(jsonPath("$[1].fechaCambio").exists());
        mvc.perform(api.auth(get("/api/auditoria"), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].accion", hasItem("CAMBIAR_ESTADO_EQUIPO")));
        cambiar(admin, "DISPONIBLE", "Revisión técnica aprobada").andExpect(status().isOk());
        assertThat(historial.buscarPorEquipoIdOrdenado(equipoId)).hasSize(3);
        reservar();
    }

    @ParameterizedTest
    @EnumSource(value = NombreRol.class)
    void soloCoordinadorYAdministradorCambianEstado(NombreRol rol) throws Exception {
        String token = api.crearUsuarioYToken(admin, "Operador", "estado" + System.nanoTime() + "@test.com", "Clave123!", rol);
        int esperado = rol == NombreRol.COORDINADOR || rol == NombreRol.ADMINISTRADOR ? 200 : 403;
        cambiar(token, "FUERA_DE_SERVICIO", "Avería").andExpect(status().is(esperado));
        if (esperado == 403) assertThat(historial.buscarPorEquipoIdOrdenado(equipoId)).hasSize(1);
    }

    @Test
    void rechazaSinTokenYDatosInvalidos_sinCambios() throws Exception {
        mvc.perform(patch("/api/equipos/" + equipoId + "/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{}" )).andExpect(status().isUnauthorized());
        cambiar(admin, "FUERA_DE_SERVICIO", "   ").andExpect(status().isBadRequest());
        cambiar(admin, "FUERA_DE_SERVICIO", "x".repeat(256)).andExpect(status().isBadRequest());
        cambiar(admin, "DESCONOCIDO", "Avería").andExpect(status().isBadRequest());
        cambiar(admin, "EN_MANTENIMIENTO", "Manual").andExpect(status().isConflict());
        cambiar(admin, "DISPONIBLE", "Repetido").andExpect(status().isConflict());
        assertThat(historial.buscarPorEquipoIdOrdenado(equipoId)).hasSize(1);
    }

    @Test
    void bajaConReserva_conservaReservaPeroBloqueaCheckoutYNuevasReservas() throws Exception {
        long reserva = reservar();
        cambiar(admin, "FUERA_DE_SERVICIO", "Avería").andExpect(status().isOk());
        checkout(reserva).andExpect(status().isConflict());
        assertThat(reservas.findById(reserva).orElseThrow().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(prestamos.buscarPorReservaId(reserva)).isEmpty();
        mvc.perform(api.auth(post("/api/reservas"), admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("equipoId", equipoId,
                        "fechaInicio", LocalDateTime.now().plusDays(4), "fechaFin", LocalDateTime.now().plusDays(4).plusHours(2)))))
                .andExpect(status().isConflict());
        mantenimiento().andExpect(status().isConflict());
        assertThat(rechazadas.contarPorEquipoId(equipoId)).isZero();
        cambiar(admin, "DISPONIBLE", "Revisado").andExpect(status().isOk());
        Reserva existente = reservas.findById(reserva).orElseThrow();
        mvc.perform(api.auth(post("/api/reservas"), admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("equipoId", equipoId,
                        "fechaInicio", existente.getFechaInicio(), "fechaFin", existente.getFechaFin()))))
                .andExpect(status().isConflict());
        assertThat(rechazadas.contarPorEquipoId(equipoId)).isEqualTo(1);
    }

    @Test
    void bajaConPrestamo_permiteDevolucionSinReactivar() throws Exception {
        long reserva = reservar();
        String body = checkout(reserva).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long prestamo = json.readTree(body).get("id").asLong();
        cambiar(admin, "FUERA_DE_SERVICIO", "Falla durante uso").andExpect(status().isOk());
        cambiar(admin, "DISPONIBLE", "Intento prematuro").andExpect(status().isConflict());
        mvc.perform(api.auth(post("/api/prestamos/" + prestamo + "/checkin"), admin)).andExpect(status().isOk());
        assertThat(equipos.findById(equipoId).orElseThrow().getEstado()).isEqualTo(EstadoEquipo.FUERA_DE_SERVICIO);
        cambiar(admin, "DISPONIBLE", "Verificado tras devolución").andExpect(status().isOk());
    }

    @Test
    void mantenimientoAbierto_bloqueaBajaYCheckout_yCierrePreservaBajaExistente() throws Exception {
        long reserva = reservar();
        long mantenimiento = programar();
        cambiar(admin, "FUERA_DE_SERVICIO", "Avería").andExpect(status().isConflict());
        checkout(reserva).andExpect(status().isConflict());
        // Simula un dato legado inconsistente: el cierre nunca debe reactivar una baja.
        Equipo equipo = equipos.findById(equipoId).orElseThrow();
        equipo.setEstado(EstadoEquipo.FUERA_DE_SERVICIO);
        equipos.saveAndFlush(equipo);
        mvc.perform(api.auth(patch("/api/mantenimientos/" + mantenimiento + "/cerrar"), admin)).andExpect(status().isOk());
        assertThat(equipos.findById(equipoId).orElseThrow().getEstado()).isEqualTo(EstadoEquipo.FUERA_DE_SERVICIO);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void bajaConcurrente_revierteReservaOCheckout(boolean entrega) throws Exception {
        long reserva = reservar();
        Usuario usuario = new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                .execute(tx -> { Usuario u = reservas.findById(reserva).orElseThrow().getUsuario(); u.getNombre(); return u; });
        var preparado = new java.util.concurrent.CountDownLatch(1);
        var continuar = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var futuro = executor.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                    .executeWithoutResult(tx -> {
                        if (entrega) prestamoService.checkout(new com.utp.smartlab.dto.PrestamoCheckoutRequestDTO(
                                reserva, LocalDateTime.now().plusDays(3)));
                        else reservaService.crear(new com.utp.smartlab.dto.ReservaRequestDTO(equipoId,
                                LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(1)), usuario);
                        preparado.countDown();
                        try {
                            if (!continuar.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo agotado");
                        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                    }));
            if (!preparado.await(10, java.util.concurrent.TimeUnit.SECONDS)) futuro.get(1, java.util.concurrent.TimeUnit.SECONDS);
            cambiar(admin, "FUERA_DE_SERVICIO", "Baja concurrente").andExpect(status().isOk());
            continuar.countDown();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> futuro.get(10, java.util.concurrent.TimeUnit.SECONDS))
                    .hasCauseInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
            assertThat(reservas.contarPorEquipoId(equipoId)).isEqualTo(1);
            assertThat(prestamos.buscarPorReservaId(reserva)).isEmpty();
            assertThat(reservas.findById(reserva).orElseThrow().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(historial.buscarPorEquipoIdOrdenado(equipoId)).hasSize(2);
        } finally {
            continuar.countDown();
            executor.shutdownNow();
        }
    }
}
