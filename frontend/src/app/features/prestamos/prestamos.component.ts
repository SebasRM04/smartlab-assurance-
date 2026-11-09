import { DatePipe } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Equipo, EstadoPrestamo, PaginaApi, Prestamo, Reserva } from '../../core/models/api.models';
import { PERMISOS } from '../../core/permisos';
import { AuthService } from '../../core/services/auth.service';
import { EquipoService } from '../../core/services/equipo.service';
import { NotificationService } from '../../core/services/notification.service';
import { PrestamoService } from '../../core/services/prestamo.service';
import { ReservaService } from '../../core/services/reserva.service';
import { aIso, enHoras, fechaFutura } from '../../core/utils/fechas';
import { EstadoBadgeComponent } from '../../shared/components/estado-badge.component';
import { FieldErrorComponent } from '../../shared/components/field-error.component';
import { PaginadorComponent } from '../../shared/components/paginador.component';
import { BotonActualizarComponent } from '../../shared/components/boton-actualizar.component';
import { FiltroFechasComponent, RangoFecha } from '../../shared/components/filtro-fechas.component';

@Component({
  selector: 'app-prestamos',
  standalone: true,
  imports: [BotonActualizarComponent, DatePipe, ReactiveFormsModule, EstadoBadgeComponent, FieldErrorComponent, PaginadorComponent, FiltroFechasComponent],
  template: `
    <div class="page-header">
      <div><h1>Préstamos</h1><p>Entrega y devolución de equipos</p></div>
      <app-boton-actualizar (actualizar)="actualizar()" />
    </div>

    @if (puedeOperar) {
      <section class="card">
        <h3>Registrar entrega (check-out)</h3>
        <form [formGroup]="form" (ngSubmit)="checkout()" novalidate>
          <div class="form-grid">
            <div class="form-group">
              <label for="reserva">Reserva confirmada</label>
              <select id="reserva" formControlName="reservaId">
                <option [ngValue]="null">Selecciona una reserva</option>
                @for (r of pendientes(); track r.id) {
                  <option [ngValue]="r.id">
                    #{{ r.id }} · {{ nombreEquipo(r.equipoId) }} · {{ r.usuarioNombre }} · {{ r.fechaInicio | date: 'dd/MM HH:mm' }}
                  </option>
                }
              </select>
              <small class="ayuda">Se listan las reservas confirmadas que ya empezaron o empiezan en las próximas {{ horasPendientes }} horas.</small>
              <app-field-error [control]="form.controls.reservaId" />
            </div>
            <div class="form-group">
              <label for="limite">Fecha límite de devolución</label>
              <input id="limite" type="datetime-local" formControlName="fechaLimite" />
              <app-field-error [control]="form.controls.fechaLimite" />
            </div>
          </div>
          <div class="toolbar" style="margin-top: 0.9rem">
            <button class="btn btn-primary" type="submit" [disabled]="guardando()">
              {{ guardando() ? 'Registrando…' : 'Registrar entrega' }}
            </button>
          </div>
        </form>
      </section>
    }

    <section class="card">
      <div class="tabs">
        <button type="button" [class.activa]="vista() === 'todos'" (click)="cambiarVista('todos')">Todos</button>
        <button type="button" [class.activa]="vista() === 'vencidos'" (click)="cambiarVista('vencidos')">
          Vencidos @if (totalVencidos() > 0) { ({{ totalVencidos() }}) }
        </button>
      </div>

      @if (vista() === 'todos') {
        <div class="toolbar">
          <select #sel (change)="cambiarEstado(sel.value)">
            <option value="">Todos los estados</option>
            <option value="ACTIVO">ACTIVO</option>
            <option value="DEVUELTO">DEVUELTO</option>
          </select>
          <app-filtro-fechas #fechas etiqueta="Entrega" (cambio)="cambiarFechas($event)" />
          @if (estadoFiltro() || fechas.activo) {
            <button class="btn btn-sm" type="button" (click)="sel.value = ''; fechas.limpiar(); cambiarEstado('')">Limpiar filtros</button>
          }
        </div>
      }

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>#</th><th>Reserva</th><th>Entrega</th><th>Límite</th><th>Devolución</th><th>Estado</th><th>Alertas</th><th></th></tr>
          </thead>
          <tbody>
            @for (p of prestamos(); track p.id) {
              <tr>
                <td>{{ p.id }}</td>
                <td>#{{ p.reservaId }}</td>
                <td>{{ p.fechaEntrega | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ p.fechaLimite | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ p.fechaDevolucionReal ? (p.fechaDevolucionReal | date: 'dd/MM/yyyy HH:mm') : '—' }}</td>
                <td><app-estado-badge [estado]="p.estado" /></td>
                <td>
                  @if (p.vencido) { <span class="badge danger">Vencido</span> }
                  @if (p.moroso) { <span class="badge warn">Devuelto con mora</span> }
                </td>
                <td class="acciones">
                  @if (puedeOperar && p.estado === 'ACTIVO') {
                    <button class="btn btn-sm btn-ok" type="button" (click)="checkin(p)">Registrar devolución</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="vacio">{{ vista() === 'vencidos' ? 'No hay préstamos vencidos' : 'Aún no hay préstamos' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
      @if (vista() === 'todos') {
        <app-paginador
          [pagina]="numeroPagina()"
          [totalPaginas]="resultado()?.totalPaginas ?? 0"
          [totalElementos]="resultado()?.totalElementos ?? 0"
          [tamanio]="tamanioPagina"
          (cambio)="irAPagina($event)"
        />
      }
    </section>
  `,
})
export class PrestamosComponent implements OnInit {
  @Input() reservaId?: string; // query param ?reservaId= (desde Reservas > Iniciar préstamo)

