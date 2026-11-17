# SmartLab Assurance

Backend del Caso 10, Desarrollo Web Integrado (100000ST61), UTP. Avance 2: JPA, JPQL, CRUD y seguridad con JWT. Preparado para el frontend del avance 3: CORS, listados por página y datos masivos.

Gestiona el catálogo de equipos de laboratorio, sus reservas, préstamos, mantenimientos e incidencias, con roles, bitácora e indicadores.

## Cómo ejecutarlo

Java 17 y Maven.

```bash
mvn spring-boot:run
```

La API queda en `http://localhost:8080`. La base es H2 en archivo (`./data/smartlabdb.mv.db`) y se conserva al reiniciar. Si arrancas con una base de una versión anterior, detén la aplicación y borra la carpeta `./data`.

| Herramienta | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI | http://localhost:8080/v3/api-docs |
| Consola H2 | http://localhost:8080/h2-console |

En la consola H2: JDBC `jdbc:h2:file:./data/smartlabdb`, usuario `sa`, sin contraseña. En Swagger, el login devuelve `token`: se pega en **Authorize** sin escribir `Bearer`.

`mvn test` usa otra base H2, en memoria, y no toca los datos de `./data`.

La primera vez que arranca tarda unos segundos más, porque carga los datos masivos (ver más abajo). Desde esta versión `spring.jpa.show-sql` está en `false`; ponlo en `true` si necesitas ver las consultas.

## Usuarios de prueba

`DatosPruebaSeeder` carga el escenario al primer arranque (`app.seed.datos-demo=true`).

| Correo | Rol | Contraseña |
|---|---|---|
| `admin@smartlab.com` | ADMINISTRADOR | `Admin123!` |
| `coordinador@smartlab.com` | COORDINADOR | `Demo123!` |
| `tecnico@smartlab.com` | TECNICO | `Demo123!` |
| `docente@smartlab.com` | DOCENTE | `Demo123!` |
| `estudiante@smartlab.com` | ESTUDIANTE | `Demo123!` |

Con la base vacía quedan 4 categorías (Óptico, Electrónico, Fabricación, Química), 8 equipos, 3 reservas del estudiante, un préstamo devuelto con mora y otro activo, un mantenimiento abierto (la centrífuga queda `EN_MANTENIMIENTO`) y uno cerrado tarde, y 2 incidencias. La impresora 3D acumula 120 h y aparece en las recomendaciones de mantenimiento.

## Datos masivos

`DatosMasivosSeeder` (`app.seed.datos-masivos=true`) completa la base hasta **180 equipos** y carga **más de 10 000 registros** coherentes con las reglas del sistema. Corre después del escenario demo, una sola vez, y si ya hay 180 equipos o más no hace nada. Para volver a empezar de cero: detener la aplicación y borrar `./data`. Para arrancar sin esta carga, `app.seed.datos-masivos=false`.

El generador (`PlanDatosMasivos`) es determinista: con la misma semilla produce la misma estructura, y las fechas se calculan hacia atrás desde el momento del arranque (casi un año de historia más 30 días de reservas futuras). Orden de magnitud de lo que carga:

| Dato | Cantidad aproximada |
|---|---|
| Usuarios (más el administrador) | 54: 40 estudiantes, 8 docentes, 4 técnicos y 2 coordinadores |
| Categorías | 8 |
| Equipos (con los 8 del escenario demo) | 180: 165 disponibles y 15 en mantenimiento |
| Reservas | unas 5 900, de ellas unas 4 300 atendidas |
| Préstamos | unos 4 300, con 8 vencidos y algunos en curso |
| Mantenimientos | unos 240: 222 cerrados y 18 programados |
| Historial de estado de equipos | unos 630 cambios |
| Solicitudes rechazadas por conflicto | unas 410 |
| Incidencias | 160 |

Los indicadores quedan así, con una variación de uno o dos puntos según la fecha de arranque: no-show cerca de 9 %, conflicto de reserva cerca de 6,5 %, mora cerca de 13 %, mantenimiento tardío cerca de 25 %, exactitud de disponibilidad 100 %, unos 35 equipos sobreutilizados (100 h o más, con la impresora 3D del escenario demo) y unos 17 subutilizados (sin ningún préstamo). Las horas de uso de cada equipo son la suma de sus préstamos devueltos, calculada igual que en el check-in.

Reglas que respeta: nunca hay dos reservas `CONFIRMADA` o `ATENDIDA` superpuestas del mismo equipo, ninguna reserva cae mientras el equipo estaba en mantenimiento, solo las reservas `ATENDIDA` tienen préstamo, y todo equipo con un mantenimiento `PROGRAMADO` está `EN_MANTENIMIENTO`. Quedan pendientes a propósito unas reservas de las últimas 48 horas en `CONFIRMADA`, para marcar NO_SHOW o hacer el check-out desde el frontend.

