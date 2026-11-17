package com.utp.smartlab.config.masivo;

import com.utp.smartlab.entity.EstadoEquipo;
import com.utp.smartlab.entity.EstadoIncidencia;
import com.utp.smartlab.entity.EstadoMantenimiento;
import com.utp.smartlab.entity.EstadoPrestamo;
import com.utp.smartlab.entity.EstadoReserva;
import com.utp.smartlab.entity.NombreRol;
import com.utp.smartlab.entity.TipoMantenimiento;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Genera, sin tocar la base de datos, un escenario grande y coherente con las reglas del sistema
 * (más de 5000 registros). El resultado es un plan de datos simples; {@code DatosMasivosSeeder}
 * lo convierte en entidades y lo guarda.
 *
 * Garantías del plan, que {@code PlanDatosMasivosTest} comprueba:
 * - nunca hay dos reservas CONFIRMADA/ATENDIDA superpuestas del mismo equipo;
 * - ninguna reserva cae mientras el equipo estaba en mantenimiento;
 * - toda reserva ATENDIDA tiene un préstamo, y solo esas;
 * - las horas de uso de cada equipo son la suma de las horas de sus préstamos devueltos;
 * - todo equipo con un mantenimiento PROGRAMADO está EN_MANTENIMIENTO (exactitud de 100 %);
 * - el historial de cada equipo es continuo y termina en su estado actual.
 */
public final class PlanDatosMasivos {

    public record UsuarioPlan(String nombre, String email, NombreRol rol) { }

    public record EquipoPlan(String nombre, int categoria, String ubicacion, EstadoEquipo estado, int horasUso) { }

    public record ReservaPlan(int equipo, int usuario, LocalDateTime inicio, LocalDateTime fin, EstadoReserva estado) { }

    public record PrestamoPlan(int reserva, LocalDateTime entrega, LocalDateTime limite,
                               LocalDateTime devolucion, EstadoPrestamo estado) { }

    public record MantenimientoPlan(int equipo, int usuario, TipoMantenimiento tipo, LocalDateTime programada,
                                    LocalDateTime ejecucion, EstadoMantenimiento estado) { }

    public record HistorialPlan(int equipo, EstadoEquipo anterior, EstadoEquipo nuevo, int usuario,
                                String motivo, LocalDateTime fecha) { }

    public record RechazoPlan(int equipo, LocalDateTime inicio, LocalDateTime fin, LocalDateTime intento) { }

    public record IncidenciaPlan(int equipo, int usuario, String descripcion, EstadoIncidencia estado,
                                 LocalDateTime fecha) { }

    public record Plan(LocalDateTime ahora,
                       List<String> categorias,
                       List<UsuarioPlan> usuarios,
                       List<EquipoPlan> equipos,
                       List<ReservaPlan> reservas,
                       List<PrestamoPlan> prestamos,
                       List<MantenimientoPlan> mantenimientos,
                       List<HistorialPlan> historiales,
                       List<RechazoPlan> rechazos,
                       List<IncidenciaPlan> incidencias) {

        public int totalRegistros() {
            return categorias.size() + usuarios.size() + equipos.size() + reservas.size() + prestamos.size()
                    + mantenimientos.size() + historiales.size() + rechazos.size() + incidencias.size();
        }
    }

    private PlanDatosMasivos() { }

    public static Plan generar(LocalDateTime ahora, long semilla, int cantidadEquipos) {
        return new Generador(ahora, semilla, cantidadEquipos).generar();
    }

    // ------------------------------------------------------------------------------------------

    private record TipoEquipo(String nombre, int categoria, String ubicacion) { }

    private static final List<String> CATEGORIAS = List.of(
            "Óptico", "Electrónico", "Fabricación", "Química", "Medición", "Cómputo", "Mecánica", "Seguridad");

