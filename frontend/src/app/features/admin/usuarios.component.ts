import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ROLES, Rol, Usuario } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { FieldErrorComponent } from '../../shared/components/field-error.component';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [ReactiveFormsModule, FieldErrorComponent],
  template: `
    <div class="page-header">
      <div><h1>Usuarios</h1><p>Alta, edición y baja de usuarios con su rol</p></div>
    </div>

    <section class="card">
      <h3>{{ editandoId() ? 'Editar usuario #' + editandoId() : 'Nuevo usuario' }}</h3>
      <form [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        <div class="form-grid">
          <div class="form-group">
            <label for="nombre">Nombre</label>
            <input id="nombre" type="text" formControlName="nombre" />
            <app-field-error [control]="form.controls.nombre" />
          </div>
          <div class="form-group">
            <label for="email">Correo</label>
            <input id="email" type="email" formControlName="email" />
            <app-field-error [control]="form.controls.email" />
          </div>
          <div class="form-group">
            <label for="password">Contraseña {{ editandoId() ? '(vacía = no cambiar)' : '' }}</label>
            <input id="password" type="password" formControlName="password" autocomplete="new-password" />
            <app-field-error [control]="form.controls.password" />
          </div>
          <div class="form-group">
            <label for="rol">Rol</label>
            <select id="rol" formControlName="rol">
              @for (r of roles; track r) { <option [value]="r">{{ r }}</option> }
            </select>
          </div>
        </div>
        <div class="toolbar" style="margin-top: 0.9rem">
          <button class="btn btn-primary" type="submit" [disabled]="guardando()">{{ editandoId() ? 'Actualizar' : 'Crear usuario' }}</button>
          @if (editandoId()) { <button class="btn" type="button" (click)="cancelarEdicion()">Cancelar edición</button> }
        </div>
      </form>
    </section>

    <section class="card">
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>#</th><th>Nombre</th><th>Correo</th><th>Rol</th><th></th></tr></thead>
          <tbody>
            @for (u of usuarios(); track u.id) {
              <tr>
                <td>{{ u.id }}</td>
                <td>{{ u.nombre }}</td>
                <td>{{ u.email }}</td>
                <td><span class="badge neutral">{{ u.rol }}</span></td>
                <td class="acciones">
                  <button class="btn btn-sm" type="button" (click)="editar(u)">Editar</button>
                  @if (u.id !== auth.sesion()?.usuarioId) {
                    <button class="btn btn-sm btn-danger" type="button" (click)="eliminar(u)">Eliminar</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="vacio">No hay usuarios</td></tr>
            }
          </tbody>
        </table>
      </div>
    </section>
  `,
})
export class UsuariosComponent implements OnInit {
  private fb = inject(FormBuilder);
  private usuariosApi = inject(UsuarioService);
  private avisos = inject(NotificationService);
  auth = inject(AuthService);

  readonly roles = ROLES;
  usuarios = signal<Usuario[]>([]);
  editandoId = signal<number | null>(null);
  guardando = signal(false);

  form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    rol: ['ESTUDIANTE' as Rol, [Validators.required]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  editar(u: Usuario): void {
    this.editandoId.set(u.id);
    // En edición la contraseña es opcional
    this.form.controls.password.setValidators([Validators.minLength(6)]);
    this.form.controls.password.updateValueAndValidity();
    this.form.reset({ nombre: u.nombre, email: u.email, password: '', rol: u.rol });
  }

  cancelarEdicion(): void {
    this.editandoId.set(null);
    this.form.controls.password.setValidators([Validators.required, Validators.minLength(6)]);
    this.form.reset({ nombre: '', email: '', password: '', rol: 'ESTUDIANTE' });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const id = this.editandoId();
    this.guardando.set(true);

    const peticion = id
      ? this.usuariosApi.actualizar(id, { nombre: v.nombre, email: v.email, rol: v.rol, ...(v.password ? { password: v.password } : {}) })
      : this.usuariosApi.crear({ nombre: v.nombre, email: v.email, password: v.password, rol: v.rol });

    peticion.subscribe({
      next: () => {
        this.avisos.exito(id ? 'Usuario actualizado' : 'Usuario creado');
        this.guardando.set(false);
        this.cancelarEdicion();
        this.cargar();
      },
      error: (e) => {
        this.avisos.error(e);
        this.guardando.set(false);
      },
    });
  }

  eliminar(u: Usuario): void {
    if (!confirm(`¿Eliminar al usuario ${u.nombre}?`)) return;
    this.usuariosApi.eliminar(u.id).subscribe({
      next: () => {
        this.avisos.exito('Usuario eliminado');
        this.cargar();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  private cargar(): void {
    this.usuariosApi.listar().subscribe({ next: (l) => this.usuarios.set(l), error: (e) => this.avisos.error(e) });
  }
}
