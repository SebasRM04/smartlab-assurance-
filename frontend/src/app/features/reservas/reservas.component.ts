import { DatePipe } from '@angular/common';
import { Component, Input, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  ESTADOS_RESERVA, Equipo, EstadoReserva, PaginaApi, Reserva, ReservaRequest, UsuarioOpcion } from '../../core/models/api.models';
import { PERMISOS } from '../../core/permisos';
import { UsuarioService } from '../../core/services/usuario.service';
import { AuthService } from '../../core/services/auth.service';
import { EquipoService } from '../../core/services/equipo.service';
import { NotificationService } from '../../core/services/notification.service';
import { ReservaService } from '../../core/services/reserva.service';
import { EstadoBadgeComponent } from '../../shared/components/estado-badge.component';
import { PaginadorComponent } from '../../shared/components/paginador.component';
import { ReservaFormComponent } from './reserva-form.component';
import { BotonActualizarComponent } from '../../shared/components/boton-actualizar.component';
import { FiltroFechasComponent, RangoFecha } from '../../shared/components/filtro-fechas.component';

@Component({
  selector: 'app-reservas',
  standalone: true,
  imports: [BotonActualizarComponent, DatePipe, RouterLink, EstadoBadgeComponent, PaginadorComponent, ReservaFormComponent, FiltroFechasComponent],
  template: `
    <div class="page-header">
      <div>
        <h1>Reservas</h1>
        <p>{{ esEstudiante ? 'Tus reservas de equipos' : 'Reservas de todos los usuarios' }}</p>
      </div>
      <app-boton-actualizar (actualizar)="actualizar()" />
    </div>

    @if (puedeCrear) {
      <section class="card">
        <h3>Nueva reserva</h3>
        <app-reserva-form
          [equipos]="disponibles()"
          [equipoId]="equipoPreseleccionado"
          [guardando]="guardando()"
          (crear)="crear($event)"
        />
      </section>
    }

    <section class="card">
      <div class="toolbar">
        <select [value]="filtro()" (change)="cambiarFiltro($any($event.target).value)" aria-label="Filtrar por estado">
          <option value="">Todos los estados</option>
          @for (e of estados; track e) { <option [value]="e">{{ e }}</option> }
        </select>
        @if (!esEstudiante) {
          <select [value]="equipoFiltro() ?? ''" (change)="cambiarEquipo($any($event.target).value)" aria-label="Filtrar por equipo">
            <option value="">Todos los equipos</option>
            @for (e of equiposOrdenados(); track e.id) { <option [value]="e.id">{{ e.nombre }}</option> }
          </select>
        }
        @if (puedeFiltrarPorUsuario) {
          <select [value]="usuarioFiltro() ?? ''" (change)="cambiarUsuario($any($event.target).value)" aria-label="Filtrar por usuario">
            <option value="">Todos los usuarios</option>
            @for (u of usuarios(); track u.id) { <option [value]="u.id">{{ u.nombre }}</option> }
          </select>
        }
        <app-filtro-fechas #fechas etiqueta="Inicio" (cambio)="cambiarFechas($event)" />
        @if (hayFiltros() || fechas.activo) {
          <button class="btn btn-sm" type="button" (click)="fechas.limpiar(); limpiarFiltros()">Limpiar filtros</button>
        }
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>#</th><th>Equipo</th><th>Usuario</th><th>Inicio</th><th>Fin</th><th>Estado</th><th></th></tr>
          </thead>
          <tbody>
            @for (r of reservas(); track r.id) {
              <tr>
                <td>{{ r.id }}</td>
                <td><a [routerLink]="['/equipos', r.equipoId]">{{ nombreEquipo(r.equipoId) }}</a></td>
                <td>{{ r.usuarioNombre }}</td>
                <td>{{ r.fechaInicio | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ r.fechaFin | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td><app-estado-badge [estado]="r.estado" /></td>
                <td class="acciones">
                  @if (r.estado === 'CONFIRMADA') {
                    <button class="btn btn-sm" type="button" (click)="cambiar(r, 'CANCELADA')">Cancelar</button>
                    @if (puedeNoShow) {
                      <button class="btn btn-sm btn-danger" type="button" [disabled]="!yaInicio(r)"
                        [title]="yaInicio(r) ? '' : 'Solo se puede marcar después de la hora de inicio'"
                        (click)="cambiar(r, 'NO_SHOW')">No-show</button>
                    }
                    @if (puedeOperarPrestamo) {
                      <a class="btn btn-sm btn-primary" routerLink="/prestamos" [queryParams]="{ reservaId: r.id }">Iniciar préstamo</a>
                    }
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="vacio">No hay reservas para mostrar</td></tr>
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
export class ReservasComponent implements OnInit {
  @Input() equipoId?: string; // query param ?equipoId= (desde el detalle del equipo)
  @ViewChild(ReservaFormComponent) formulario?: ReservaFormComponent;

  private reservasApi = inject(ReservaService);
  private equiposApi = inject(EquipoService);
  private avisos = inject(NotificationService);
  private auth = inject(AuthService);
  private usuariosApi = inject(UsuarioService);

  readonly estados = ESTADOS_RESERVA;
  readonly tamanioPagina = 15;
  readonly esEstudiante = this.auth.rol() === 'ESTUDIANTE';
  readonly puedeCrear = this.auth.tieneRol(PERMISOS.reservaCrear);
  readonly puedeNoShow = this.auth.tieneRol(PERMISOS.reservaNoShow);
  readonly puedeFiltrarPorUsuario = this.auth.tieneRol(PERMISOS.reservaFiltrarPorUsuario);
  readonly puedeOperarPrestamo = this.auth.tieneRol(PERMISOS.prestamoOperar);

  resultado = signal<PaginaApi<Reserva> | null>(null);
  numeroPagina = signal(0);
  filtro = signal<EstadoReserva | ''>('');
  /** Filtro por equipo (solo personal): se envía al servidor, que filtra toda la tabla y no solo la página. */
  equipoFiltro = signal<number | null>(null);
  equiposOrdenados = computed(() => [...this.equipos()].sort((a, b) => a.nombre.localeCompare(b.nombre)));
  /** Filtro por usuario (técnico, coordinador y administrador): también se envía al servidor. */
  usuarioFiltro = signal<number | null>(null);
  usuarios = signal<UsuarioOpcion[]>([]);
  desde = signal('');
  hasta = signal('');
  rangoInvalido = signal(false);
  hayFiltros = computed(
    () => this.filtro() !== '' || this.equipoFiltro() !== null || this.usuarioFiltro() !== null,
  );
  equipos = signal<Equipo[]>([]);
  guardando = signal(false);

  reservas = computed(() => this.resultado()?.contenido ?? []);
  disponibles = computed(() => this.equipos().filter((e) => e.estado === 'DISPONIBLE'));

  get equipoPreseleccionado(): number | null {
    return this.equipoId ? +this.equipoId : null;
  }

  ngOnInit(): void {
    this.actualizar();
  }

  /** Vuelve a consultar los equipos (para el formulario) y la página actual de reservas, con el filtro activo. */
  actualizar(): void {
    this.equiposApi.listar().subscribe({ next: (l) => this.equipos.set(l), error: (e) => this.avisos.error(e) });
    if (this.puedeFiltrarPorUsuario) {
      this.usuariosApi.opciones().subscribe({ next: (l) => this.usuarios.set(l), error: (e) => this.avisos.error(e) });
    }
    this.cargar();
  }

  nombreEquipo(id: number): string {
    return this.equipos().find((e) => e.id === id)?.nombre ?? `Equipo #${id}`;
  }

  cambiarEquipo(valor: string): void {
    this.equipoFiltro.set(valor ? +valor : null);
    this.numeroPagina.set(0);
    this.cargar();
  }

  cambiarUsuario(valor: string): void {
    this.usuarioFiltro.set(valor ? +valor : null);
    this.numeroPagina.set(0);
    this.cargar();
  }

  limpiarFiltros(): void {
    this.filtro.set('');
    this.equipoFiltro.set(null);
    this.usuarioFiltro.set(null);
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

  cambiarFiltro(valor: string): void {
    this.filtro.set(valor as EstadoReserva | '');
    this.numeroPagina.set(0);
    this.cargar();
  }

  irAPagina(pagina: number): void {
    this.numeroPagina.set(pagina);
    this.cargar();
  }

  yaInicio(r: Reserva): boolean {
    return new Date(r.fechaInicio).getTime() <= Date.now();
  }

  crear(dto: ReservaRequest): void {
    this.guardando.set(true);
    this.reservasApi.crear(dto).subscribe({
      next: () => {
        this.avisos.exito('Reserva confirmada');
        this.guardando.set(false);
        this.formulario?.reiniciar();
        this.cargar();
      },
      error: (e) => {
        this.avisos.error(e); // por ejemplo 409: el equipo ya está reservado en ese horario
        this.guardando.set(false);
      },
    });
  }

  cambiar(r: Reserva, estado: EstadoReserva): void {
    this.reservasApi.cambiarEstado(r.id, estado).subscribe({
      next: () => {
        this.avisos.exito(`Reserva #${r.id} actualizada a ${estado}`);
        this.cargar();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  private cargar(): void {
    if (this.rangoInvalido()) return;
    this.reservasApi.pagina(
      this.numeroPagina(),
      this.tamanioPagina,
      this.filtro(),
      this.equipoFiltro(),
      this.usuarioFiltro(),
      this.desde(),
      this.hasta(),
    ).subscribe({
      next: (p) => {
        // Si se vació la última página (por ejemplo, al cancelar su único elemento), vuelve a la anterior
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
}
