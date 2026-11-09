import { DatePipe } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  Equipo, Mantenimiento, PaginaApi, TIPOS_MANTENIMIENTO, TipoMantenimiento,
} from '../../core/models/api.models';
import { PERMISOS } from '../../core/permisos';
import { AuthService } from '../../core/services/auth.service';
import { EquipoService } from '../../core/services/equipo.service';
import { MantenimientoService } from '../../core/services/mantenimiento.service';
import { NotificationService } from '../../core/services/notification.service';
import { aIso, enHoras } from '../../core/utils/fechas';
import { EstadoBadgeComponent } from '../../shared/components/estado-badge.component';
import { FieldErrorComponent } from '../../shared/components/field-error.component';
import { PaginadorComponent } from '../../shared/components/paginador.component';
import { BotonActualizarComponent } from '../../shared/components/boton-actualizar.component';
import { FiltroFechasComponent, RangoFecha } from '../../shared/components/filtro-fechas.component';

@Component({
  selector: 'app-mantenimientos',
  standalone: true,
  imports: [BotonActualizarComponent, DatePipe, ReactiveFormsModule, RouterLink, EstadoBadgeComponent, FieldErrorComponent, PaginadorComponent, FiltroFechasComponent],
  template: `
    <div class="page-header">
      <div><h1>Mantenimientos</h1><p>Programar un mantenimiento bloquea el equipo hasta cerrar el último pendiente</p></div>
      <app-boton-actualizar (actualizar)="actualizar()" />
    </div>

    @if (puedeOperar) {
      <section class="card">
        <h3>Programar mantenimiento</h3>
        <form [formGroup]="form" (ngSubmit)="programar()" novalidate>
          <div class="form-grid">
            <div class="form-group">
              <label for="equipo">Equipo</label>
              <select id="equipo" formControlName="equipoId">
                <option [ngValue]="null">Selecciona un equipo</option>
                @for (e of equipos(); track e.id) { <option [ngValue]="e.id">{{ e.nombre }} ({{ e.estado }})</option> }
              </select>
              <app-field-error [control]="form.controls.equipoId" />
            </div>
            <div class="form-group">
              <label for="tipo">Tipo</label>
              <select id="tipo" formControlName="tipo">
                @for (t of tipos; track t) { <option [value]="t">{{ t }}</option> }
              </select>
            </div>
            <div class="form-group">
              <label for="fecha">Fecha programada</label>
              <input id="fecha" type="datetime-local" formControlName="fechaProgramada" />
              <app-field-error [control]="form.controls.fechaProgramada" />
            </div>
          </div>
          <div class="toolbar" style="margin-top: 0.9rem">
            <button class="btn btn-primary" type="submit" [disabled]="guardando()">
              {{ guardando() ? 'Programando…' : 'Programar' }}
            </button>
          </div>
        </form>
      </section>
    }

    <section class="card">
      <div class="toolbar">
        <select [value]="estadoFiltro()" (change)="cambiarEstado($any($event.target).value)" aria-label="Filtrar por estado">
          <option value="">Todos los estados</option>
          <option value="PROGRAMADO">PROGRAMADO</option>
          <option value="CERRADO">CERRADO</option>
        </select>
        <select [value]="tipoFiltro()" (change)="cambiarTipo($any($event.target).value)" aria-label="Filtrar por tipo">
          <option value="">Todos los tipos</option>
          @for (t of tipos; track t) { <option [value]="t">{{ t }}</option> }
        </select>
        <select [value]="equipoFiltro() ?? ''" (change)="cambiarEquipo($any($event.target).value)" aria-label="Filtrar por equipo">
          <option value="">Todos los equipos</option>
          @for (e of equiposOrdenados(); track e.id) { <option [value]="e.id">{{ e.nombre }}</option> }
        </select>
        <app-filtro-fechas #fechas etiqueta="Programada" (cambio)="cambiarFechas($event)" />
        @if (hayFiltros() || fechas.activo) {
          <button class="btn btn-sm" type="button" (click)="fechas.limpiar(); limpiarFiltros()">Limpiar filtros</button>
        }
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>#</th><th>Equipo</th><th>Tipo</th><th>Programada</th><th>Ejecutada</th><th>Responsable</th><th>Estado</th><th></th></tr>
          </thead>
          <tbody>
            @for (m of mantenimientos(); track m.id) {
              <tr>
                <td>{{ m.id }}</td>
                <td><a [routerLink]="['/equipos', m.equipoId]">{{ nombreEquipo(m.equipoId) }}</a></td>
                <td>{{ m.tipo }}</td>
                <td>{{ m.fechaProgramada | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ m.fechaEjecucion ? (m.fechaEjecucion | date: 'dd/MM/yyyy HH:mm') : '—' }}</td>
                <td>{{ m.usuarioNombre }}</td>
                <td>
                  <app-estado-badge [estado]="m.estado" />
                  @if (m.tardio) { <span class="badge warn">Tardío</span> }
                </td>
                <td class="acciones">
                  @if (puedeOperar && m.estado === 'PROGRAMADO') {
                    <button class="btn btn-sm btn-ok" type="button" (click)="cerrar(m)">Cerrar</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="vacio">{{ hayFiltros() ? 'No hay mantenimientos que coincidan con los filtros' : 'No hay mantenimientos registrados' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
      <app-paginador
        [pagina]="numeroPagina()"
        [totalPaginas]="resultado()?.totalPaginas ?? 0"
        [totalElementos]="resultado()?.totalElementos ?? 0"
        [tamanio]="tamanioPagina"
        (cambio)="irAPagina($event)"
      />
    </section>
  `,
})
export class MantenimientosComponent implements OnInit {
  @Input() equipoId?: string; // query param ?equipoId= (desde el detalle del equipo)

