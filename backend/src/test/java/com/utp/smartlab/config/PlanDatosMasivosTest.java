package com.utp.smartlab.config;

import com.utp.smartlab.config.masivo.PlanDatosMasivos;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.EquipoPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.HistorialPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.MantenimientoPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.Plan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.PrestamoPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.RechazoPlan;
import com.utp.smartlab.config.masivo.PlanDatosMasivos.ReservaPlan;
import com.utp.smartlab.entity.EstadoEquipo;
import com.utp.smartlab.entity.EstadoMantenimiento;
import com.utp.smartlab.entity.EstadoPrestamo;
import com.utp.smartlab.entity.EstadoReserva;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba que el escenario masivo respeta las reglas del sistema. No necesita base de datos.
 */
class PlanDatosMasivosTest {

    private static final long SEMILLA = 2026L;
    private static final int EQUIPOS = 172;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 3, 15, 30);

    private final Plan plan = PlanDatosMasivos.generar(AHORA, SEMILLA, EQUIPOS);

    @Test
    void supera5000Registros_yTieneLosEquiposDelCaso() {
        assertTrue(plan.totalRegistros() >= 5000, "registros: " + plan.totalRegistros());
        assertTrue(plan.reservas().size() >= 5000, "reservas: " + plan.reservas().size());
        assertEquals(EQUIPOS, plan.equipos().size(), "equipos");
    }

    @Test
    void esReproducible_conLaMismaSemillaYFecha() {
        Plan otro = PlanDatosMasivos.generar(AHORA, SEMILLA, EQUIPOS);
        assertEquals(plan.reservas(), otro.reservas(), "reservas");
        assertEquals(plan.prestamos(), otro.prestamos(), "préstamos");
        assertEquals(plan.mantenimientos(), otro.mantenimientos(), "mantenimientos");
    }

    @Test
    void noHayReservasActivasSuperpuestas_delMismoEquipo() {
        for (Plan p : escenarios()) {
            Map<Integer, List<ReservaPlan>> activasPorEquipo = new HashMap<>();
            for (ReservaPlan r : p.reservas()) {
                if (r.estado() == EstadoReserva.CONFIRMADA || r.estado() == EstadoReserva.ATENDIDA) {
                    activasPorEquipo.computeIfAbsent(r.equipo(), k -> new ArrayList<>()).add(r);
                }
            }
            for (List<ReservaPlan> lista : activasPorEquipo.values()) {
                lista.sort(Comparator.comparing(ReservaPlan::inicio));
                for (int i = 1; i < lista.size(); i++) {
                    assertFalse(lista.get(i).inicio().isBefore(lista.get(i - 1).fin()),
                            "reservas superpuestas: " + lista.get(i - 1) + " y " + lista.get(i));
                }
            }
        }
    }

    @Test
    void ningunaReservaCaeMientrasElEquipoEstabaBloqueado() {
        for (Plan p : escenarios()) {
            for (int e = 0; e < p.equipos().size(); e++) {
                List<HistorialPlan> historial = historialDe(p, e);
                for (ReservaPlan r : p.reservas()) {
                    if (r.equipo() != e) {
                        continue;
                    }
                    for (int i = 0; i < historial.size(); i++) {
                        HistorialPlan h = historial.get(i);
                        LocalDateTime hasta = i + 1 < historial.size() ? historial.get(i + 1).fecha() : null;
                        boolean bloqueado = h.nuevo() != EstadoEquipo.DISPONIBLE;
                        boolean solapa = r.inicio().isBefore(hasta == null ? LocalDateTime.MAX : hasta)
                                && r.fin().isAfter(h.fecha());
                        assertFalse(bloqueado && solapa,
                                "reserva " + r + " cae con el equipo " + e + " en " + h.nuevo() + " desde " + h.fecha());
                    }
                }
            }
        }
    }

    @Test
    void soloLasReservasAtendidasTienenPrestamo_yCadaUnaTieneUno() {
        for (Plan p : escenarios()) {
            Set<Integer> conPrestamo = new HashSet<>();
            for (PrestamoPlan pr : p.prestamos()) {
                assertTrue(conPrestamo.add(pr.reserva()), "dos préstamos para la reserva " + pr.reserva());
                assertEquals(EstadoReserva.ATENDIDA, p.reservas().get(pr.reserva()).estado(), "estado de la reserva");
            }
            for (int i = 0; i < p.reservas().size(); i++) {
                assertEquals(p.reservas().get(i).estado() == EstadoReserva.ATENDIDA, conPrestamo.contains(i),
                        "reserva " + i + " y su préstamo");
            }
        }
    }

    @Test
    void lasFechasDeLosPrestamos_sonCoherentes() {
        for (Plan p : escenarios()) {
            for (PrestamoPlan pr : p.prestamos()) {
                ReservaPlan r = p.reservas().get(pr.reserva());
                assertFalse(pr.entrega().isBefore(r.inicio()), "entrega antes del inicio: " + pr);
                assertFalse(pr.entrega().isAfter(p.ahora()), "entrega en el futuro: " + pr);
                assertEquals(r.fin(), pr.limite(), "fecha límite");
                if (pr.estado() == EstadoPrestamo.DEVUELTO) {
                    assertTrue(pr.devolucion() != null, "devuelto sin fecha real: " + pr);
                    assertTrue(pr.devolucion().isAfter(pr.entrega()), "devolución antes de la entrega: " + pr);
                    assertFalse(pr.devolucion().isAfter(p.ahora()), "devolución en el futuro: " + pr);
                } else {
                    assertTrue(pr.devolucion() == null, "activo con devolución: " + pr);
                }
            }
        }
    }

    @Test
    void lasHorasDeUsoSonLaSumaDeLosPrestamosDevueltos() {
        for (Plan p : escenarios()) {
            long[] horas = new long[p.equipos().size()];
            for (PrestamoPlan pr : p.prestamos()) {
                if (pr.estado() == EstadoPrestamo.DEVUELTO) {
                    horas[p.reservas().get(pr.reserva()).equipo()] +=
                            Math.max(Duration.between(pr.entrega(), pr.devolucion()).toHours(), 0);
                }
            }
            for (int e = 0; e < horas.length; e++) {
                assertEquals(horas[e], p.equipos().get(e).horasUso(), "horas del equipo " + e);
            }
        }
    }

    @Test
    void laExactitudDeDisponibilidadEs100_porqueTodoEquipoConMantenimientoAbiertoEstaBloqueado() {
        for (Plan p : escenarios()) {
            Set<Integer> conProgramado = new HashSet<>();
            for (MantenimientoPlan m : p.mantenimientos()) {
                if (m.estado() == EstadoMantenimiento.PROGRAMADO) {
                    conProgramado.add(m.equipo());
                    assertEquals(EstadoEquipo.EN_MANTENIMIENTO, p.equipos().get(m.equipo()).estado(),
                            "equipo con mantenimiento programado " + m.equipo());
                } else {
                    assertTrue(m.ejecucion() != null && !m.ejecucion().isAfter(p.ahora()), "cierre inválido: " + m);
                }
            }
            for (int e = 0; e < p.equipos().size(); e++) {
                if (p.equipos().get(e).estado() == EstadoEquipo.EN_MANTENIMIENTO) {
                    assertTrue(conProgramado.contains(e), "equipo " + e + " en mantenimiento sin mantenimiento abierto");
                }
            }
        }
    }

    @Test
    void elHistorialDeCadaEquipoEsContinuo_yTerminaEnSuEstadoActual() {
        for (Plan p : escenarios()) {
            for (int e = 0; e < p.equipos().size(); e++) {
                List<HistorialPlan> historial = historialDe(p, e);
                assertTrue(historial.get(0).anterior() == null, "el primer cambio es el alta");
                assertEquals("Alta del equipo", historial.get(0).motivo(), "motivo del alta");
                for (int i = 1; i < historial.size(); i++) {
                    assertEquals(historial.get(i - 1).nuevo(), historial.get(i).anterior(),
                            "cadena rota en el equipo " + e + " paso " + i);
                    assertTrue(historial.get(i).fecha().isAfter(historial.get(i - 1).fecha()),
                            "historial desordenado en el equipo " + e);
                }
                assertEquals(p.equipos().get(e).estado(), historial.get(historial.size() - 1).nuevo(),
                        "estado actual del equipo " + e);
                assertFalse(historial.get(historial.size() - 1).fecha().isAfter(p.ahora()), "historial en el futuro");
            }
        }
    }

    @Test
    void elNoShowSoloAparece_enReservasQueYaEmpezaron_yNoQuedanConfirmadasViejas() {
        for (Plan p : escenarios()) {
            for (ReservaPlan r : p.reservas()) {
                if (r.estado() == EstadoReserva.NO_SHOW) {
                    assertTrue(r.inicio().isBefore(p.ahora()), "NO_SHOW de una reserva futura: " + r);
                }
                if (r.estado() == EstadoReserva.CONFIRMADA && !r.inicio().isAfter(p.ahora())) {
                    assertTrue(r.inicio().isAfter(p.ahora().minusHours(48)),
                            "CONFIRMADA vieja sin atender: " + r);
                }
            }
        }
    }

    @Test
    void cadaSolicitudRechazadaSeSuperponeConUnaReservaActivaDelMismoEquipo() {
        for (Plan p : escenarios()) {
            for (RechazoPlan x : p.rechazos()) {
                assertFalse(x.intento().isAfter(p.ahora()), "intento en el futuro: " + x);
                assertTrue(x.fin().isAfter(x.inicio()), "rango inválido: " + x);
                boolean solapa = false;
                for (ReservaPlan r : p.reservas()) {
                    if (r.equipo() == x.equipo()
                            && (r.estado() == EstadoReserva.CONFIRMADA || r.estado() == EstadoReserva.ATENDIDA)
                            && r.inicio().isBefore(x.fin()) && r.fin().isAfter(x.inicio())) {
                        solapa = true;
                        break;
                    }
                }
                assertTrue(solapa, "el rechazo no se superpone con ninguna reserva activa: " + x);
            }
        }
    }

    @Test
    void losUsuariosYLosEquiposTienenIdentificadoresUnicos() {
        Set<String> emails = new HashSet<>();
        plan.usuarios().forEach(u -> assertTrue(emails.add(u.email()), "correo repetido: " + u.email()));
        Set<String> nombres = new HashSet<>();
        plan.equipos().forEach(e -> assertTrue(nombres.add(e.nombre()), "equipo repetido: " + e.nombre()));
    }

    @Test
    void losIndicadoresQuedanEnRangosRealistas() {
        long noShow = plan.reservas().stream().filter(r -> r.estado() == EstadoReserva.NO_SHOW).count();
        long confirmadasConservadas = plan.reservas().stream()
                .filter(r -> r.estado() != EstadoReserva.CANCELADA).count();
        double tasaNoShow = 100.0 * noShow / confirmadasConservadas;
        assertTrue(tasaNoShow > 5 && tasaNoShow < 15, "no-show " + tasaNoShow);

        double tasaConflicto = 100.0 * plan.rechazos().size() / (plan.reservas().size() + plan.rechazos().size());
        assertTrue(tasaConflicto > 4 && tasaConflicto < 10, "conflicto " + tasaConflicto);

        long devueltos = plan.prestamos().stream().filter(x -> x.estado() == EstadoPrestamo.DEVUELTO).count();
        long tarde = plan.prestamos().stream()
                .filter(x -> x.estado() == EstadoPrestamo.DEVUELTO && x.devolucion().isAfter(x.limite())).count();
        double tasaMora = 100.0 * tarde / devueltos;
        assertTrue(tasaMora > 8 && tasaMora < 20, "mora " + tasaMora);

        long cerrados = plan.mantenimientos().stream().filter(m -> m.estado() == EstadoMantenimiento.CERRADO).count();
        long tardios = plan.mantenimientos().stream()
                .filter(m -> m.estado() == EstadoMantenimiento.CERRADO && m.ejecucion().isAfter(m.programada())).count();
        double tasaTardio = 100.0 * tardios / cerrados;
        assertTrue(tasaTardio > 15 && tasaTardio < 35, "mantenimiento tardío " + tasaTardio);

        long vencidos = plan.prestamos().stream()
                .filter(x -> x.estado() == EstadoPrestamo.ACTIVO && x.limite().isBefore(plan.ahora())).count();
        assertTrue(vencidos >= 5, "préstamos vencidos: " + vencidos);

        long sobreutilizados = plan.equipos().stream().filter(e -> e.horasUso() >= 100).count();
        assertTrue(sobreutilizados >= 10, "equipos sobreutilizados: " + sobreutilizados);

        Set<Integer> conPrestamo = new HashSet<>();
        plan.prestamos().forEach(x -> conPrestamo.add(plan.reservas().get(x.reserva()).equipo()));
        long subutilizados = plan.equipos().stream()
                .filter(e -> e.horasUso() < 100 && !conPrestamo.contains(plan.equipos().indexOf(e))).count();
        assertTrue(subutilizados >= 5, "equipos subutilizados: " + subutilizados);
    }

    // --- apoyo ------------------------------------------------------------------------------

    /** El plan base y otros cinco, con la hora del día y el día de la semana distintos. */
    private List<Plan> escenarios() {
        List<Plan> lista = new ArrayList<>();
        lista.add(plan);
        for (int i = 1; i <= 5; i++) {
            LocalDateTime otraFecha = AHORA.plusDays(i * 3L).plusHours(i * 5L).plusMinutes(i * 7L);
            lista.add(PlanDatosMasivos.generar(otraFecha, SEMILLA + i, EQUIPOS));
        }
        return lista;
    }

    private List<HistorialPlan> historialDe(Plan p, int equipo) {
        List<HistorialPlan> lista = new ArrayList<>();
        for (HistorialPlan h : p.historiales()) {
            if (h.equipo() == equipo) {
                lista.add(h);
            }
        }
        lista.sort(Comparator.comparing(HistorialPlan::fecha));
        return lista;
    }
}
