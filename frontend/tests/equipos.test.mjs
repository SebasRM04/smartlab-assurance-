import '@angular/compiler';
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createEnvironmentInjector, runInInjectionContext } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { EstadoBadgeComponent } from '../src/app/shared/components/estado-badge.component.ts';
import { EquiposListComponent } from '../src/app/features/equipos/equipos-list.component.ts';
import { EquipoDetalleComponent } from '../src/app/features/equipos/equipo-detalle.component.ts';
import { ReservaFormComponent } from '../src/app/features/reservas/reserva-form.component.ts';
import { EquipoService } from '../src/app/core/services/equipo.service.ts';
import { AuthService } from '../src/app/core/services/auth.service.ts';
import { NotificationService } from '../src/app/core/services/notification.service.ts';
import { IncidenciaService } from '../src/app/core/services/incidencia.service.ts';
import { ESTADOS_EQUIPO } from '../src/app/core/models/api.models.ts';

const equipo = { id: 1, nombre: 'Microscopio', categoriaNombre: 'Óptica', estado: 'DISPONIBLE' };
function contexto(tipo, rol = 'ADMINISTRADOR', api = {}) {
  const llamadas = [];
  const injector = createEnvironmentInjector([
    { provide: FormBuilder, useValue: new FormBuilder() },
    { provide: AuthService, useValue: { tieneRol: roles => roles.includes(rol) } },
    { provide: EquipoService, useValue: { historial: () => of([]), cambiarEstado: (id, dto) => {
      llamadas.push({ id, dto }); return of({ ...equipo, estado: dto.estado }); }, ...api } },
    { provide: NotificationService, useValue: { exito() {}, error() {} } },
    { provide: IncidenciaService, useValue: {} },
    { provide: Router, useValue: {} },
  ]);
  const componente = runInInjectionContext(injector, () => new tipo());
  return { componente, llamadas, injector };
}

test('El catálogo filtra fuera de servicio y conserva los demás estados', () => {
  assert.ok(ESTADOS_EQUIPO.includes('FUERA_DE_SERVICIO'));
  const { componente: c, injector } = contexto(EquiposListComponent);
  c.equipos.set([equipo, { ...equipo, id: 2, estado: 'FUERA_DE_SERVICIO' }]);
  c.elegirEstado('FUERA_DE_SERVICIO');
  assert.deepEqual(c.visibles().map(e => e.id), [2]);
  c.elegirEstado(''); assert.equal(c.filtrados().length, 2);
  injector.destroy();
});
test('La insignia muestra FUERA DE SERVICIO con el tono de alerta', () => {
  const badge = new EstadoBadgeComponent(); badge.estado = 'FUERA_DE_SERVICIO';
  assert.equal(badge.texto, 'FUERA DE SERVICIO'); assert.equal(badge.tono, 'danger');
});
for (const rol of ['ADMINISTRADOR', 'COORDINADOR', 'TECNICO', 'DOCENTE', 'ESTUDIANTE']) {
  test('Permiso de cambio de estado para ' + rol, () => {
    const { componente: c, llamadas, injector } = contexto(EquipoDetalleComponent, rol);
    c.equipo.set(equipo); c.estadoForm.controls.motivo.setValue('Avería'); c.cambiarEstado();
    assert.equal(llamadas.length, ['ADMINISTRADOR', 'COORDINADOR'].includes(rol) ? 1 : 0);
    injector.destroy();
  });
}
test('La baja actualiza disponibilidad e historial y exige motivo', () => {
  let lecturas = 0;
  const { componente: c, llamadas, injector } = contexto(EquipoDetalleComponent, 'ADMINISTRADOR', {
    historial: () => { lecturas++; return of([{ estadoNuevo: 'FUERA_DE_SERVICIO', motivo: 'Avería' }]); }
  });
  c.id = '1'; c.equipo.set(equipo);
  c.estadoForm.controls.motivo.setValue('   '); c.cambiarEstado(); assert.equal(llamadas.length, 0);
  c.estadoForm.controls.motivo.setValue('  Avería  '); c.cambiarEstado();
  assert.deepEqual(llamadas[0], { id: 1, dto: { estado: 'FUERA_DE_SERVICIO', motivo: 'Avería' } });
  assert.equal(c.disponibilidad(), 'FUERA_DE_SERVICIO'); assert.equal(lecturas, 1);
  assert.equal(c.historial()[0].estadoNuevo, 'FUERA_DE_SERVICIO');
  c.estadoForm.controls.motivo.setValue('Revisado'); c.cambiarEstado();
  assert.equal(llamadas[1].dto.estado, 'DISPONIBLE');
  injector.destroy();
});
test('Un rechazo conserva el estado y permite corregir el formulario', () => {
  const { componente: c, injector } = contexto(EquipoDetalleComponent, 'ADMINISTRADOR', {
    cambiarEstado: () => throwError(() => ({ status: 409 }))
  });
  c.equipo.set(equipo); c.estadoForm.controls.motivo.setValue('Avería'); c.cambiarEstado();
  assert.equal(c.equipo().estado, 'DISPONIBLE'); assert.equal(c.cambiandoEstado(), false);
  assert.equal(c.estadoForm.controls.motivo.value, 'Avería'); injector.destroy();
});
test('No duplica solicitudes mientras guarda', () => {
  const respuesta = new Subject(); let envios = 0;
  const { componente: c, injector } = contexto(EquipoDetalleComponent, 'ADMINISTRADOR', {
    cambiarEstado: () => { envios++; return respuesta; }
  });
  c.equipo.set(equipo); c.estadoForm.controls.motivo.setValue('Avería');
  c.cambiarEstado(); c.cambiarEstado(); assert.equal(envios, 1);
  respuesta.next({ ...equipo, estado: 'FUERA_DE_SERVICIO' }); respuesta.complete(); injector.destroy();
});
test('Una preselección no disponible no envía una reserva', () => {
  const { componente: c, injector } = contexto(ReservaFormComponent);
  c.equipos = [equipo]; c.equipoId = 2;
  let envios = 0; c.crear.subscribe(() => envios++);
  c.enviar(); assert.equal(envios, 0);
  assert.equal(c.form.controls.equipoId.errors?.noDisponible, true);
  c.equipoId = 1; c.enviar(); assert.equal(envios, 1);
  injector.destroy();
});