  private fb = inject(FormBuilder);
  private prestamosApi = inject(PrestamoService);
  private reservasApi = inject(ReservaService);
  private equiposApi = inject(EquipoService);
  private avisos = inject(NotificationService);
  private auth = inject(AuthService);

  readonly puedeOperar = this.auth.tieneRol(PERMISOS.prestamoOperar);
  readonly tamanioPagina = 15;
  readonly horasPendientes = 72;

  vista = signal<'todos' | 'vencidos'>('todos');
  resultado = signal<PaginaApi<Prestamo> | null>(null);
  numeroPagina = signal(0);
  estadoFiltro = signal<EstadoPrestamo | ''>('');
  desde = signal('');
  hasta = signal('');
  rangoInvalido = signal(false);
  vencidos = signal<Prestamo[]>([]);
  totalVencidos = signal(0);
  pendientes = signal<Reserva[]>([]);
  equipos = signal<Equipo[]>([]);
  guardando = signal(false);

  prestamos = computed(() => (this.vista() === 'vencidos' ? this.vencidos() : this.resultado()?.contenido ?? []));

  form = this.fb.group({
    reservaId: [null as number | null, [Validators.required]],
    fechaLimite: [enHoras(4), [Validators.required, fechaFutura]],
  });

  ngOnInit(): void {
    if (this.reservaId) this.form.patchValue({ reservaId: +this.reservaId });
    if (this.puedeOperar) this.cargarPendientes();
    this.equiposApi.listar().subscribe({ next: (l) => this.equipos.set(l), error: (e) => this.avisos.error(e) });
    this.cargarPrestamos();
  }

  /** Vuelve a consultar la cola de entregas, los vencidos y la lista activa (todos o vencidos). */
  actualizar(): void {
    if (this.puedeOperar) this.cargarPendientes();
    this.equiposApi.listar().subscribe({ next: (l) => this.equipos.set(l), error: (e) => this.avisos.error(e) });
    this.cargarPrestamos();
  }

  nombreEquipo(id: number): string {
    return this.equipos().find((e) => e.id === id)?.nombre ?? `Equipo #${id}`;
  }

  cambiarVista(v: 'todos' | 'vencidos'): void {
    this.vista.set(v);
    this.cargarPrestamos();
  }

  cambiarEstado(valor: string): void {
    this.estadoFiltro.set(valor as EstadoPrestamo | '');
    this.numeroPagina.set(0);
    this.cargarPagina();
  }

  cambiarFechas(rango: RangoFecha): void {
    this.desde.set(rango.desde);
    this.hasta.set(rango.hasta);
    this.rangoInvalido.set(!!rango.desde && !!rango.hasta && rango.desde > rango.hasta);
    this.numeroPagina.set(0);
    if (!this.rangoInvalido()) this.cargarPagina();
  }

  irAPagina(pagina: number): void {
    this.numeroPagina.set(pagina);
    this.cargarPagina();
  }

  checkout(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.prestamosApi.checkout({ reservaId: v.reservaId as number, fechaLimite: aIso(v.fechaLimite ?? '') }).subscribe({
      next: () => {
        this.avisos.exito('Entrega registrada. La reserva quedó ATENDIDA.');
        this.guardando.set(false);
        this.form.reset({ reservaId: null, fechaLimite: enHoras(4) });
        this.cargarPendientes();
        this.cargarPrestamos();
      },
      error: (e) => {
        this.avisos.error(e);
        this.guardando.set(false);
      },
    });
  }

  checkin(p: Prestamo): void {
    this.prestamosApi.checkin(p.id).subscribe({
      next: (r) => {
        this.avisos.exito(r.moroso ? 'Devolución registrada con mora' : 'Devolución registrada a tiempo');
        this.cargarPrestamos();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  private cargarPendientes(): void {
    this.reservasApi.pendientesEntrega(this.horasPendientes).subscribe({
      next: (lista) => {
        this.pendientes.set(lista);
        this.agregarPreseleccionada();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  /** La reserva que llega por ?reservaId= puede estar fuera de la ventana de horas: se agrega a la lista. */
  private agregarPreseleccionada(): void {
    const id = this.reservaId ? +this.reservaId : null;
    if (!id || this.pendientes().some((r) => r.id === id)) return;
    this.reservasApi.obtener(id).subscribe({
      next: (r) => {
        if (r.estado === 'CONFIRMADA') this.pendientes.update((lista) => [r, ...lista]);
      },
      error: (e) => this.avisos.error(e),
    });
  }

  private cargarPrestamos(): void {
    this.cargarVencidos();
    if (this.vista() === 'todos') this.cargarPagina();
  }

  private cargarVencidos(): void {
    this.prestamosApi.vencidos().subscribe({
      next: (l) => {
        this.vencidos.set(l);
        this.totalVencidos.set(l.length);
      },
      error: (e) => this.avisos.error(e),
    });
  }

  private cargarPagina(): void {
    if (this.rangoInvalido()) return;
    this.prestamosApi.pagina(this.numeroPagina(), this.tamanioPagina, this.estadoFiltro(), this.desde(), this.hasta()).subscribe({
      next: (p) => {
        if (p.contenido.length === 0 && p.pagina > 0) {
          this.numeroPagina.set(Math.max(p.totalPaginas - 1, 0));
          this.cargarPagina();
          return;
        }
        this.resultado.set(p);
      },
      error: (e) => this.avisos.error(e),
    });
  }
}