    private static final List<TipoEquipo> TIPOS = List.of(
            new TipoEquipo("Microscopio óptico", 0, "Laboratorio 1"),
            new TipoEquipo("Espectrómetro UV", 0, "Laboratorio 1"),
            new TipoEquipo("Refractómetro", 0, "Laboratorio 1"),
            new TipoEquipo("Telescopio de práctica", 0, "Azotea de ciencias"),
            new TipoEquipo("Osciloscopio digital", 1, "Laboratorio 2"),
            new TipoEquipo("Multímetro de precisión", 1, "Laboratorio 2"),
            new TipoEquipo("Fuente de poder programable", 1, "Laboratorio 2"),
            new TipoEquipo("Generador de funciones", 1, "Laboratorio 2"),
            new TipoEquipo("Analizador lógico", 1, "Laboratorio 4"),
            new TipoEquipo("Impresora 3D", 2, "Taller 1"),
            new TipoEquipo("Cortadora láser", 2, "Taller 1"),
            new TipoEquipo("Fresadora CNC", 2, "Taller 2"),
            new TipoEquipo("Estación de soldadura", 2, "Taller 2"),
            new TipoEquipo("Centrífuga de mesa", 3, "Laboratorio 3"),
            new TipoEquipo("Balanza analítica", 3, "Laboratorio 3"),
            new TipoEquipo("Agitador magnético", 3, "Laboratorio 3"),
            new TipoEquipo("Medidor de pH", 3, "Laboratorio 3"),
            new TipoEquipo("Baño termostático", 3, "Laboratorio 5"),
            new TipoEquipo("Calibrador digital", 4, "Laboratorio 6"),
            new TipoEquipo("Anemómetro", 4, "Laboratorio 6"),
            new TipoEquipo("Termómetro infrarrojo", 4, "Laboratorio 6"),
            new TipoEquipo("Medidor de vibraciones", 4, "Laboratorio 6"),
            new TipoEquipo("Servidor de pruebas", 5, "Sala de cómputo"),
            new TipoEquipo("Estación de trabajo", 5, "Sala de cómputo"),
            new TipoEquipo("Kit Arduino", 5, "Laboratorio 4"),
            new TipoEquipo("Kit Raspberry Pi", 5, "Laboratorio 4"),
            new TipoEquipo("Banco de pruebas de motores", 6, "Taller 3"),
            new TipoEquipo("Prensa hidráulica", 6, "Taller 3"),
            new TipoEquipo("Torno de mesa", 6, "Taller 3"),
            new TipoEquipo("Cámara termográfica", 7, "Laboratorio 6"),
            new TipoEquipo("Detector de gases", 7, "Laboratorio 5"),
            new TipoEquipo("Sonómetro", 7, "Laboratorio 6"));

    private static final List<String> NOMBRES = List.of(
            "Ana", "Luis", "María", "Jorge", "Lucía", "Carlos", "Valeria", "Diego", "Sofía", "Andrés",
            "Camila", "Renato", "Daniela", "Marco", "Paola", "Hugo", "Fiorella", "Iván", "Rosa", "Esteban");

    private static final List<String> APELLIDOS = List.of(
            "Quispe", "Flores", "Rojas", "Huamán", "Mendoza", "Vargas", "Castillo", "Torres", "Salazar", "Paredes",
            "Cruz", "Ramos", "Chávez", "Soto", "Delgado", "Ortiz", "Navarro", "Campos", "Vega", "Ríos");

    private static final List<String> DESCRIPCIONES_INCIDENCIA = List.of(
            "La pantalla presenta líneas verticales al encender",
            "Vibración excesiva y ruido anormal durante el uso",
            "El equipo no enciende después de conectarlo",
            "Lectura inestable, los valores cambian sin motivo",
            "Cable de alimentación dañado en la conexión",
            "Falta un accesorio que debía venir con el equipo",
            "El equipo se apaga solo tras unos minutos de uso",
            "Tornillos flojos en la base, el equipo se mueve",
            "Calibración fuera de rango según el patrón de referencia",
            "Mensaje de error al iniciar la autoprueba",
            "Sobrecalentamiento durante una sesión larga",
            "Botón de encendido atascado");

    private static final int[] INICIOS_BLOQUE = {8, 11, 14, 17};

    private static final class Generador {

        private final LocalDateTime ahora;
        private final Random rnd;
        private final int n;

        private final List<UsuarioPlan> usuarios = new ArrayList<>();
        private final List<Integer> estudiantes = new ArrayList<>();
        private final List<Integer> docentes = new ArrayList<>();
        private final List<Integer> tecnicos = new ArrayList<>();
        private final List<Integer> coordinadores = new ArrayList<>();

        private final List<ReservaPlan> reservas = new ArrayList<>();
        private final List<PrestamoPlan> prestamos = new ArrayList<>();
        private final List<MantenimientoPlan> mantenimientos = new ArrayList<>();
        private final List<HistorialPlan> historiales = new ArrayList<>();
        private final List<RechazoPlan> rechazos = new ArrayList<>();
        private final List<IncidenciaPlan> incidencias = new ArrayList<>();

