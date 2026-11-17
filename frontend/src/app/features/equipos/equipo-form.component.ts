import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Categoria, Equipo, EquipoRequest } from '../../core/models/api.models';
import { FieldErrorComponent } from '../../shared/components/field-error.component';

/**
 * Formulario "tonto" de equipo: recibe datos por @Input y avisa al padre con @Output.
 * No llama al API; eso lo hace la página que lo contiene.
 */
@Component({
  selector: 'app-equipo-form',
  standalone: true,
  imports: [ReactiveFormsModule, FieldErrorComponent],
  template: `
    <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
      <div class="form-grid">
        <div class="form-group">
          <label for="nombre">Nombre</label>
          <input id="nombre" type="text" formControlName="nombre" />
          <app-field-error [control]="form.controls.nombre" />
        </div>
        <div class="form-group">
          <label for="categoria">Categoría</label>
          <select id="categoria" formControlName="categoriaId">
            <option [ngValue]="null">Selecciona una categoría</option>
            @for (c of categorias; track c.id) { <option [ngValue]="c.id">{{ c.nombre }}</option> }
          </select>
          <app-field-error [control]="form.controls.categoriaId" />
        </div>
        <div class="form-group">
          <label for="ubicacion">Ubicación</label>
          <input id="ubicacion" type="text" formControlName="ubicacion" />
        </div>
      </div>
      <div class="toolbar" style="margin-top: 1rem">
        <button class="btn btn-primary" type="submit" [disabled]="guardando">
          {{ guardando ? 'Guardando…' : 'Guardar' }}
        </button>
        <button class="btn" type="button" (click)="cancelar.emit()">Cancelar</button>
      </div>
    </form>
  `,
})
export class EquipoFormComponent {
  private fb = inject(FormBuilder);

  @Input() categorias: Categoria[] = [];
  @Input() guardando = false;
  @Input() set equipo(valor: Equipo | null) {
    if (valor) {
      this.form.patchValue({
        nombre: valor.nombre,
        categoriaId: valor.categoriaId,
        ubicacion: valor.ubicacion ?? '',
      });
    }
  }
  @Output() guardar = new EventEmitter<EquipoRequest>();
  @Output() cancelar = new EventEmitter<void>();

  form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    categoriaId: [null as number | null, [Validators.required]],
    ubicacion: [''],
  });

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.guardar.emit({ nombre: v.nombre ?? '', categoriaId: v.categoriaId as number, ubicacion: v.ubicacion ?? '' });
  }
}
