import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Equipo, ReservaRequest } from '../../core/models/api.models';
import { aIso, enHoras, fechaFutura, rangoValido } from '../../core/utils/fechas';
import { FieldErrorComponent } from '../../shared/components/field-error.component';

/** Formulario de reserva (presentacional): emite (crear) y el padre decide qué hacer. */
@Component({
  selector: 'app-reserva-form',
  standalone: true,
  imports: [ReactiveFormsModule, FieldErrorComponent],
  template: `
    <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
      <div class="form-grid">
        <div class="form-group">
          <label for="equipo">Equipo</label>
          <select id="equipo" formControlName="equipoId">
            <option [ngValue]="null">Selecciona un equipo disponible</option>
            @for (e of equipos; track e.id) { <option [ngValue]="e.id">{{ e.nombre }} — {{ e.categoriaNombre }}</option> }
          </select>
          <app-field-error [control]="form.controls.equipoId" />
        </div>
        <div class="form-group">
          <label for="inicio">Inicio</label>
          <input id="inicio" type="datetime-local" formControlName="fechaInicio" />
          <app-field-error [control]="form.controls.fechaInicio" />
        </div>
        <div class="form-group">
          <label for="fin">Fin</label>
          <input id="fin" type="datetime-local" formControlName="fechaFin" />
          <app-field-error [control]="form.controls.fechaFin" />
        </div>
      </div>
      @if (form.errors?.['rango'] && form.controls.fechaFin.touched) {
        <small class="field-error">La fecha de fin debe ser posterior a la de inicio</small>
      }
      <div class="toolbar" style="margin-top: 0.9rem">
        <button class="btn btn-primary" type="submit" [disabled]="guardando">
          {{ guardando ? 'Reservando…' : 'Crear reserva' }}
        </button>
      </div>
    </form>
  `,
})
export class ReservaFormComponent {
  private fb = inject(FormBuilder);

  @Input() equipos: Equipo[] = [];
  @Input() guardando = false;
  @Input() set equipoId(valor: number | null) {
    if (valor) this.form.patchValue({ equipoId: valor });
  }
  @Output() crear = new EventEmitter<ReservaRequest>();

  form = this.fb.group(
    {
      equipoId: [null as number | null, [Validators.required]],
      fechaInicio: [enHoras(24), [Validators.required, fechaFutura]],
      fechaFin: [enHoras(26), [Validators.required, fechaFutura]],
    },
    { validators: [rangoValido] },
  );

  /** El padre lo llama (vía ViewChild) cuando la reserva se creó bien. */
  reiniciar(): void {
    this.form.reset({ equipoId: null, fechaInicio: enHoras(24), fechaFin: enHoras(26) });
  }

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.crear.emit({ equipoId: v.equipoId as number, fechaInicio: aIso(v.fechaInicio ?? ''), fechaFin: aIso(v.fechaFin ?? '') });
  }
}
