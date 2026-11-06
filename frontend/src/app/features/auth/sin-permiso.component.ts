import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-sin-permiso',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="card">
      <h1>Sin permiso</h1>
      <p>Tu rol no tiene acceso a esta sección.</p>
      <a class="btn btn-primary" routerLink="/dashboard">Volver al panel</a>
    </div>
  `,
})
export class SinPermisoComponent {}
