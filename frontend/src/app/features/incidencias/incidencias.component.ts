import { DatePipe } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Equipo, EstadoIncidencia, Incidencia } from '../../core/models/api.models';
import { PERMISOS } from '../../core/permisos';
import { AuthService } from '../../core/services/auth.service';
import { EquipoService } from '../../core/services/equipo.service';
import { IncidenciaService } from '../../core/services/incidencia.service';
import { NotificationService } from '../../core/services/notification.service';
import { EstadoBadgeComponent } from '../../shared/components/estado-badge.component';
import { paginaValida, rebanar, totalPaginasDe } from '../../core/utils/paginacion';
import { FieldErrorComponent } from '../../shared/components/field-error.component';
import { PaginadorComponent } from '../../shared/components/paginador.component';
import { BotonActualizarComponent } from '../../shared/components/boton-actualizar.component';
import { FiltroFechasComponent, RangoFecha } from '../../shared/components/filtro-fechas.component';

@Component({
  selector: 'app-incidencias',
  standalone: true,
  imports: [BotonActualizarComponent, DatePipe, ReactiveFormsModule, RouterLink, EstadoBadgeComponent, FieldErrorComponent, PaginadorComponent, FiltroFechasComponent],
  template: `
    <div class="page-header">
      <div><h1>Incidencias</h1><p>Reporta fallas o daños de un equipo</p></div>
      <app-boton-actualizar (actualizar)="actualizar()" />
    </div>

    <section class="card">
      <h3>Reportar incidencia</h3>
      <form [formGroup]="form" (ngSubmit)="registrar()" novalidate>
        <div class="form-grid">
          <div class="form-group">
            <label for="equipo">Equipo</label>
            <select id="equipo" formControlName="equipoId">
              <option [ngValue]="null">Selecciona un equipo</option>
              @for (e of equipos(); track e.id) { <option [ngValue]="e.id">{{ e.nombre }}</option> }
            </select>
            <app-field-error [control]="form.controls.equipoId" />
          </div>
          <div class="form-group">
            <label for="evidencia">Evidencia (URL, opcional)</label>
            <input id="evidencia" type="url" formControlName="evidenciaUrl" placeholder="https://..." />
            <app-field-error [control]="form.controls.evidenciaUrl" />
          </div>
        </div>
        <div class="form-group" style="margin-top: 0.9rem">
          <label for="descripcion">Descripción</label>
          <textarea id="descripcion" rows="3" formControlName="descripcion"></textarea>
          <app-field-error [control]="form.controls.descripcion" />
        </div>
        <div class="toolbar" style="margin-top: 0.9rem">
          <button class="btn btn-primary" type="submit" [disabled]="guardando()">
            {{ guardando() ? 'Enviando…' : 'Registrar incidencia' }}
          </button>
        </div>
      </form>
    </section>

    @if (puedeVer) {
      <section class="card">
        <div class="toolbar">
          <input type="search" placeholder="Buscar por equipo, usuario o descripción" style="min-width: 260px"
            [value]="busqueda()" (input)="buscar($any($event.target).value)" />
          <select [value]="estadoSel()" (change)="elegirEstado($any($event.target).value)" aria-label="Filtrar por estado">
            <option value="">Todos los estados</option>
            @for (e of estados; track e) { <option [value]="e">{{ e }}</option> }
          </select>
          <select [value]="equipoSel()" (change)="elegirEquipo($any($event.target).value)" aria-label="Filtrar por equipo">
            <option value="">Todos los equipos</option>
            @for (e of equiposOrdenados(); track e.id) { <option [value]="e.id">{{ e.nombre }}</option> }
          </select>
          <app-filtro-fechas #fechas etiqueta="Registro" (cambio)="cambiarFechas($event)" />
          @if (hayFiltros() || fechas.activo) {
            <button class="btn btn-sm" type="button" (click)="fechas.limpiar(); limpiarFiltros()">Limpiar filtros</button>
          }
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>#</th><th>Equipo</th><th>Fecha</th><th>Reportada por</th><th>Descripción</th><th>Estado</th><th></th></tr></thead>
            <tbody>
              @for (i of visibles(); track i.id) {
                <tr>
                  <td>{{ i.id }}</td>
                  <td><a [routerLink]="['/equipos', i.equipoId]">{{ nombreEquipo(i.equipoId) }}</a></td>
                  <td>{{ i.fechaRegistro | date: 'dd/MM/yyyy HH:mm' }}</td>
                  <td>{{ i.usuarioNombre }}</td>
                  <td>
                    {{ i.descripcion }}
                    @if (i.evidenciaUrl) { <br /><a [href]="i.evidenciaUrl" target="_blank" rel="noopener">Ver evidencia</a> }
                  </td>
                  <td><app-estado-badge [estado]="i.estado" /></td>
                  <td class="acciones">
                    @if (puedeGestionar && i.estado === 'ABIERTA') {
                      <button class="btn btn-sm" type="button" (click)="cambiar(i, 'EN_REVISION')">En revisión</button>
                    }
                    @if (puedeGestionar && i.estado !== 'RESUELTA') {
                      <button class="btn btn-sm btn-ok" type="button" (click)="cambiar(i, 'RESUELTA')">Resolver</button>
                    }
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="7" class="vacio">{{ hayFiltros() ? 'No hay incidencias que coincidan con los filtros' : 'No hay incidencias registradas' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
        <app-paginador
          [pagina]="paginaActual()"
          [totalPaginas]="totalPaginas()"
          [totalElementos]="filtradas().length"
          [tamanio]="tamanioPagina"
          (cambio)="pagina.set($event)"
        />
      </section>
    }
  `,
})
export class IncidenciasComponent implements OnInit {
  @Input() equipoId?: string; // query param ?equipoId=