        private String[] nombreEquipo;
        private int[] categoriaEquipo;
        private String[] ubicacionEquipo;
        private EstadoEquipo[] estadoEquipo;
        private int[] horasEquipo;
        private double[] peso;
        private LocalDateTime[] tope;
        private List<List<LocalDateTime[]>> bloqueos;

        private List<Integer> abiertos;

        Generador(LocalDateTime ahora, long semilla, int n) {
            this.ahora = ahora;
            this.rnd = new Random(semilla);
            this.n = Math.max(n, 0);
        }

        Plan generar() {
            generarUsuarios();
            generarEquipos();
            generarMantenimientos();
            generarReservasEspeciales();
            generarReservasDeLaRejilla();
            generarRechazos();
            generarIncidencias();

            List<EquipoPlan> equipos = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                equipos.add(new EquipoPlan(nombreEquipo[i], categoriaEquipo[i], ubicacionEquipo[i],
                        estadoEquipo[i], horasEquipo[i]));
            }
            return new Plan(ahora, CATEGORIAS, List.copyOf(usuarios), equipos, reservas, prestamos,
                    mantenimientos, historiales, rechazos, incidencias);
        }

        // --- usuarios ---------------------------------------------------------------------

        private void generarUsuarios() {
            agregarUsuario("Carla Estudiante", "estudiante@smartlab.com", NombreRol.ESTUDIANTE, estudiantes);
            agregarUsuario("Tomás Técnico", "tecnico@smartlab.com", NombreRol.TECNICO, tecnicos);
            agregarUsuario("Diana Docente", "docente@smartlab.com", NombreRol.DOCENTE, docentes);
            agregarUsuario("Camilo Coordinador", "coordinador@smartlab.com", NombreRol.COORDINADOR, coordinadores);

            for (int i = 1; i <= 39; i++) {
                agregarUsuario(nombreAleatorio(), String.format("estudiante%02d@smartlab.com", i),
                        NombreRol.ESTUDIANTE, estudiantes);
            }
            for (int i = 1; i <= 7; i++) {
                agregarUsuario("Prof. " + nombreAleatorio(), String.format("docente%02d@smartlab.com", i),
                        NombreRol.DOCENTE, docentes);
            }
            for (int i = 1; i <= 3; i++) {
                agregarUsuario(nombreAleatorio(), String.format("tecnico%02d@smartlab.com", i),
                        NombreRol.TECNICO, tecnicos);
            }
            agregarUsuario(nombreAleatorio(), "coordinador02@smartlab.com", NombreRol.COORDINADOR, coordinadores);
        }

        private void agregarUsuario(String nombre, String email, NombreRol rol, List<Integer> grupo) {
            usuarios.add(new UsuarioPlan(nombre, email, rol));
            grupo.add(usuarios.size() - 1);
        }

        private String nombreAleatorio() {
            return NOMBRES.get(rnd.nextInt(NOMBRES.size())) + " " + APELLIDOS.get(rnd.nextInt(APELLIDOS.size()));
        }

        // --- equipos ----------------------------------------------------------------------

        private void generarEquipos() {
            nombreEquipo = new String[n];
            categoriaEquipo = new int[n];
            ubicacionEquipo = new String[n];
            estadoEquipo = new EstadoEquipo[n];
            horasEquipo = new int[n];
            peso = new double[n];
            tope = new LocalDateTime[n];
            abiertos = new ArrayList<>();
            bloqueos = new ArrayList<>();

            int[] contadorPorTipo = new int[TIPOS.size()];
            LocalDateTime finDelMundo = ahora.plusYears(10);

            for (int i = 0; i < n; i++) {
                int t = i % TIPOS.size();
                contadorPorTipo[t]++;
                TipoEquipo tipo = TIPOS.get(t);
                nombreEquipo[i] = tipo.nombre() + " " + String.format("%03d", contadorPorTipo[t]);
                categoriaEquipo[i] = tipo.categoria();
                ubicacionEquipo[i] = tipo.ubicacion();
                estadoEquipo[i] = EstadoEquipo.DISPONIBLE;
                tope[i] = finDelMundo;
                bloqueos.add(new ArrayList<>());

                double r = rnd.nextDouble();
                peso[i] = r < 0.10 ? 0.0 : r < 0.35 ? 0.3 : r < 0.82 ? 1.0 : 4.5;

                // Alta del equipo: antes de todo lo demás
                historiales.add(new HistorialPlan(i, null, EstadoEquipo.DISPONIBLE, pick(coordinadores),
                        "Alta del equipo", ahora.minusDays(345).minusHours(rnd.nextInt(96))));
            }

            // Equipos especiales: los que tienen un mantenimiento abierto
            List<Integer> orden = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                orden.add(i);
            }
            Collections.shuffle(orden, rnd);

