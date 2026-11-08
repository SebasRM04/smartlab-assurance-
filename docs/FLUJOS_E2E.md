# Flujos E2E críticos (front + back)

Para probarlos, levanta el backend (`mvn spring-boot:run`) y el frontend (`npm start`), y abre http://localhost:4200.
Las fechas se eligen en los campos de fecha de cada formulario. Las del ejemplo son orientativas.

## Flujo 1 · Autenticación y autorización
1. Abre `/dashboard` sin sesión: te manda a `/login?returnUrl=/dashboard`.
2. Ingresa con una contraseña incorrecta: aparece el mensaje de credenciales inválidas del backend.
3. Ingresa como `estudiante@smartlab.com`: el menú solo muestra Panel, Equipos, Reservas e Incidencias.
4. Escribe en la barra `/admin/usuarios`: te lleva a "Sin permiso".
5. Cierra sesión y entra como `admin@smartlab.com`: aparecen Usuarios, Categorías y Auditoría.
6. Registro público: en `/registro` crea una cuenta; entra con ella y comprueba que es ESTUDIANTE.

## Flujo 2 · Reserva sin conflicto (estudiante)
1. Entra como `estudiante@smartlab.com`, ve a Reservas y crea una reserva de un equipo disponible, mañana.
2. Crea otra del mismo equipo en un horario que se cruce: se muestra el error 409 de conflicto (queda contado en el indicador de conflicto).
3. Cancela la primera: pasa a `CANCELADA`.

## Flujo 3 · Préstamo completo (técnico)
1. Como estudiante, crea una reserva que empiece en una hora.
2. Entra como `tecnico@smartlab.com` y ve a Préstamos: la reserva aparece en el selector (se listan las confirmadas que empiezan en las próximas 72 horas). Elígela. También puedes ir a Reservas, filtrar por `CONFIRMADA` y pulsar **Iniciar préstamo** sobre ella, que abre Préstamos con la reserva elegida.
3. Fecha límite: pon una hora dentro de 2 minutos y registra la entrega. La reserva queda `ATENDIDA`.
4. Espera 2 minutos y recarga Préstamos: sale la alerta **Vencido** y el contador en la pestaña *Vencidos* (con los datos masivos ya hay 8 vencidos antes de empezar, así que el contador sube a 9).
5. Pulsa **Registrar devolución**: queda `DEVUELTO` con alerta *Devuelto con mora*; el equipo suma horas de uso.

## Flujo 4 · Mantenimiento y disponibilidad (coordinador o técnico)
1. En Mantenimientos programa dos mantenimientos para el mismo equipo (elige uno `DISPONIBLE`). El equipo pasa a `EN_MANTENIMIENTO`.
2. En el detalle del equipo, el estudiante ve que no se puede reservar.
3. Cierra el primero: el equipo **sigue** `EN_MANTENIMIENTO`.
4. Cierra el segundo: el equipo vuelve a `DISPONIBLE`.
5. El historial del equipo muestra solo un cambio a `DISPONIBLE`.

## Flujo 5 · Equipos y categorías (coordinador/administrador)
1. Como administrador, crea una categoría en Categorías.
2. Como coordinador, en Equipos pulsa **Nuevo equipo**, elige la categoría y guarda (el equipo nace `DISPONIBLE`).
3. Intenta guardar sin nombre: el campo muestra el error y no se llama al API.

## Flujo 6 · Incidencias
1. Como estudiante, reporta una incidencia (descripción de al menos 10 caracteres).
2. Como técnico, en Incidencias pásala a *En revisión* y luego *Resolver*.

## Flujo 7 · Indicadores (docente, coordinador, administrador)
1. En el Panel: tasa de inasistencia, de conflicto, de mora, préstamos vencidos, mantenimiento tardío y exactitud de disponibilidad.
2. Cambia el umbral de horas y pulsa **Actualizar** para ver la utilización y las recomendaciones de mantenimiento preventivo.

## Flujo 8 · Muchos datos y paginación
Requiere la carga masiva (activa por defecto la primera vez que arranca el backend).
1. Como administrador, abre Reservas: hay unas 5 900, se muestran 15 por página y el paginador permite ir a la primera, anterior, siguiente y última. Filtra por `NO_SHOW` o `CANCELADA`: el total y las páginas cambian y vuelves a la página 1.
2. Abre Préstamos: la lista llega por páginas; filtra por `ACTIVO` o `DEVUELTO`. La pestaña *Vencidos* muestra los 8 de la carga.
3. Abre Mantenimientos y filtra por estado `PROGRAMADO`: hay 18 (algunos equipos tienen dos). Combina con el tipo `PREVENTIVO`.
4. Abre Equipos: son 180, 20 por página. Busca "Microscopio": el paginador se ajusta a los resultados. Filtra por estado `EN_MANTENIMIENTO` (15).
5. Entra como `estudiante05@smartlab.com` (`Demo123!`): en Reservas solo ve las suyas (unas cien).
6. En el Panel, la tabla de utilización está ordenada por horas de uso: arriba están los sobreutilizados (unos 35) y los equipos sin ningún préstamo quedan al final como subutilizados.
7. Como técnico, abre Reservas, filtra por `CONFIRMADA` y revisa las que empezaron hace menos de 48 horas: son las que se pueden marcar `NO_SHOW` o convertir en préstamo.

