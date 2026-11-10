# SmartLab Assurance

Sistema de gestión de equipos de laboratorio (Caso 10 · Desarrollo Web Integrado, UTP).

- `backend/`: API REST con Spring Boot, JWT y MySQL.
- `frontend/`: cliente Angular que consume el API.
- `docs/`: guía de flujos E2E para probar front y back juntos.

Los tests del backend siguen usando H2 en memoria. No hace falta MySQL para `mvn test`.

## Levantar todo

MySQL tiene que estar encendido. Crea la base una sola vez:

```sql
CREATE DATABASE smartlab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

En `backend/src/main/resources/application.properties` revisa usuario, clave y puerto (`localhost:3306` por defecto).

Terminal 1 (backend, puerto 8080):

```bash
cd backend
mvn spring-boot:run
```

La primera vez Hibernate crea las tablas y los seeders cargan los datos. Puede tardar un poco.

Terminal 2 (frontend, puerto 4200):

```bash
cd frontend
npm install
npm start
```

Abre http://localhost:4200. Swagger sigue disponible en http://localhost:8080/swagger-ui.html.

Usuarios de prueba: `admin@smartlab.com` / `Admin123!` y, para los demás roles, `estudiante@`, `tecnico@`, `docente@` o `coordinador@smartlab.com` con `Demo123!`. La carga masiva agrega `estudiante01@` a `estudiante39@`, `docente01@` a `docente07@`, `tecnico01@` a `tecnico03@` y `coordinador02@smartlab.com`, todos con `Demo123!`.

## Datos de prueba

La primera vez que arranca el backend carga 180 equipos y más de 10 000 registros coherentes (unas 5 900 reservas, 4 300 préstamos, 240 mantenimientos, historial de estado, solicitudes rechazadas e incidencias). Se carga una sola vez: si ya hay 180 equipos o más, no se vuelve a insertar.

Para empezar de cero, detén el backend y vacía MySQL:

```sql
DROP DATABASE smartlab;
CREATE DATABASE smartlab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Luego vuelve a arrancar. El detalle de cantidades, indicadores y reglas está en `backend/README.md`. Si no quieres la carga masiva, pon `app.seed.datos-masivos=false`.

Con tantos registros, Reservas, Préstamos y Mantenimientos se paginan en el servidor (`/pagina`), y Equipos, Panel, Incidencias y Auditoría se paginan en el navegador.

## Estructura del repositorio

```
smartlab-assurance/
├── README.md
├── .gitignore
├── docs/
│   └── FLUJOS_E2E.md
├── backend/
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/java/com/utp/smartlab/
│       │   ├── config/          seeders (demo y datos masivos) y configuración de OpenAPI
│       │   ├── controller/      endpoints REST
│       │   ├── dto/             objetos de entrada y salida
│       │   ├── entity/          entidades JPA y enums
│       │   ├── exception/       excepciones y manejador global
│       │   ├── repository/      Spring Data JPA
│       │   ├── security/        JWT, SecurityConfig (incluye CORS)
│       │   └── service/         reglas de negocio
│       ├── main/resources/application.properties
│       └── test/                pruebas de servicio y de controller (H2 en memoria)
└── frontend/
    ├── package.json, angular.json, tsconfig*.json
    ├── README.md
    └── src/
        ├── main.ts, index.html, styles.scss
        ├── environments/        URL del API
        ├── styles/              SASS (variables, mixins, base, componentes)
        └── app/
            ├── core/            modelos, servicios, interceptores, guards, utilidades
            ├── shared/          componentes reutilizables
            ├── layout/          menú y cabecera
            └── features/        auth, dashboard, equipos, reservas, préstamos,
                                 mantenimientos, incidencias y admin
```

## Cambios del backend para el front end

- CORS en `SecurityConfig`: permite `http://localhost:4200` y `http://127.0.0.1:4200` (propiedad `app.cors.allowed-origins`).
- `GET /api/categorias` y `/{id}` también para el COORDINADOR: necesita la lista para registrar equipos. Crear, editar y eliminar categorías sigue siendo solo del ADMINISTRADOR.
- Listados por página (`/api/reservas/pagina`, `/api/prestamos/pagina`, `/api/mantenimientos/pagina`) y `GET /api/reservas/pendientes-entrega`, la cola de entregas del técnico.
- Filtro de reservas por usuario (`usuarioId` en `/api/reservas/pagina`) y `GET /api/usuarios/opciones` (solo id y nombre), ambos solo para técnico, coordinador y administrador.
- Filtro por rango de fechas (`desde` y `hasta`) en préstamos, reservas y mantenimientos. Si `desde` es posterior a `hasta`, la API responde 400.
- Carga masiva de datos (`DatosMasivosSeeder`) con su generador y pruebas.
- MySQL en el arranque de la aplicación. H2 queda solo para los tests.
- Un valor inválido en un filtro de la URL responde 400 y no 500.
- Nuevos tests: CORS, paginación, generador de datos, carga masiva y permisos del coordinador sobre categorías. En total, 155 tests.
