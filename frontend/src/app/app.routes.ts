import { Routes } from '@angular/router';
import { PERMISOS } from './core/permisos';
import { authGuard, invitadoGuard, rolGuard } from './core/guards/auth.guard';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'registro',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./features/auth/registro.component').then((m) => m.RegistroComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'equipos',
        loadComponent: () => import('./features/equipos/equipos-list.component').then((m) => m.EquiposListComponent),
      },
      {
        path: 'equipos/nuevo',
        canActivate: [rolGuard],
        data: { roles: PERMISOS.equipoEscribir },
        loadComponent: () => import('./features/equipos/equipo-form-page.component').then((m) => m.EquipoFormPageComponent),
      },
      {
        path: 'equipos/:id/editar',
        canActivate: [rolGuard],
        data: { roles: PERMISOS.equipoEscribir },
        loadComponent: () => import('./features/equipos/equipo-form-page.component').then((m) => m.EquipoFormPageComponent),
      },
      {
        path: 'equipos/:id',
        loadComponent: () => import('./features/equipos/equipo-detalle.component').then((m) => m.EquipoDetalleComponent),
      },
      {
        path: 'reservas',
        loadComponent: () => import('./features/reservas/reservas.component').then((m) => m.ReservasComponent),
      },
      {
        path: 'prestamos',
        canActivate: [rolGuard],
        data: { roles: PERMISOS.prestamoVer },
        loadComponent: () => import('./features/prestamos/prestamos.component').then((m) => m.PrestamosComponent),
      },
      {
        path: 'mantenimientos',
        canActivate: [rolGuard],
        data: { roles: PERMISOS.mantenimientoVer },
        loadComponent: () => import('./features/mantenimientos/mantenimientos.component').then((m) => m.MantenimientosComponent),
      },
      {
        path: 'incidencias',
        loadComponent: () => import('./features/incidencias/incidencias.component').then((m) => m.IncidenciasComponent),
      },
      {
        path: 'admin/usuarios',
        canActivate: [rolGuard],
        data: { roles: PERMISOS.admin },
        loadComponent: () => import('./features/admin/usuarios.component').then((m) => m.UsuariosComponent),
      },
      {
        path: 'admin/categorias',
        canActivate: [rolGuard],
        data: { roles: PERMISOS.admin },
        loadComponent: () => import('./features/admin/categorias.component').then((m) => m.CategoriasComponent),
      },
      {
        path: 'admin/auditoria',
        canActivate: [rolGuard],
        data: { roles: PERMISOS.admin },
        loadComponent: () => import('./features/admin/auditoria.component').then((m) => m.AuditoriaComponent),
      },
      {
        path: 'sin-permiso',
        loadComponent: () => import('./features/auth/sin-permiso.component').then((m) => m.SinPermisoComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
