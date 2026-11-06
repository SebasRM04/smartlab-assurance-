import { Component, Input } from '@angular/core';

const TONOS: Record<string, string> = {
  DISPONIBLE: 'ok', EN_MANTENIMIENTO: 'warn',
  CONFIRMADA: 'info', ATENDIDA: 'ok', NO_SHOW: 'danger', CANCELADA: 'neutral',
  ACTIVO: 'info', DEVUELTO: 'ok',
  PROGRAMADO: 'info', CERRADO: 'ok',
  ABIERTA: 'danger', EN_REVISION: 'warn', RESUELTA: 'ok',
  SUBUTILIZADO: 'warn', NORMAL: 'ok', SOBREUTILIZADO: 'danger',
};

/** Insignia de color para cualquier estado del sistema. */
@Component({
  selector: 'app-estado-badge',
  standalone: true,
  template: `<span class="badge" [class]="tono">{{ texto }}</span>`,
})
export class EstadoBadgeComponent {
  @Input() estado = '';

  get tono(): string {
    return TONOS[this.estado] ?? 'neutral';
  }

  get texto(): string {
    return this.estado.replace(/_/g, ' ');
  }
}
