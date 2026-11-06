import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Categoria } from '../../core/models/api.models';
import { CategoriaService } from '../../core/services/categoria.service';
import { NotificationService } from '../../core/services/notification.service';
import { FieldErrorComponent } from '../../shared/components/field-error.component';

@Component({
  selector: 'app-categorias',
  standalone: true,
  imports: [ReactiveFormsModule, FieldErrorComponent],
  template: `
    <div class="page-header">
      <div><h1>Categorías</h1><p>Clasificación de los equipos del laboratorio</p></div>
    </div>

    <section class="card">
      <h3>{{ editandoId() ? 'Editar categoría #' + editandoId() : 'Nueva categoría' }}</h3>
      <form [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        <div class="toolbar">
          <div class="form-group" style="min-width: 280px">
            <input type="text" formControlName="nombre" placeholder="Nombre de la categoría" />
            <app-field-error [control]="form.controls.nombre" />
          </div>
          <button class="btn btn-primary" type="submit">{{ editandoId() ? 'Actualizar' : 'Crear' }}</button>
          @if (editandoId()) { <button class="btn" type="button" (click)="cancelar()">Cancelar</button> }
        </div>
      </form>
    </section>

    <section class="card">
      <table class="table">
        <thead><tr><th>#</th><th>Nombre</th><th></th></tr></thead>
        <tbody>
          @for (c of categorias(); track c.id) {
            <tr>
              <td>{{ c.id }}</td>
              <td>{{ c.nombre }}</td>
              <td class="acciones">
                <button class="btn btn-sm" type="button" (click)="editar(c)">Editar</button>
                <button class="btn btn-sm btn-danger" type="button" (click)="eliminar(c)">Eliminar</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="3" class="vacio">No hay categorías</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class CategoriasComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(CategoriaService);
  private avisos = inject(NotificationService);

  categorias = signal<Categoria[]>([]);
  editandoId = signal<number | null>(null);

  form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  editar(c: Categoria): void {
    this.editandoId.set(c.id);
    this.form.reset({ nombre: c.nombre });
  }

  cancelar(): void {
    this.editandoId.set(null);
    this.form.reset({ nombre: '' });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const dto = this.form.getRawValue();
    const id = this.editandoId();
    (id ? this.api.actualizar(id, dto) : this.api.crear(dto)).subscribe({
      next: () => {
        this.avisos.exito(id ? 'Categoría actualizada' : 'Categoría creada');
        this.cancelar();
        this.cargar();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  eliminar(c: Categoria): void {
    if (!confirm(`¿Eliminar la categoría "${c.nombre}"?`)) return;
    this.api.eliminar(c.id).subscribe({
      next: () => {
        this.avisos.exito('Categoría eliminada');
        this.cargar();
      },
      error: (e) => this.avisos.error(e), // por ejemplo 409 si tiene equipos asociados
    });
  }

  private cargar(): void {
    this.api.listar().subscribe({ next: (l) => this.categorias.set(l), error: (e) => this.avisos.error(e) });
  }
}
