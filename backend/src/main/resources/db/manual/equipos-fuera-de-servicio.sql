-- Ejecutar una vez antes de arrancar la nueva versión sobre una base existente.
-- Conserva todos los registros y amplía únicamente los valores permitidos.
-- Hibernate ddl-auto=update añade equipos.version BIGINT NOT NULL DEFAULT 0.
ALTER TABLE equipos MODIFY COLUMN estado ENUM('DISPONIBLE', 'EN_MANTENIMIENTO', 'FUERA_DE_SERVICIO') NOT NULL;
ALTER TABLE historial_estado_equipo MODIFY COLUMN estado_anterior ENUM('DISPONIBLE', 'EN_MANTENIMIENTO', 'FUERA_DE_SERVICIO') NULL;
ALTER TABLE historial_estado_equipo MODIFY COLUMN estado_nuevo ENUM('DISPONIBLE', 'EN_MANTENIMIENTO', 'FUERA_DE_SERVICIO') NOT NULL;
