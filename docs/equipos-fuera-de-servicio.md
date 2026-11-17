# Equipos fuera de servicio

La baja impide nuevas reservas y entregas sin eliminar el equipo ni su historia. La acción aparece en el detalle del equipo para COORDINADOR y ADMINISTRADOR.

## API y reglas

PATCH /api/equipos/{id}/estado recibe un objeto con estado (FUERA_DE_SERVICIO o DISPONIBLE) y motivo (obligatorio, hasta 255 caracteres). Devuelve el EquipoResponseDTO existente.

- DISPONIBLE → FUERA_DE_SERVICIO: permitido sin mantenimientos abiertos. Conserva reservas confirmadas y préstamos activos; bloquea nuevas reservas y checkout.
- FUERA_DE_SERVICIO → DISPONIBLE: requiere motivo y ausencia de préstamos activos y mantenimientos abiertos.
- EN_MANTENIMIENTO: se gestiona mediante mantenimiento, no mediante este endpoint.
- Se rechaza repetir el estado actual. Cada cambio conserva responsable autenticado, fecha, motivo y estados anterior/nuevo; el endpoint guarda también auditoría dentro de la misma transacción.
- Un préstamo activo se puede devolver aunque el equipo esté fuera de servicio. La devolución conserva la baja y el cálculo de horas y mora.
- Las reservas anteriores se gestionan explícitamente con las operaciones existentes. No se cancelan ni se marcan NO_SHOW de forma automática por la baja.
- No se programa mantenimiento sobre una baja. Cerrar el último mantenimiento solo libera un equipo EN_MANTENIMIENTO; conserva una baja que proceda de datos legados.
- Las fórmulas de indicadores y la consulta de solapamientos permanecen iguales. Un rechazo por estado no incrementa los conflictos de horario.

El campo interno equipos.version coordina reservas, entregas y cambios de estado mediante bloqueo optimista. Una operación que detecta una modificación concurrente revierte sus escrituras y responde 409; se deben actualizar los datos antes de reintentar. No se reintentan escrituras automáticamente. El registro independiente REQUIRES_NEW de solapamientos se conserva.

## Base existente

Con la aplicación detenida, aplicar backend/src/main/resources/db/manual/equipos-fuera-de-servicio.sql a la base MySQL existente antes de iniciar esta versión. Amplía los ENUM de equipos.estado y de ambos estados del historial, conservando los datos. No se ejecuta automáticamente. Una base nueva recibe los tres valores al generarse las tablas.

La configuración existente ddl-auto=update añade equipos.version BIGINT NOT NULL DEFAULT 0; el valor inicial permite conservar los registros anteriores. No basta confiar en ddl-auto=update para ampliar un ENUM existente. Si el entorno usa ddl-auto=validate, debe añadirse también esa columna antes del arranque.

La prueba EquipoSchemaUpdateTest aplica el SQL a un esquema anterior de H2 en modo MySQL, verifica los estados y horas existentes, el valor inicial de version y la persistencia de la baja. Esta prueba no sustituye validar la migración en un servidor MySQL real.

## Validación

Backend (JDK compatible con el proyecto y Maven):

~~~powershell
cd backend
mvn verify
~~~

Frontend (Node 20.11 o superior compatible con Angular 19):

~~~powershell
cd frontend
npm install
npm test
npm run build
~~~

Las pruebas frontend ejecutan las clases de componentes con inyección Angular y el runner integrado de Node, usando TypeScript ya incluido en el proyecto. Cubren filtro, etiqueta, permisos, motivo, actualización de historial, errores, doble envío y preselección de reservas. No añaden dependencias y no son pruebas de navegador.

Antes del merge, comprobar en la interfaz el flujo de baja y reactivación con un coordinador/administrador, la ausencia de la acción con otros roles y el bloqueo de entregas desde una reserva anterior. Para una base MySQL existente, validar además la migración antes de operar.