Los usuarios generados (`estudiante01@smartlab.com` a `estudiante39@smartlab.com`, `docente01` a `docente07`, `tecnico01` a `tecnico03` y `coordinador02`, todos con `@smartlab.com`) usan la contraseña `Demo123!`. Estos datos se escriben directo en los repositorios y no dejan registros en la auditoría.

## CORS

El frontend de Angular corre en otro origen, así que la API acepta los orígenes de `app.cors.allowed-origins` (por defecto `http://localhost:4200` y `http://127.0.0.1:4200`; varios, separados por coma). Permite `GET`, `POST`, `PUT`, `PATCH`, `DELETE` y `OPTIONS` con los encabezados `Authorization`, `Content-Type` y `Accept`, sin cookies: el token viaja en `Authorization`. La solicitud previa `OPTIONS` no necesita token. Para otro puerto o dominio, cambia esa propiedad.

## Listados por página

Con miles de registros, el frontend usa los listados por página. Los `GET` que devolvían todo (`/api/reservas`, `/api/prestamos`, `/api/mantenimientos`) siguen igual; las versiones paginadas están en `/pagina` y devuelven siempre un mismo formato:

```json
{
  "contenido": [ ... ],
  "pagina": 0,
  "tamanio": 20,
  "totalElementos": 5906,
  "totalPaginas": 296,
  "primera": true,
  "ultima": false
}
```

| Endpoint | Parámetros opcionales | Orden |
|---|---|---|
| `GET /api/reservas/pagina` | `pagina`, `tamanio`, `estado`, `equipoId`, `usuarioId` (solo técnico, coordinador y administrador; con otro rol responde 403) | inicio de la reserva, de la más reciente a la más antigua |
| `GET /api/prestamos/pagina` | `pagina`, `tamanio`, `estado` | fecha de entrega, de la más reciente a la más antigua |
| `GET /api/mantenimientos/pagina` | `pagina`, `tamanio`, `estado`, `tipo`, `equipoId` | fecha programada, de la más reciente a la más antigua |

Para el técnico hay además `GET /api/reservas/pendientes-entrega?horas=24`: las reservas `CONFIRMADA` que ya empezaron o empiezan en las próximas `horas` (1 a 168), de la más antigua a la más próxima, hasta 100. Es la lista que usa el formulario de préstamos del frontend, porque con miles de reservas no sirve cargar todas para elegir una.

`pagina` empieza en 0 (por defecto 0). `tamanio` por defecto es 20, va de 1 a 100 y los valores fuera de rango se ajustan. El orden es fijo. Un `estado` o `tipo` que no existe responde 400. Las reglas por rol son las de los listados completos: el estudiante solo ve sus reservas (y `equipoId` no le aplica), y los préstamos y mantenimientos los ve el mismo grupo de roles de siempre.

## Arquitectura

```
src/main/java/com/utp/smartlab/
├── entity/        11 entidades y 8 enums
├── repository/    Spring Data JPA, con JPQL en las consultas de negocio
├── service/       reglas, indicadores y usuario autenticado
├── controller/    REST: equipos, categorías, reservas, préstamos, mantenimientos, incidencias, auth, usuarios y auditoría
├── dto/
├── security/      JWT, filtro y configuración de roles
├── exception/
└── config/        roles, administrador, datos de ejemplo y Swagger
```

Relaciones: `Reserva`, `Mantenimiento` e `Incidencia` apuntan a `Equipo` y a `Usuario`. `Prestamo` es `@OneToOne` con `Reserva`. `Equipo` pertenece a una `Categoria`. `Usuario` tiene un `Rol` y `Auditoria` guarda al usuario que ejecutó la acción. El dueño de una reserva, un mantenimiento o una incidencia sale del token, no del body.

Estados de un equipo: `DISPONIBLE` y `EN_MANTENIMIENTO`.

## Reglas de negocio

1. Un equipo en `EN_MANTENIMIENTO` no se reserva. Programar un mantenimiento lo pasa a `EN_MANTENIMIENTO`.
2. No hay dos reservas superpuestas del mismo equipo. El intento rechazado se guarda para el indicador de conflicto.
3. El préstamo solo inicia sobre una reserva `CONFIRMADA` que todavía no tenga préstamo. El checkout la deja `ATENDIDA`.
4. Cerrar el mantenimiento devuelve el equipo a `DISPONIBLE`, salvo que tenga otro mantenimiento todavía `PROGRAMADO`: en ese caso sigue en `EN_MANTENIMIENTO` hasta cerrar el último.
5. Cada cambio de estado del equipo queda en el historial desde el alta, con el usuario responsable, el motivo y la fecha y hora. El uso real se consulta en `GET /api/equipos/{id}/utilizacion`.

