import { Component, EventEmitter, OnDestroy, Output, signal } from '@angular/core';

/**
 * Botón "Actualizar" reutilizable. No carga datos por sí mismo: avisa al padre con @Output
 * (actualizar) para que vuelva a consultar la API. Queda desactivado un instante tras cada
 * clic para evitar ráfagas de peticiones.
 */
@Component({
  selector: 'app-boton-actualizar',
  standalone: true,
  styleUrl: './boton-actualizar.component.scss',
  template: `
    <button
      class="btn btn-sm"
      type="button"
      [disabled]="ocupado()"
      title="Volver a consultar los datos"
      aria-label="Actualizar datos"
      (click)="pulsar()"
    >
      <span class="icono" [class.girando]="ocupado()" aria-hidden="true">↻</span>
      {{ ocupado() ? 'Actualizando…' : 'Actualizar' }}
    </button>
  `,
})
export class BotonActualizarComponent implements OnDestroy {
  @Output() actualizar = new EventEmitter<void>();

  readonly ocupado = signal(false);
  private temporizador?: ReturnType<typeof setTimeout>;

  pulsar(): void {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.actualizar.emit();
    this.temporizador = setTimeout(() => this.ocupado.set(false), 700);
  }

  ngOnDestroy(): void {
    clearTimeout(this.temporizador);
  }
}
