import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { mensajeDeError } from '../../core/utils/errores';
import { FieldErrorComponent } from '../../shared/components/field-error.component';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, FieldErrorComponent],
  template: `
    <main class="auth">
      <section class="auth-card">
        <h1>Crear cuenta</h1>
        <p class="sub">El registro público siempre crea un usuario con rol ESTUDIANTE</p>

        <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
          <div class="form-group">
            <label for="nombre">Nombre completo</label>
            <input id="nombre" type="text" formControlName="nombre" />
            <app-field-error [control]="form.controls.nombre" />
          </div>
          <div class="form-group">
            <label for="email">Correo</label>
            <input id="email" type="email" formControlName="email" autocomplete="username" />
            <app-field-error [control]="form.controls.email" />
          </div>
          <div class="form-group">
            <label for="password">Contraseña (mínimo 6 caracteres)</label>
            <input id="password" type="password" formControlName="password" autocomplete="new-password" />
            <app-field-error [control]="form.controls.password" />
          </div>

          @if (errorGeneral()) {
            <div class="alert alert-error">{{ errorGeneral() }}</div>
          }

          <button class="btn btn-primary block" type="submit" [disabled]="cargando()">
            {{ cargando() ? 'Creando…' : 'Registrarme' }}
          </button>
        </form>

        <p class="pie">¿Ya tienes cuenta? <a routerLink="/login">Inicia sesión</a></p>
      </section>
    </main>
  `,
  styleUrl: './auth.scss',
})
export class RegistroComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private avisos = inject(NotificationService);

  cargando = signal(false);
  errorGeneral = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando.set(true);
    this.errorGeneral.set(null);

    this.auth.registrar(this.form.getRawValue()).subscribe({
      next: () => {
        this.avisos.exito('Cuenta creada. Ya puedes iniciar sesión.');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.errorGeneral.set(mensajeDeError(err));
        this.cargando.set(false);
      },
    });
  }
}