Además: el registro público siempre crea `ESTUDIANTE`. El estudiante solo ve sus reservas y solo puede cancelar la suya si sigue `CONFIRMADA`. `NO_SHOW` solo lo marca el personal, y únicamente si la reserva está `CONFIRMADA` y ya inició. La tasa de inasistencia es `NO_SHOW` sobre las confirmadas que se conservan (`CONFIRMADA` + `ATENDIDA` + `NO_SHOW`); las canceladas no entran. Una incidencia `RESUELTA` no cambia de estado. No se elimina un equipo con reservas, mantenimientos, incidencias o solicitudes rechazadas, ni una categoría que ya tenga equipos. Un administrador no se cambia el rol ni se elimina, y no se borra a un usuario que ya tiene auditoría.

## Indicadores

| Indicador | Endpoint |
|---|---|
| Tasa de inasistencia (no-show) | `GET /api/reservas/reportes/no-show` |
| Tasa de conflicto de reserva | `GET /api/reservas/reportes/conflictos` |
| Tasa de mora en devolución | `GET /api/prestamos/reportes/mora` |
| Préstamos vencidos (activos con la fecha límite pasada) | `GET /api/prestamos/vencidos` |
| Tasa de mantenimiento tardío | `GET /api/mantenimientos/reportes/mantenimiento` |
| Exactitud de disponibilidad | `GET /api/equipos/reportes/disponibilidad` |
| Recomendación de mantenimiento preventivo | `GET /api/equipos/recomendaciones-mantenimiento` |
| Dashboard de utilización | `GET /api/equipos/reportes/utilizacion` |

Un préstamo `ACTIVO` cuya fecha límite ya pasó sale con `vencido: true` en su detalle y en `GET /api/prestamos/vencidos`; el reporte de mora trae también el total en `vencidos`. `moroso` sigue significando devuelto fuera de plazo, y la tasa de mora no cambia: devueltos tarde ÷ devueltos. Al hacer el check-in, el préstamo deja de estar vencido y pasa a moroso si se devolvió tarde.

El check-in suma al equipo las horas reales del préstamo. La recomendación preventiva incluye al equipo que llega a `umbralHoras` (100 por defecto) y también al que tiene un mantenimiento `PREVENTIVO` todavía `PROGRAMADO` con fecha ya vencida. El dashboard de utilización lista horas y cantidad de préstamos de cada equipo: `SOBREUTILIZADO` si las horas llegan al umbral (`umbralSobreutilizacionHoras`, 100 por defecto), `SUBUTILIZADO` si nunca tuvo un préstamo, y `NORMAL` en el resto.

## Seguridad

JWT sin sesión. Contraseñas con BCrypt. Cada método lleva `@PreAuthorize`: sin token responde 401 y con rol insuficiente, 403.

La bitácora anota las escrituras que terminan bien: registro, login, reservas, préstamos, mantenimientos, incidencias y el CRUD de categorías, equipos y usuarios. Login y registro usan el email, porque esa petición todavía no tiene token. Los datos del seeder no pasan por la API y no quedan en la auditoría.