  private fb = inject(FormBuilder);
  private mantenimientosApi = inject(MantenimientoService);
  private equiposApi = inject(EquipoService);
  private avisos = inject(NotificationService);
  private auth = inject(AuthService);

  readonly tipos = TIPOS_MANTENIMIENTO;
  readonly tamanioPagina = 15;
  readonly puedeOperar = this.auth.tieneRol(PERMISOS.mantenimientoOperar);

  resultado = signal<PaginaApi<Mantenimiento> | null>(null);
  numeroPagina = signal(0);
  estadoFiltro = signal<'PROGRAMADO' | 'CERRADO' | ''>('');
  tipoFiltro = signal<TipoMantenimiento | ''>('');
  /** Filtro por equipo: se envía al servidor, que filtra toda la tabla y no solo la página. */
  equipoFiltro = signal<number | null>(null);
  equiposOrdenados = computed(() => [...this.equipos()].sort((a, b) => a.nombre.localeCompare(b.nombre)));
  desde = signal('');
  hasta = signal('');
  rangoInvalido = signal(false);
  hayFiltros = computed(
    () => this.estadoFiltro() !== '' || this.tipoFiltro() !== '' || this.equipoFiltro() !== null,
  );
  equipos = signal<Equipo[]>([]);
  mantenimientos = computed(() => this.resultado()?.contenido ?? []);
  guardando = signal(false);

  form = this.fb.group({
    equipoId: [null as number | null, [Validators.required]],
    tipo: ['PREVENTIVO' as TipoMantenimiento, [Validators.required]],
    fechaProgramada: [enHoras(24), [Validators.required]],
  });

  ngOnInit(): void {
    if (this.equipoId) this.form.patchValue({ equipoId: +this.equipoId });
    this.cargarEquipos();
    this.cargar();
  }

  actualizar(): void {
    this.cargarEquipos();
    this.cargar();
  }

  cambiarEstado(valor: string): void {
    this.estadoFiltro.set(valor as 'PROGRAMADO' | 'CERRADO' | '');
    this.numeroPagina.set(0);
    this.cargar();
  }

  cambiarEquipo(valor: string): void {
    this.equipoFiltro.set(valor ? +valor : null);
    this.numeroPagina.set(0);
    this.cargar();
  }

  limpiarFiltros(): void {
    this.estadoFiltro.set('');
    this.tipoFiltro.set('');
    this.equipoFiltro.set(null);
    this.numeroPagina.set(0);
    this.cargar();
  }

  cambiarFechas(rango: RangoFecha): void {
    this.desde.set(rango.desde);
    this.hasta.set(rango.hasta);
    this.rangoInvalido.set(!!rango.desde && !!rango.hasta && rango.desde > rango.hasta);
    this.numeroPagina.set(0);
    if (!this.rangoInvalido()) this.cargar();
  }

  cambiarTipo(valor: string): void {
    this.tipoFiltro.set(valor as TipoMantenimiento | '');
    this.numeroPagina.set(0);
    this.cargar();
  }

  irAPagina(pagina: number): void {
    this.numeroPagina.set(pagina);
    this.cargar();
  }

  nombreEquipo(id: number): string {
    return this.equipos().find((e) => e.id === id)?.nombre ?? `Equipo #${id}`;
  }

  programar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.mantenimientosApi
      .programar({ equipoId: v.equipoId as number, tipo: v.tipo as TipoMantenimiento, fechaProgramada: aIso(v.fechaProgramada ?? '') })
      .subscribe({
        next: () => {
          this.avisos.exito('Mantenimiento programado. El equipo quedó EN_MANTENIMIENTO.');
          this.guardando.set(false);
          this.cargar();
          this.cargarEquipos();
        },
        error: (e) => {
          this.avisos.error(e);
          this.guardando.set(false);
        },
      });
  }

  cerrar(m: Mantenimiento): void {
    this.mantenimientosApi.cerrar(m.id).subscribe({
      next: () => {
        this.avisos.exito(`Mantenimiento #${m.id} cerrado`);
        this.cargar();
        this.cargarEquipos(); // el equipo vuelve a DISPONIBLE solo si no quedan otros programados
      },
      error: (e) => this.avisos.error(e),
    });
  }

  private cargar(): void {
    if (this.rangoInvalido()) return;
    this.mantenimientosApi
      .pagina(this.numeroPagina(), this.tamanioPagina, this.estadoFiltro(), this.tipoFiltro(), this.equipoFiltro(), this.desde(), this.hasta())
      .subscribe({
        next: (p) => {
          if (p.contenido.length === 0 && p.pagina > 0) {
            this.numeroPagina.set(Math.max(p.totalPaginas - 1, 0));
            this.cargar();
            return;
          }
          this.resultado.set(p);
        },
        error: (e) => this.avisos.error(e),
      });
  }

  private cargarEquipos(): void {
    this.equiposApi.listar().subscribe({ next: (l) => this.equipos.set(l), error: (e) => this.avisos.error(e) });
  }
}
