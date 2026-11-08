# SmartLab Assurance · Frontend (Angular)

Cliente web del sistema de gestión del laboratorio. Consume el API REST del backend Spring Boot (`../backend`).

## Requisitos

- Node.js 20.19+ o 22 (con npm)
- Backend corriendo en `http://localhost:8080` (ver `../backend/README.md`)

## Ejecutar

```bash
cd frontend
npm install
npm start          # equivale a ng serve -> http://localhost:4200
```

Si el backend corre en otra URL, cambia `apiUrl` en `src/environments/environment.ts`.
El backend ya permite los orígenes `http://localhost:4200` y `http://127.0.0.1:4200` (CORS).

## Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| ADMINISTRADOR | admin@smartlab.com | Admin123! |
| COORDINADOR | coordinador@smartlab.com | Demo123! |
| DOCENTE | docente@smartlab.com | Demo123! |
| TECNICO | tecnico@smartlab.com | Demo123! |
| ESTUDIANTE | estudiante@smartlab.com | Demo123! |

Los usuarios generados por la carga masiva (`estudiante01@` a `estudiante39@`, `docente01@` a `docente07@`, `tecnico01@` a `tecnico03@` y `coordinador02@smartlab.com`) también usan `Demo123!`.

## Manejo de muchos datos

La base de prueba trae miles de registros, así que ninguna pantalla pide todo de una vez:

| Pantalla | Cómo se pagina |
|---|---|
| Reservas | En el servidor: `GET /api/reservas/pagina` (15 por página) con filtro por estado |
| Préstamos | En el servidor: `GET /api/prestamos/pagina` (15 por página) con filtro por estado; la pestaña *Vencidos* usa `/vencidos` |
| Mantenimientos | En el servidor: `GET /api/mantenimientos/pagina` (15 por página) con filtros por estado y tipo |
| Equipos (180), Panel (utilización), Incidencias y Auditoría | En el navegador: la lista llega completa y se muestra por páginas con `core/utils/paginacion.ts` |

`PaginadorComponent` (`shared/components/paginador.component.ts`) es el control de páginas de todas: recibe la página por `@Input` y avisa con `@Output` (`cambio`).

El formulario de **Préstamos** ya no baja todas las reservas para elegir una: usa `GET /api/reservas/pendientes-entrega` (las confirmadas que ya empezaron o empiezan en las próximas 72 horas). Si llegas desde *Iniciar préstamo* con una reserva que está más lejos en el tiempo, se agrega a la lista.

## Cómo cubre lo que pide el curso (APF3)

| Requisito | Dónde está |
|---|---|
| Componentes + SASS | `src/app/**` (componentes standalone) y `src/styles/` (variables, mixins, base, componentes) más un `.scss` por componente que lo necesita |
| Rutas | `app.routes.ts`: carga perezosa (`loadComponent`), rutas hijas dentro del layout y parámetros (`/equipos/:id`) |
| Comunicación entre componentes | `@Input`/`@Output`: `EquipoForm` y `ReservaForm` emiten al padre; `StatCard`, `EstadoBadge`, `FieldError` y `Paginador` reciben datos. Los parámetros de ruta y query llegan como `@Input` |
| Formularios | Reactive Forms en login, registro, equipos, reservas, préstamos, mantenimientos, incidencias, usuarios y categorías |
| Validación y manejo de errores | Validadores (`required`, `email`, `minlength`, fecha futura, rango de fechas), `FieldErrorComponent`, `mensajeDeError()` que lee el `ErrorResponse` del backend y avisos (toast) |
| Consumo REST | `core/services/*.service.ts` con `HttpClient`, un servicio por recurso del API, con listados paginados (`HttpParams`) |
| Autenticación y autorización | `AuthService` (JWT en `localStorage`, vence según el `exp` del token), `authInterceptor` (Bearer), `errorInterceptor` (401 → cierra sesión), guards `authGuard`/`rolGuard`/`invitadoGuard`, menú y botones según el rol (`core/permisos.ts`) |

Los permisos del front reflejan la matriz del backend, pero la seguridad real la aplica el backend: si un botón se ve y el rol no tiene permiso, el API responde 403 y se muestra el mensaje.

## Estructura

```
src/
├── main.ts, index.html, styles.scss
├── environments/environment.ts        URL del API
├── styles/                            SASS: _variables, _mixins, _base, _components
└── app/
    ├── app.component.ts, app.config.ts, app.routes.ts
    ├── core/                          lógica transversal (sin pantallas)
    │   ├── models/api.models.ts       tipos que reflejan los DTO del backend
    │   ├── permisos.ts                roles permitidos por funcionalidad
    │   ├── services/                  auth, notificaciones y un servicio por recurso del API
    │   ├── interceptors/              auth (Bearer) y errores (401)
    │   ├── guards/                    authGuard, rolGuard, invitadoGuard
    │   └── utils/                     mensajes de error y utilidades de fechas/validadores
    ├── shared/components/             toast, field-error, estado-badge, stat-card
    ├── layout/shell.component.*       menú lateral por rol + cabecera
    └── features/
        ├── auth/                      login, registro, sin-permiso
        ├── dashboard/                 indicadores, utilización y recomendaciones
        ├── equipos/                   listado, formulario, página de formulario, detalle
        ├── reservas/                  listado + formulario de reserva
        ├── prestamos/                 check-out, check-in y vencidos
        ├── mantenimientos/            programar y cerrar
        ├── incidencias/               reportar y gestionar
        └── admin/                     usuarios, categorías y auditoría
```