            int nAbiertos = Math.min(14, n / 6);
            for (int k = 0; k < nAbiertos; k++) {
                abiertos.add(orden.get(k));
            }
        }

        // --- mantenimientos ---------------------------------------------------------------

        private void generarMantenimientos() {
            // Mantenimientos ya cerrados, repartidos en el último año
            for (int i = 0; i < n; i++) {
                int k = cantidadDeCerrados();
                List<Integer> dias = new ArrayList<>();
                int intentos = 0;
                while (dias.size() < k && intentos++ < 40) {
                    int d = 25 + rnd.nextInt(296);
                    boolean separado = true;
                    for (int otro : dias) {
                        if (Math.abs(otro - d) < 20) {
                            separado = false;
                        }
                    }
                    if (separado) {
                        dias.add(d);
                    }
                }
                for (int d : dias) {
                    LocalDateTime programada = ahora.minusDays(d).withHour(8 + rnd.nextInt(9))
                            .withMinute(0).withSecond(0).withNano(0);
                    TipoMantenimiento tipo = rnd.nextDouble() < 0.60
                            ? TipoMantenimiento.PREVENTIVO : TipoMantenimiento.CORRECTIVO;
                    boolean tardio = rnd.nextDouble() < 0.25;
                    LocalDateTime bloqueo = programada.minusDays(2 + rnd.nextInt(6)).minusHours(rnd.nextInt(6));
                    LocalDateTime cierre = tardio
                            ? programada.plusHours(1 + rnd.nextInt(72))
                            : programada.minusHours(1 + rnd.nextInt(24));
                    int tecnico = pick(tecnicos);

                    mantenimientos.add(new MantenimientoPlan(i, tecnico, tipo, programada, cierre,
                            EstadoMantenimiento.CERRADO));
                    historiales.add(new HistorialPlan(i, EstadoEquipo.DISPONIBLE, EstadoEquipo.EN_MANTENIMIENTO,
                            tecnico, "Mantenimiento " + tipo + " programado", bloqueo));
                    historiales.add(new HistorialPlan(i, EstadoEquipo.EN_MANTENIMIENTO, EstadoEquipo.DISPONIBLE,
                            tecnico, "Mantenimiento cerrado", cierre));
                    bloqueos.get(i).add(new LocalDateTime[] {bloqueo, cierre});
                }
            }

            // Mantenimientos abiertos: el equipo sigue bloqueado. Los tres primeros tienen dos.
            int dobles = Math.min(3, abiertos.size());
            for (int j = 0; j < abiertos.size(); j++) {
                int i = abiertos.get(j);
                boolean doble = j < dobles;
                int diasAtras = doble ? 4 + rnd.nextInt(6) : 1 + rnd.nextInt(9);
                LocalDateTime bloqueo = ahora.minusDays(diasAtras).minusHours(rnd.nextInt(10));
                int tecnico = pick(tecnicos);

                TipoMantenimiento tipo1 = tipoAleatorio();
                mantenimientos.add(new MantenimientoPlan(i, tecnico, tipo1, programadaAbierta(bloqueo, tipo1),
                        null, EstadoMantenimiento.PROGRAMADO));
                historiales.add(new HistorialPlan(i, EstadoEquipo.DISPONIBLE, EstadoEquipo.EN_MANTENIMIENTO,
                        tecnico, "Mantenimiento " + tipo1 + " programado", bloqueo));

                if (doble) {
                    LocalDateTime segundo = bloqueo.plusDays(1 + rnd.nextInt(2));
                    TipoMantenimiento tipo2 = tipoAleatorio();
                    mantenimientos.add(new MantenimientoPlan(i, tecnico, tipo2, programadaAbierta(segundo, tipo2),
                            null, EstadoMantenimiento.PROGRAMADO));
                    historiales.add(new HistorialPlan(i, EstadoEquipo.EN_MANTENIMIENTO,
                            EstadoEquipo.EN_MANTENIMIENTO, tecnico, "Mantenimiento " + tipo2 + " programado", segundo));
                }

                estadoEquipo[i] = EstadoEquipo.EN_MANTENIMIENTO;
                tope[i] = bloqueo;
            }
        }

        private LocalDateTime programadaAbierta(LocalDateTime base, TipoMantenimiento tipo) {
            int maxDias = tipo == TipoMantenimiento.PREVENTIVO ? 9 : 6;
            return base.plusDays(rnd.nextInt(maxDias)).plusHours(1 + rnd.nextInt(5));
        }

        private TipoMantenimiento tipoAleatorio() {
            return rnd.nextBoolean() ? TipoMantenimiento.PREVENTIVO : TipoMantenimiento.CORRECTIVO;
        }

        private int cantidadDeCerrados() {
            double r = rnd.nextDouble();
            return r < 0.35 ? 0 : r < 0.65 ? 1 : r < 0.85 ? 2 : r < 0.95 ? 3 : 4;
        }

        // --- reservas ---------------------------------------------------------------------

        /** Préstamos activos: unos en curso y otros vencidos (fecha límite ya pasada). */
        private void generarReservasEspeciales() {
            List<Integer> candidatos = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (peso[i] > 0 && estadoEquipo[i] == EstadoEquipo.DISPONIBLE) {
                    candidatos.add(i);
                }
            }
            Collections.shuffle(candidatos, rnd);

            int enCurso = Math.min(6, candidatos.size());
            for (int k = 0; k < enCurso; k++) {
                int i = candidatos.get(k);
                LocalDateTime inicio = ahora.minusHours(1);
                agregarReservaAtendida(i, inicio, inicio.plusHours(3), inicio.plusMinutes(5), null);
            }

            int vencidos = Math.min(8, Math.max(candidatos.size() - enCurso, 0));
            for (int k = 0; k < vencidos; k++) {
                int i = candidatos.get(enCurso + k);
                LocalDateTime inicio = ahora.minusHours(20 + rnd.nextInt(27));
                agregarReservaAtendida(i, inicio, inicio.plusHours(2), inicio.plusMinutes(7), null);
            }
        }

        private void generarReservasDeLaRejilla() {
            List<LocalDateTime> rejilla = new ArrayList<>();
            for (int d = -330; d <= 30; d++) {
                var fecha = ahora.toLocalDate().plusDays(d);
                if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    continue;
                }
                for (int hora : INICIOS_BLOQUE) {
                    rejilla.add(fecha.atTime(hora, 0));
                }
            }

            double sumaPesos = 0;
            for (double p : peso) {
                sumaPesos += p;
            }
            if (sumaPesos == 0) {
                return;
            }
            double objetivoTotal = 34.0 * n;

            for (int i = 0; i < n; i++) {
                if (peso[i] == 0) {
                    continue;
                }
                int cuantas = (int) Math.round(objetivoTotal * peso[i] / sumaPesos);
                List<LocalDateTime> candidatos = new ArrayList<>(rejilla);
                Collections.shuffle(candidatos, rnd);

                int creadas = 0;
                for (LocalDateTime inicio : candidatos) {
                    if (creadas >= cuantas) {
                        break;
                    }
                    if (crearReservaDeRejilla(i, inicio)) {
                        creadas++;
                    }
                }
            }
        }

        private boolean crearReservaDeRejilla(int equipo, LocalDateTime inicio) {
            double d = rnd.nextDouble();
            int horas = d < 0.15 ? 1 : d < 0.60 ? 2 : 3;
            LocalDateTime fin = inicio.plusHours(horas);
            if (!estaLibre(equipo, inicio, fin)) {
                return false;
            }

            EstadoReserva estado;
            if (inicio.isAfter(ahora)) {
                estado = rnd.nextDouble() < 0.10 ? EstadoReserva.CANCELADA : EstadoReserva.CONFIRMADA;
            } else if (inicio.isAfter(ahora.minusHours(48)) && rnd.nextDouble() < 0.35) {
                estado = EstadoReserva.CONFIRMADA;
            } else {
                double r = rnd.nextDouble();
                estado = r < 0.78 ? EstadoReserva.ATENDIDA : r < 0.87 ? EstadoReserva.NO_SHOW : EstadoReserva.CANCELADA;
            }

            if (estado == EstadoReserva.ATENDIDA) {
                LocalDateTime entrega = inicio.plusMinutes(rnd.nextInt(11));
                if (entrega.isAfter(ahora)) {
                    entrega = ahora;
                }
                LocalDateTime devolucion = null;
                if (!fin.isAfter(ahora)) {
                    boolean tarde = rnd.nextDouble() < 0.13;
                    devolucion = tarde
                            ? fin.plusMinutes(5 + rnd.nextInt(236))
                            : fin.minusMinutes(rnd.nextInt(21));
                    if (devolucion.isAfter(ahora)) {
                        devolucion = ahora.minusMinutes(1);
                    }
                }
                agregarReservaAtendida(equipo, inicio, fin, entrega, devolucion);
            } else {
                reservas.add(new ReservaPlan(equipo, quienReserva(), inicio, fin, estado));
                if (estado == EstadoReserva.CONFIRMADA) {
                    // las confirmadas ocupan el horario
                    bloqueos.get(equipo).add(new LocalDateTime[] {inicio, fin});
                }
            }
            return true;
        }

        /** Reserva ATENDIDA con su préstamo. Sin devolución, el préstamo queda ACTIVO. */
        private void agregarReservaAtendida(int equipo, LocalDateTime inicio, LocalDateTime fin,
                                            LocalDateTime entrega, LocalDateTime devolucion) {
            reservas.add(new ReservaPlan(equipo, quienReserva(), inicio, fin, EstadoReserva.ATENDIDA));
            bloqueos.get(equipo).add(new LocalDateTime[] {inicio, fin});
            int indice = reservas.size() - 1;

            if (devolucion == null) {
                prestamos.add(new PrestamoPlan(indice, entrega, fin, null, EstadoPrestamo.ACTIVO));
            } else {
                prestamos.add(new PrestamoPlan(indice, entrega, fin, devolucion, EstadoPrestamo.DEVUELTO));
                horasEquipo[equipo] += (int) Math.max(Duration.between(entrega, devolucion).toHours(), 0);
            }
        }

        private boolean estaLibre(int equipo, LocalDateTime inicio, LocalDateTime fin) {
            if (fin.isAfter(tope[equipo])) {
                return false;
            }
            for (LocalDateTime[] b : bloqueos.get(equipo)) {
                if (b[0].isBefore(fin) && b[1].isAfter(inicio)) {
                    return false;
                }
            }
            return true;
        }

        private int quienReserva() {
            return rnd.nextDouble() < 0.70 ? pick(estudiantes) : pick(docentes);
        }

        // --- intentos rechazados e incidencias --------------------------------------------

        private void generarRechazos() {
            List<Integer> activas = new ArrayList<>();
            for (int r = 0; r < reservas.size(); r++) {
                EstadoReserva e = reservas.get(r).estado();
                if (e == EstadoReserva.CONFIRMADA || e == EstadoReserva.ATENDIDA) {
                    activas.add(r);
                }
            }
            if (activas.isEmpty()) {
                return;
            }

            int cantidad = (int) Math.round(0.07 * reservas.size());
            for (int k = 0; k < cantidad; k++) {
                ReservaPlan existente = reservas.get(activas.get(rnd.nextInt(activas.size())));
                int duracion = (int) Duration.between(existente.inicio(), existente.fin()).toMinutes();

                // La solicitud empieza poco antes o durante la reserva existente, así que se superpone
                LocalDateTime inicio = existente.inicio().minusMinutes(44).plusMinutes(1 + rnd.nextInt(duracion + 30));
                LocalDateTime fin = inicio.plusMinutes(60 + rnd.nextInt(61));

                LocalDateTime intento = existente.inicio().minusHours(1 + rnd.nextInt(24 * 10));
                if (intento.isAfter(ahora)) {
                    intento = ahora.minusMinutes(1 + rnd.nextInt(600));
                }
                rechazos.add(new RechazoPlan(existente.equipo(), inicio, fin, intento));
            }
        }

        private void generarIncidencias() {
            List<Integer> reportan = new ArrayList<>(estudiantes);
            reportan.addAll(docentes);
            reportan.addAll(tecnicos);

            for (int k = 0; k < 160 && n > 0; k++) {
                double r = rnd.nextDouble();
                EstadoIncidencia estado = r < 0.35 ? EstadoIncidencia.ABIERTA
                        : r < 0.60 ? EstadoIncidencia.EN_REVISION : EstadoIncidencia.RESUELTA;
                incidencias.add(new IncidenciaPlan(
                        rnd.nextInt(n),
                        pick(reportan),
                        DESCRIPCIONES_INCIDENCIA.get(rnd.nextInt(DESCRIPCIONES_INCIDENCIA.size())),
                        estado,
                        ahora.minusMinutes(1 + rnd.nextInt(300 * 24 * 60))));
            }
        }

        private int pick(List<Integer> grupo) {
            return grupo.get(rnd.nextInt(grupo.size()));
        }
    }
}
