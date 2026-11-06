import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, WritableSignal, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import {
  ConflictoMetricas, DisponibilidadMetricas, EquipoRecomendacion, MantenimientoMetricas, PrestamoMetricas,
  ReservaMetricas, UtilizacionDashboard,
} from '../../core/models/api.models';
import { PERMISOS } from '../../core/permisos';
import { AuthService } from '../../core/services/auth.service';
import { EquipoService } from '../../core/services/equipo.service';
import { NotificationService } from '../../core/services/notification.service';
import { ReporteService } from '../../core/services/reporte.service';
import { paginaValida, rebanar, totalPaginasDe } from '../../core/utils/paginacion';
import { EstadoBadgeComponent } from '../../shared/components/estado-badge.component';
import { PaginadorComponent } from '../../shared/components/paginador.component';
import { StatCardComponent } from '../../shared/components/stat-card.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink, StatCardComponent, EstadoBadgeComponent, PaginadorComponent,
  ],
  template: `
    <div class="page-header">
      <div>
        <h1>Hola, {{ auth.nombre() }}</h1>
        <p>Panel de indicadores de SmartLab Assurance</p>
      </div>
      @if (verUtilizacion || verRecomendaciones) {
        <div class="toolbar">
          <label for="umbral">Umbral de horas</label>
          <input id="umbral" type="number" min="1" style="width: 90px" [formControl]="umbral" />
          <button class="btn btn-primary btn-sm" type="button" (click)="actualizarUmbrales()">Actualizar</button>
        </div>
      }
    </div>

    @if (!verGestion && !verUtilizacion) {
      <div class="card">
        <h3>¿Qué quieres hacer?</h3>
        <div class="toolbar">
          <a class="btn btn-primary" routerLink="/reservas">Reservar un equipo</a>
          <a class="btn" routerLink="/equipos">Ver equipos</a>
          <a class="btn" routerLink="/incidencias">Reportar una incidencia</a>
        </div>
      </div>
    }

    <div class="grid-stats">
      @if (noShow(); as m) {
        <app-stat-card titulo="Tasa de inasistencia" [valor]="(m.tasaNoShow | number: '1.0-2') + ' %'"
          [detalle]="m.noShow + ' no-show de ' + m.totalConfirmadas + ' confirmadas'" tono="alerta" />
      }
      @if (conflictos(); as m) {
        <app-stat-card titulo="Tasa de conflicto de reserva" [valor]="(m.tasaConflicto | number: '1.0-2') + ' %'"
          [detalle]="m.rechazadasPorConflicto + ' rechazadas de ' + m.totalSolicitudes + ' solicitudes'" tono="alerta" />
      }
      @if (mora(); as m) {
        <app-stat-card titulo="Tasa de mora en devolución" [valor]="(m.tasaMora | number: '1.0-2') + ' %'"
          [detalle]="m.conMora + ' tardías de ' + m.devueltos + ' devueltos'" tono="alerta" />
        <app-stat-card titulo="Préstamos vencidos" [valor]="m.vencidos" detalle="Activos con la fecha límite pasada"
          [tono]="m.vencidos > 0 ? 'peligro' : 'ok'" />
      }
      @if (mantenimiento(); as m) {
        <app-stat-card titulo="Tasa de mantenimiento tardío" [valor]="(m.tasaTardio | number: '1.0-2') + ' %'"
          [detalle]="m.tardios + ' tardíos de ' + m.cerrados + ' cerrados'" tono="alerta" />
      }
      @if (disponibilidad(); as m) {
        <app-stat-card titulo="Exactitud de disponibilidad" [valor]="(m.exactitudDisponibilidad | number: '1.0-2') + ' %'"
          [detalle]="m.equiposCorrectamenteBloqueados + ' de ' + m.totalEquiposEnMantenimiento + ' bloqueados correctamente'"
          [tono]="m.exactitudDisponibilidad >= 100 ? 'ok' : 'peligro'" />
      }
    </div>

    @if (utilizacion(); as u) {
      <section class="card">
        <h3>Utilización de equipos</h3>
        <p>
          <app-estado-badge estado="SUBUTILIZADO" /> {{ u.subutilizados }} ·
          <app-estado-badge estado="NORMAL" /> {{ u.normales }} ·
          <app-estado-badge estado="SOBREUTILIZADO" /> {{ u.sobreutilizados }}
          (sobreutilizado desde {{ u.umbralSobreutilizacionHoras }} h) · ordenados por horas de uso
        </p>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Equipo</th><th>Horas de uso</th><th>Préstamos</th><th>Clasificación</th></tr></thead>
            <tbody>
              @for (e of equiposVisibles(); track e.equipoId) {
                <tr>
                  <td><a [routerLink]="['/equipos', e.equipoId]">{{ e.nombre }}</a></td>
                  <td>{{ e.horasUso }}</td>
                  <td>{{ e.frecuenciaPrestamos }}</td>
                  <td><app-estado-badge [estado]="e.clasificacion" /></td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="vacio">No hay equipos registrados</td></tr>
              }
            </tbody>
          </table>
        </div>
        <app-paginador
          [pagina]="paginaUtilizacionActual()"
          [totalPaginas]="totalPaginasUtilizacion()"
          [totalElementos]="equiposOrdenados().length"
          [tamanio]="tamanioPagina"
          (cambio)="paginaUtilizacion.set($event)"
        />
      </section>
    }

    @if (verRecomendaciones) {
      <section class="card">
        <h3>Equipos que requieren mantenimiento preventivo</h3>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Equipo</th><th>Horas de uso</th><th>Fecha programada</th><th>Motivo</th></tr></thead>
            <tbody>
              @for (r of recomendaciones(); track r.equipoId) {
                <tr>
                  <td><a [routerLink]="['/equipos', r.equipoId]">{{ r.nombre }}</a></td>
                  <td>{{ r.contadorUsoHoras }}</td>
                  <td>{{ r.fechaProgramada ? (r.fechaProgramada | date: 'dd/MM/yyyy HH:mm') : '—' }}</td>
                  <td>{{ r.motivo }}</td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="vacio">Ningún equipo necesita atención preventiva por ahora</td></tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }
  `,
})
export class DashboardComponent implements OnInit {
  auth = inject(AuthService);
  private reportes = inject(ReporteService);
  private equipos = inject(EquipoService);
  private avisos = inject(NotificationService);