  private fb = inject(FormBuilder);
  private incidenciasApi = inject(IncidenciaService);
  private equiposApi = inject(EquipoService);
  private avisos = inject(NotificationService);
  private auth = inject(AuthService);

  readonly puedeVer = this.auth.tieneRol(PERMISOS.incidenciaVer);
  readonly puedeGestionar = this.auth.tieneRol(PERMISOS.incidenciaGestionar);

  readonly tamanioPagina = 15;
  incidencias = signal<Incidencia[]>([]);
  pagina = signal(0);
  equipos = signal<Equipo[]>([]);

  // Filtros del navegador: se aplican sobre la lista ya cargada y se recalculan solos al recargarla
  readonly estados: EstadoIncidencia[] = ['ABIERTA', 'EN_REVISION', 'RESUELTA'];
  busqueda = signal('');
  estadoSel = signal('');
  equipoSel = signal('');
  equiposOrdenados = computed(() => [...this.equipos()].sort((a, b) => a.nombre.localeCompare(b.nombre)));
  desde = signal('');
  hasta = signal('');
  hayFiltros = computed(() => this.busqueda().trim() !== '' || this.estadoSel() !== '' || this.equipoSel() !== '' || this.desde() !== '' || this.hasta() !== '');
  filtradas = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const estado = this.estadoSel();
    const equipo = this.equipoSel();
    const desde = this.desde();
    const hasta = this.hasta();
    if (desde && hasta && desde > hasta) return [];
    return this.incidencias().filter((i) => {
      const dia = diaLocal(i.fechaRegistro);
      return (
        (!estado || i.estado === estado) &&
        (!equipo || i.equipoId === +equipo) &&
        (!desde || dia >= desde) &&
        (!hasta || dia <= hasta) &&
        (!texto ||
          `${this.nombreEquipo(i.equipoId)} ${i.usuarioNombre} ${i.descripcion}`.toLowerCase().includes(texto))
      );
    });
  });
  totalPaginas = computed(() => totalPaginasDe(this.filtradas().length, this.tamanioPagina));
  paginaActual = computed(() => paginaValida(this.pagina(), this.filtradas().length, this.tamanioPagina));
  visibles = computed(() => rebanar(this.filtradas(), this.pagina(), this.tamanioPagina));
  guardando = signal(false);

  form = this.fb.group({
    equipoId: [null as number | null, [Validators.required]],
    descripcion: ['', [Validators.required, Validators.minLength(10)]],
    evidenciaUrl: ['', [Validators.pattern(/^(https?:\/\/\S+)?$/)]],
  });

  ngOnInit(): void {
    if (this.equipoId) this.form.patchValue({ equipoId: +this.equipoId });
    this.actualizar();
  }

  buscar(texto: string): void {
    this.busqueda.set(texto);
    this.pagina.set(0);
  }

  elegirEstado(valor: string): void {
    this.estadoSel.set(valor);
    this.pagina.set(0);
  }

  elegirEquipo(valor: string): void {
    this.equipoSel.set(valor);
    this.pagina.set(0);
  }

  limpiarFiltros(): void {
    this.busqueda.set('');
    this.estadoSel.set('');
    this.equipoSel.set('');
    this.desde.set('');
    this.hasta.set('');
    this.pagina.set(0);
  }

  cambiarFechas(rango: RangoFecha): void {
    this.desde.set(rango.desde);
    this.hasta.set(rango.hasta);
    this.pagina.set(0);
  }

  actualizar(): void {
    this.equiposApi.listar().subscribe({ next: (l) => this.equipos.set(l), error: (e) => this.avisos.error(e) });
    if (this.puedeVer) this.cargar();
  }

  nombreEquipo(id: number): string {
    return this.equipos().find((e) => e.id === id)?.nombre ?? `Equipo #${id}`;
  }

  registrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.incidenciasApi
      .registrar({ equipoId: v.equipoId as number, descripcion: v.descripcion ?? '', evidenciaUrl: v.evidenciaUrl || null })
      .subscribe({
        next: () => {
          this.avisos.exito('Incidencia registrada. Gracias por el reporte.');
          this.guardando.set(false);
          this.form.reset({ equipoId: null, descripcion: '', evidenciaUrl: '' });
          if (this.puedeVer) this.cargar();
        },
        error: (e) => {
          this.avisos.error(e);
          this.guardando.set(false);
        },
      });
  }

  cambiar(i: Incidencia, estado: EstadoIncidencia): void {
    this.incidenciasApi.cambiarEstado(i.id, estado).subscribe({
      next: () => {
        this.avisos.exito(`Incidencia #${i.id} actualizada`);
        this.cargar();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  private cargar(): void {
    this.incidenciasApi.listar().subscribe({ next: (l) => this.incidencias.set(l), error: (e) => this.avisos.error(e) });
  }
}

function diaLocal(iso: string): string {
  const d = new Date(iso);
  const z = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;
}
