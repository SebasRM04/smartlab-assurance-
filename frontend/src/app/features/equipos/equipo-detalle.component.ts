import { DatePipe } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  EstadoEquipo, Equipo, HistorialEstado, Incidencia, UtilizacionEquipo,
} from '../../core/models/api.models';
import { PERMISOS } from '../../core/permisos';
import { AuthService } from '../../core/services/auth.service';
import { EquipoService } from '../../core/services/equipo.service';
import { IncidenciaService } from '../../core/services/incidencia.service';
import { NotificationService } from '../../core/services/notification.service';
import { EstadoBadgeComponent } from '../../shared/components/estado-badge.component';

type Pestana = 'historial' | 'utilizacion' | 'incidencias';

@Component({
  selector: 'app-equipo-detalle',
  standalone: true,
  imports: [DatePipe, RouterLink, EstadoBadgeComponent],
  template: `
    @if (equipo(); as e) {
      <div class="page-header">
        <div>
          <h1>{{ e.nombre }}</h1>
          <p>{{ e.categoriaNombre }} · {{ e.ubicacion || 'Sin ubicación' }}</p>
        </div>
        <div>
          <div class="toolbar">
            @if (puedeReservar && disponibilidad() !== 'EN_MANTENIMIENTO') {
              <a class="btn btn-primary" routerLink="/reservas" [queryParams]="{ equipoId: e.id }">Reservar</a>
            }
            @if (puedeMantener) {
              <a class="btn" routerLink="/mantenimientos" [queryParams]="{ equipoId: e.id }">Programar mantenimiento</a>
            }
            <a class="btn" routerLink="/incidencias" [queryParams]="{ equipoId: e.id }">Reportar incidencia</a>
            @if (puedeEditar) {
              <a class="btn" [routerLink]="['/equipos', e.id, 'editar']">Editar</a>
            }
            @if (puedeEliminar) {
              <button class="btn btn-danger" type="button" (click)="eliminar(e)">Eliminar</button>
            }
          </div>
          <div style="text-align: right; margin-top: 0.5rem">
              <a routerLink="/equipos" style="font-size: 0.9rem">← Volver a equipos</a>
            </div>
        </div>
      </div>

      <div class="card">
        <dl class="detalle-grid">
          <div><dt>Disponibilidad actual</dt><dd>@if (disponibilidad(); as d) { <app-estado-badge [estado]="d" /> }</dd></div>
          <div><dt>Horas de uso acumuladas</dt><dd>{{ e.contadorUsoHoras }} h</dd></div>
          <div><dt>Identificador</dt><dd>#{{ e.id }}</dd></div>
        </dl>
      </div>

      @if (puedeVerHistorial) {
        <div class="card">
          <div class="tabs">
            <button type="button" [class.activa]="pestana() === 'historial'" (click)="cambiar('historial')">Historial de estados</button>
            <button type="button" [class.activa]="pestana() === 'utilizacion'" (click)="cambiar('utilizacion')">Historial de utilización</button>
            <button type="button" [class.activa]="pestana() === 'incidencias'" (click)="cambiar('incidencias')">Incidencias</button>
          </div>

          @switch (pestana()) {
            @case ('historial') {
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>Fecha y hora</th><th>Estado anterior</th><th>Estado nuevo</th><th>Responsable</th><th>Motivo</th></tr></thead>
                  <tbody>
                    @for (h of historial(); track $index) {
                      <tr>
                        <td>{{ h.fechaCambio | date: 'dd/MM/yyyy HH:mm' }}</td>
                        <td>@if (h.estadoAnterior) { <app-estado-badge [estado]="h.estadoAnterior" /> } @else { — }</td>
                        <td><app-estado-badge [estado]="h.estadoNuevo" /></td>
                        <td>{{ h.usuarioNombre || '—' }}</td>
                        <td>{{ h.motivo || '—' }}</td>
                      </tr>
                    } @empty {
                      <tr><td colspan="5" class="vacio">Sin cambios registrados</td></tr>
                    }
                  </tbody>
                </table>
              </div>
            }
            @case ('utilizacion') {
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>Préstamo</th><th>Usuario</th><th>Entrega</th><th>Límite</th><th>Devolución</th><th>Horas</th><th>Estado</th></tr></thead>
                  <tbody>
                    @for (u of utilizacion(); track u.prestamoId) {
                      <tr>
                        <td>#{{ u.prestamoId }}</td>
                        <td>{{ u.usuarioNombre }}</td>
                        <td>{{ u.fechaEntrega | date: 'dd/MM/yyyy HH:mm' }}</td>
                        <td>{{ u.fechaLimite | date: 'dd/MM/yyyy HH:mm' }}</td>
                        <td>{{ u.fechaDevolucionReal ? (u.fechaDevolucionReal | date: 'dd/MM/yyyy HH:mm') : '—' }}</td>
                        <td>{{ u.horasUso ?? '—' }}</td>
                        <td><app-estado-badge [estado]="u.estado" /></td>
                      </tr>
                    } @empty {
                      <tr><td colspan="7" class="vacio">Este equipo aún no tiene préstamos</td></tr>
                    }
                  </tbody>
                </table>
              </div>
            }
            @case ('incidencias') {
              <div class="table-wrap">
                <table class="table">
                  <thead><tr><th>Fecha</th><th>Reportada por</th><th>Descripción</th><th>Estado</th></tr></thead>
                  <tbody>
                    @for (i of incidencias(); track i.id) {
                      <tr>
                        <td>{{ i.fechaRegistro | date: 'dd/MM/yyyy HH:mm' }}</td>
                        <td>{{ i.usuarioNombre }}</td>
                        <td>{{ i.descripcion }}</td>
                        <td><app-estado-badge [estado]="i.estado" /></td>
                      </tr>
                    } @empty {
                      <tr><td colspan="4" class="vacio">Sin incidencias reportadas</td></tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          }
        </div>
      }
    } @else {
      <div class="card vacio">Cargando equipo…</div>
    }
  `,
})
export class EquipoDetalleComponent implements OnInit {
  @Input() id!: string;