| Endpoint | ESTUDIANTE | TECNICO | DOCENTE | COORDINADOR | ADMINISTRADOR |
|---|:-:|:-:|:-:|:-:|:-:|
| `POST /api/auth/registro`, `POST /api/auth/login` | público | público | público | público | público |
| `POST /api/equipos`, `PUT /api/equipos/{id}` | — | — | — | ✔ | ✔ |
| `DELETE /api/equipos/{id}` | — | — | — | — | ✔ |
| `POST/PUT/DELETE /api/categorias` | — | — | — | — | ✔ |
| `GET /api/categorias`, `/{id}` | — | — | — | ✔ | ✔ |
| `GET /api/equipos`, `/{id}`, `/{id}/disponibilidad` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `GET /api/equipos/{id}/historial` | — | ✔ | ✔ | ✔ | ✔ |
| `GET /api/equipos/{id}/utilizacion` | — | ✔ | ✔ | ✔ | ✔ |
| `GET /api/equipos/reportes/disponibilidad` | — | — | ✔ | ✔ | ✔ |
| `GET /api/equipos/recomendaciones-mantenimiento` | — | ✔ | — | ✔ | ✔ |
| `GET /api/equipos/reportes/utilizacion` | — | ✔ | ✔ | ✔ | ✔ |
| `POST /api/reservas` | ✔ | — | ✔ | ✔ | ✔ |
| `GET /api/reservas`, `/pagina`, `/{id}` | ✔ | ✔ | ✔ | ✔ | ✔ |
| Filtrar reservas por usuario (`usuarioId` en `/pagina`) y `GET /api/usuarios/opciones` (solo id y nombre) | — | ✔ | — | ✔ | ✔ |
| `GET /api/reservas/pendientes-entrega` | — | ✔ | — | ✔ | ✔ |
| `PATCH /api/reservas/{id}/estado` | ✔ | ✔ | ✔ | ✔ | ✔ |
| Reportes de no-show y conflictos | — | — | ✔ | ✔ | ✔ |
| Check-out y check-in | — | ✔ | — | ✔ | ✔ |
| `GET /api/prestamos`, `/pagina`, `/{id}`, `/vencidos` | — | ✔ | ✔ | ✔ | ✔ |
| Reporte de mora | — | — | ✔ | ✔ | ✔ |
| Programar y cerrar mantenimiento | — | ✔ | — | ✔ | ✔ |
| Ver mantenimientos (incluido `/pagina`) y su reporte | — | ✔ | ✔ | ✔ | ✔ |
| `POST /api/incidencias` | ✔ | ✔ | ✔ | ✔ | ✔ |
| Ver incidencias | — | ✔ | ✔ | ✔ | ✔ |
| Cambiar estado de incidencia | — | ✔ | — | ✔ | ✔ |
| `/api/usuarios` (lista completa, alta, edición y baja) y `/api/auditoria` | — | — | — | — | ✔ |

El estudiante, en reservas, solo opera las suyas. En `PATCH` únicamente puede pasar la propia de `CONFIRMADA` a `CANCELADA`. El resto de roles ve y cambia cualquier reserva.

## CRUD

| Entidad | Alta | Consulta | Cambio | Baja |
|---|---|---|---|---|
| Categoría | `POST /api/categorias` | `GET /api/categorias`, `/{id}` | `PUT /{id}` | `DELETE /{id}` |
| Equipo | `POST /api/equipos` | `GET /api/equipos`, `/{id}` | `PUT /{id}` | `DELETE /{id}` |
| Usuario | `POST /api/usuarios` y registro público | `GET /api/usuarios`, `/{id}` | `PUT /{id}` | `DELETE /{id}` |
| Reserva | `POST /api/reservas` | `GET /api/reservas`, `/pagina`, `/{id}` | `PATCH /{id}/estado` | cancelación (`CANCELADA`) |
| Préstamo | `POST /api/prestamos/checkout` | `GET /api/prestamos`, `/pagina`, `/{id}`, `/vencidos` | `POST /{id}/checkin` | — |
| Mantenimiento | `POST /api/mantenimientos` | `GET /api/mantenimientos`, `/pagina`, `/{id}` | `PATCH /{id}/cerrar` | — |
| Incidencia | `POST /api/incidencias` | `GET /api/incidencias`, `/{id}`, `/equipo/{id}` | `PATCH /{id}/estado` | — |
| Auditoría | automática | `GET /api/auditoria` | — | — |

El `PUT` de equipo cambia nombre, categoría y ubicación. El estado y las horas de uso solo cambian por mantenimiento y préstamo. En el `PUT` de usuario, `password` es opcional: si no se envía, se conserva la actual.

## Pruebas

`mvn test`: 155 tests. 66 de servicio, 71 de controller con JWT y 18 de configuración y carga de datos. Cubren las reglas, los indicadores, los préstamos vencidos, el cierre de mantenimiento con otro programado, la recomendación preventiva, el dashboard de utilización, el login, el registro público, el 401 sin token y el 403 por rol, y desde esta versión también:

- CORS: la solicitud previa del frontend se acepta y un origen desconocido se rechaza.
- Listados por página: totales, orden, filtros, límite de tamaño, 400 por filtro inválido y reservas propias del estudiante, más la cola de reservas pendientes de entrega.
- Permisos del coordinador para consultar categorías.
- Filtro de reservas por usuario (`FiltroReservasPorUsuarioControllerTest` y 3 tests de servicio): solo ese usuario, combinado con equipo y con estado, 403 para docente y estudiante, y `/api/usuarios/opciones` con solo id y nombre (sin correo ni rol).
- Generador de datos masivos (`PlanDatosMasivosTest`, sin base de datos): más de 5 000 registros, sin reservas superpuestas, sin reservas durante un mantenimiento, historial continuo, horas de uso exactas e indicadores en rangos realistas, repetido con varias fechas de arranque.
- Carga masiva sobre la aplicación completa (`DatosMasivosSeederTest`, con su propia base en memoria): 180 equipos, más de 10 000 registros, no se duplica al ejecutarse de nuevo, indicadores coherentes y paginación con miles de filas.
