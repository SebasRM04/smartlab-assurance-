import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Controles de paginación reutilizables. No carga datos: recibe la página actual por @Input
 * y avisa al padre con @Output (cambio) cuando se pide otra. La página empieza en 0.
 */
@Component({
  selector: 'app-paginador',
  standalone: true,
  template: `
    @if (totalElementos > 0) {
      <nav class="paginador" aria-label="Paginación">
        <span class="paginador-resumen">{{ desde }}–{{ hasta }} de {{ totalElementos }}</span>
        <div class="paginador-botones">
          <button class="btn btn-sm" type="button" [disabled]="pagina <= 0" (click)="ir(0)" aria-label="Primera página">«</button>
          <button class="btn btn-sm" type="button" [disabled]="pagina <= 0" (click)="ir(pagina - 1)">Anterior</button>
          <span class="paginador-pagina">Página {{ pagina + 1 }} de {{ totalPaginas }}</span>
          <button class="btn btn-sm" type="button" [disabled]="pagina >= totalPaginas - 1" (click)="ir(pagina + 1)">Siguiente</button>
          <button class="btn btn-sm" type="button" [disabled]="pagina >= totalPaginas - 1" (click)="ir(totalPaginas - 1)" aria-label="Última página">»</button>
        </div>
      </nav>
    }
  `,
})
export class PaginadorComponent {
  @Input() pagina = 0;
  @Input() totalPaginas = 0;
  @Input() totalElementos = 0;
  @Input() tamanio = 20;
  @Output() cambio = new EventEmitter<number>();

  get desde(): number {
    return this.pagina * this.tamanio + 1;
  }

  get hasta(): number {
    return Math.min((this.pagina + 1) * this.tamanio, this.totalElementos);
  }

  ir(destino: number): void {
    const limitado = Math.min(Math.max(destino, 0), Math.max(this.totalPaginas - 1, 0));
    if (limitado !== this.pagina) {
      this.cambio.emit(limitado);
    }
  }
}
