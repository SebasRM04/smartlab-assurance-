import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { PERMISOS } from '../core/permisos';
import { Rol } from '../core/models/api.models';
import { AuthService } from '../core/services/auth.service';

interface ItemMenu {
  etiqueta: string;
  ruta: string;
  roles?: readonly Rol[];
}

// Cada opción solo aparece si el rol tiene permiso en el backend
const MENU: ItemMenu[] = [
  { etiqueta: 'Panel', ruta: '/dashboard' },
  { etiqueta: 'Equipos', ruta: '/equipos' },
  { etiqueta: 'Reservas', ruta: '/reservas' },
  { etiqueta: 'Préstamos', ruta: '/prestamos', roles: PERMISOS.prestamoVer },
  { etiqueta: 'Mantenimientos', ruta: '/mantenimientos', roles: PERMISOS.mantenimientoVer },
  { etiqueta: 'Incidencias', ruta: '/incidencias' },
  { etiqueta: 'Usuarios', ruta: '/admin/usuarios', roles: PERMISOS.admin },
  { etiqueta: 'Categorías', ruta: '/admin/categorias', roles: PERMISOS.admin },
  { etiqueta: 'Auditoría', ruta: '/admin/auditoria', roles: PERMISOS.admin },
];

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="layout">
      <aside class="sidebar">
        <a class="marca" routerLink="/dashboard">SmartLab<span>Assurance</span></a>
        <nav>
          @for (item of visibles(); track item.ruta) {
            <a [routerLink]="item.ruta" routerLinkActive="activo">{{ item.etiqueta }}</a>
          }
        </nav>
      </aside>

      <div class="contenido">
        <header class="topbar">
          <div class="usuario">
            <strong>{{ auth.nombre() }}</strong>
            <span class="rol">{{ auth.rol() }}</span>
          </div>
          <button class="btn btn-sm" type="button" (click)="auth.logout()">Cerrar sesión</button>
        </header>
        <main class="pagina">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  auth = inject(AuthService);
  visibles = computed(() => MENU.filter((i) => !i.roles || this.auth.tieneRol(i.roles)));
}