  readonly verGestion = this.auth.tieneRol(PERMISOS.reportesGestion);
  readonly verUtilizacion = this.auth.tieneRol(PERMISOS.reportesOperativos);
  readonly verRecomendaciones = this.auth.tieneRol(PERMISOS.recomendaciones);

  umbral = new FormControl(100, { nonNullable: true, validators: [Validators.required, Validators.min(1)] });

  noShow = signal<ReservaMetricas | null>(null);
  conflictos = signal<ConflictoMetricas | null>(null);
  mora = signal<PrestamoMetricas | null>(null);
  mantenimiento = signal<MantenimientoMetricas | null>(null);
  disponibilidad = signal<DisponibilidadMetricas | null>(null);
  utilizacion = signal<UtilizacionDashboard | null>(null);
  recomendaciones = signal<EquipoRecomendacion[]>([]);

  // La utilización llega con todos los equipos (180): se ordena por horas y se muestra por páginas
  readonly tamanioPagina = 15;
  paginaUtilizacion = signal(0);
  equiposOrdenados = computed(() =>
    [...(this.utilizacion()?.equipos ?? [])].sort((a, b) => b.horasUso - a.horasUso),
  );
  totalPaginasUtilizacion = computed(() => totalPaginasDe(this.equiposOrdenados().length, this.tamanioPagina));
  paginaUtilizacionActual = computed(() =>
    paginaValida(this.paginaUtilizacion(), this.equiposOrdenados().length, this.tamanioPagina),
  );
  equiposVisibles = computed(() => rebanar(this.equiposOrdenados(), this.paginaUtilizacion(), this.tamanioPagina));

  ngOnInit(): void {
    if (this.verGestion) {
      this.cargar(this.reportes.noShow(), this.noShow);
      this.cargar(this.reportes.conflictos(), this.conflictos);
      this.cargar(this.reportes.mora(), this.mora);
      this.cargar(this.reportes.exactitudDisponibilidad(), this.disponibilidad);
    }
    if (this.verUtilizacion) {
      this.cargar(this.reportes.mantenimientoTardio(), this.mantenimiento);
    }
    this.actualizarUmbrales();
  }

  actualizarUmbrales(): void {
    if (this.umbral.invalid) {
      this.umbral.markAsTouched();
      return;
    }
    if (this.verUtilizacion) {
      this.cargar(this.equipos.dashboardUtilizacion(this.umbral.value), this.utilizacion);
    }
    if (this.verRecomendaciones) {
      this.equipos.recomendaciones(this.umbral.value).subscribe({
        next: (lista) => this.recomendaciones.set(lista),
        error: (e) => this.avisos.error(e),
      });
    }
  }

  private cargar<T>(origen: Observable<T>, destino: WritableSignal<T | null>): void {
    origen.subscribe({
      next: (valor) => destino.set(valor),
      error: (e) => this.avisos.error(e),
    });
  }
}