  private equipos = inject(EquipoService);
  private incidenciasApi = inject(IncidenciaService);
  private avisos = inject(NotificationService);
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly puedeReservar = this.auth.tieneRol(PERMISOS.reservaCrear);
  readonly puedeMantener = this.auth.tieneRol(PERMISOS.mantenimientoOperar);
  readonly puedeEditar = this.auth.tieneRol(PERMISOS.equipoEscribir);
  readonly puedeEliminar = this.auth.tieneRol(PERMISOS.equipoEliminar);
  readonly puedeVerHistorial = this.auth.tieneRol(PERMISOS.equipoHistorial);

  equipo = signal<Equipo | null>(null);
  disponibilidad = signal<EstadoEquipo | null>(null);
  historial = signal<HistorialEstado[]>([]);
  utilizacion = signal<UtilizacionEquipo[]>([]);
  incidencias = signal<Incidencia[]>([]);
  pestana = signal<Pestana>('historial');

  ngOnInit(): void {
    const id = +this.id;
    this.equipos.obtener(id).subscribe({
      next: (e) => this.equipo.set(e),
      error: (e) => this.avisos.error(e),
    });
    this.equipos.disponibilidad(id).subscribe({
      next: (r) => this.disponibilidad.set(r.estado),
      error: (e) => this.avisos.error(e),
    });
    if (this.puedeVerHistorial) {
      this.cambiar('historial');
    }
  }

  cambiar(p: Pestana): void {
    this.pestana.set(p);
    const id = +this.id;
    if (p === 'historial') {
      this.equipos.historial(id).subscribe({ next: (l) => this.historial.set(l), error: (e) => this.avisos.error(e) });
    } else if (p === 'utilizacion') {
      this.equipos.utilizacion(id).subscribe({ next: (l) => this.utilizacion.set(l), error: (e) => this.avisos.error(e) });
    } else {
      this.incidenciasApi.listarPorEquipo(id).subscribe({ next: (l) => this.incidencias.set(l), error: (e) => this.avisos.error(e) });
    }
  }

  eliminar(e: Equipo): void {
    if (!confirm(`¿Eliminar el equipo "${e.nombre}"? Esta acción no se puede deshacer.`)) return;
    this.equipos.eliminar(e.id).subscribe({
      next: () => {
        this.avisos.exito('Equipo eliminado');
        this.router.navigate(['/equipos']);
      },
      error: (err) => this.avisos.error(err),
    });
  }
}
