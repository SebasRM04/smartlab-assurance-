import { Component, Input } from '@angular/core';

/** Tarjeta de indicador del panel: título, valor grande y una línea de detalle. */
@Component({
  selector: 'app-stat-card',
  standalone: true,
  template: `
    <article class="stat" [class]="tono">
      <span class="titulo">{{ titulo }}</span>
      <strong class="valor">{{ valor }}</strong>
      @if (detalle) { <small class="detalle">{{ detalle }}</small> }
    </article>
  `,
  styleUrl: './stat-card.component.scss',
})
export class StatCardComponent {
  @Input() titulo = '';
  @Input() valor: string | number = '—';
  @Input() detalle = '';
  @Input() tono: 'neutro' | 'ok' | 'alerta' | 'peligro' = 'neutro';
}
