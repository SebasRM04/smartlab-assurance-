import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { mensajeDeError } from '../../core/utils/errores';
import { FieldErrorComponent } from '../../shared/components/field-error.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, FieldErrorComponent],
  template: `
    <main class="auth">
      <section class="auth-card">
        <h1>SmartLab Assurance</h1>
        <p class="sub">Inicia sesión para continuar</p>

        <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
          <div class="form-group">
            <label for="email">Correo</label>
            <input id="email" type="email" formControlName="email" autocomplete="username" />
            <app-field-error [control]="form.controls.email" />
          </div>
          <div class="form-group">
            <label for="password">Contraseña</label>
            <input id="password" type="password" formControlName="password" autocomplete="current-password" />
            <app-field-error [control]="form.controls.password" />
          </div>

          @if (errorGeneral()) {
            <div class="alert alert-error">{{ errorGeneral() }}</div>
          }

          <button class="btn btn-primary block" type="submit" [disabled]="cargando()">
            {{ cargando() ? 'Ingresando…' : 'Ingresar' }}
          </button>
        </form>

        <p class="pie">¿No tienes cuenta? <a routerLink="/registro">Regístrate</a></p>
      </section>
    </main>
  `,
  styleUrl: './auth.scss',
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  cargando = signal(false);
  errorGeneral = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando.set(true);
    this.errorGeneral.set(null);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const destino = this.route.snapshot.queryParamMap.get('returnUrl');
        // Solo rutas internas: evita redirecciones a sitios externos
        this.router.navigateByUrl(destino && destino.startsWith('/') && !destino.startsWith('//') ? destino : '/dashboard');
      },
      error: (err) => {
        this.errorGeneral.set(mensajeDeError(err));
        this.cargando.set(false);
      },
    });
  }
}
