import { Component, EventEmitter, Input, Output } from '@angular/core';

export interface RangoFecha {
  desde: string;
  hasta: string;
}

/** Rango Desde/Hasta con atajos. No dispara consulta si el rango está invertido. */
@Component({
  selector: 'app-filtro-fechas',
  standalone: true,
  template: `
    <span class="filtro-fechas">
      <span class="ff-etiq">{{ etiqueta }}</span>
      <label class="ff-campo">Desde
        <input type="date" [value]="desde" (change)="fijarDesde($any($event.target).value)" [attr.aria-label]="'Desde ' + etiqueta" />
      </label>
      <label class="ff-campo">Hasta
        <input type="date" [value]="hasta" (change)="fijarHasta($any($event.target).value)" [attr.aria-label]="'Hasta ' + etiqueta" />
      </label>
      <span class="ff-atajos">
        <button class="btn btn-sm" type="button" (click)="hoy()">Hoy</button>
        <button class="btn btn-sm" type="button" (click)="ultimos7()">7 días</button>
        <button class="btn btn-sm" type="button" (click)="esteMes()">Este mes</button>
      </span>
      @if (invalido) {
        <span class="ff-error" role="alert">Desde no puede ser posterior a Hasta</span>
      }
    </span>
  `,
  styles: [`
    :host { display: contents; }
    .filtro-fechas {
      display: inline-flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.35rem 0.5rem;
      padding: 0.15rem 0.45rem;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      background: #f8fafc;
    }
    .ff-etiq {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.03em;
      text-transform: uppercase;
      color: #64748b;
    }
    .ff-campo {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      margin: 0;
      font-size: 0.78rem;
      font-weight: 600;
      color: #64748b;
    }
    .ff-campo input { width: 9.1rem; padding: 0.32rem 0.4rem; }
    .ff-atajos { display: inline-flex; flex-wrap: wrap; gap: 0.25rem; }
    .ff-error { flex: 1 0 100%; color: #b91c1c; font-size: 0.78rem; }
  `],
})
export class FiltroFechasComponent {
  @Input() etiqueta = 'Fecha';
  @Output() cambio = new EventEmitter<RangoFecha>();

  desde = '';
  hasta = '';

  get invalido(): boolean {
    return this.desde !== '' && this.hasta !== '' && this.desde > this.hasta;
  }

  get activo(): boolean {
    return this.desde !== '' || this.hasta !== '';
  }

  fijarDesde(valor: string): void {
    this.desde = valor;
    this.emitir();
  }

  fijarHasta(valor: string): void {
    this.hasta = valor;
    this.emitir();
  }

  hoy(): void {
    const h = iso(new Date());
    this.desde = h;
    this.hasta = h;
    this.emitir();
  }

  ultimos7(): void {
    const h = new Date();
    const d = new Date();
    d.setDate(d.getDate() - 6);
    this.desde = iso(d);
    this.hasta = iso(h);
    this.emitir();
  }

  esteMes(): void {
    const h = new Date();
    this.desde = iso(new Date(h.getFullYear(), h.getMonth(), 1));
    this.hasta = iso(new Date(h.getFullYear(), h.getMonth() + 1, 0));
    this.emitir();
  }

  limpiar(): void {
    this.desde = '';
    this.hasta = '';
    this.emitir();
  }

  private emitir(): void {
    this.cambio.emit({ desde: this.desde, hasta: this.hasta });
  }
}

function iso(d: Date): string {
  const z = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;
}
