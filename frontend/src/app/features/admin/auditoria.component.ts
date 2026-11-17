import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Auditoria } from '../../core/models/api.models';
import { AuditoriaService } from '../../core/services/usuario.service';
import { NotificationService } from '../../core/services/notification.service';
import { paginaValida, rebanar, totalPaginasDe } from '../../core/utils/paginacion';
import { PaginadorComponent } from '../../shared/components/paginador.component';
import { BotonActualizarComponent } from '../../shared/components/boton-actualizar.component';
import { FiltroFechasComponent, RangoFecha } from '../../shared/components/filtro-fechas.component';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [BotonActualizarComponent, DatePipe, ReactiveFormsModule, PaginadorComponent, FiltroFechasComponent],
  template: `
    <div class="page-header">
      <div><h1>Auditoría</h1><p>Acciones registradas por el sistema</p></div>
      <app-boton-actualizar (actualizar)="actualizar()" />
    </div>

    <section class="card">
      <div class="toolbar">
        <input type="number" min="1" placeholder="ID de usuario" [formControl]="usuarioId" style="width: 150px" />
        <button class="btn btn-primary btn-sm" type="button" (click)="filtrar()">Filtrar por usuario</button>
        <button class="btn btn-sm" type="button" (click)="verTodo()">Ver todo</button>
        <select [value]="accionSel()" (change)="elegirAccion($any($event.target).value)" aria-label="Filtrar por acción">
          <option value="">Todas las acciones</option>
          @for (a of acciones(); track a) { <option [value]="a">{{ a }}</option> }
        </select>
        <input type="search" placeholder="Buscar en usuario, entidad o detalle" style="min-width: 240px"
          [value]="busqueda()" (input)="buscar($any($event.target).value)" />
        <app-filtro-fechas #fechas etiqueta="Fecha" (cambio)="cambiarFechas($event)" />
        @if (accionSel() || busqueda().trim() || fechas.activo) {
          <button class="btn btn-sm" type="button" (click)="fechas.limpiar(); limpiarFiltros()">Limpiar filtros</button>
        }
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Fecha y hora</th><th>Usuario</th><th>Acción</th><th>Entidad</th><th>Detalle</th></tr></thead>
          <tbody>
            @for (a of visibles(); track a.id) {
              <tr>
                <td>{{ a.fechaHora | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                <td>{{ a.usuarioNombre || '—' }}</td>
                <td>{{ a.accion }}</td>
                <td>{{ a.entidadAfectada }}</td>
                <td>{{ a.detalle || '—' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="vacio">{{ accionSel() || busqueda().trim() ? 'Ningún registro coincide con los filtros' : 'Sin registros' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
      <app-paginador
        [pagina]="paginaActual()"
        [totalPaginas]="totalPaginas()"
        [totalElementos]="filtrados().length"
        [tamanio]="tamanioPagina"
        (cambio)="pagina.set($event)"
      />
    </section>
  `,
})
export class AuditoriaComponent implements OnInit {
  private api = inject(AuditoriaService);
  private avisos = inject(NotificationService);

  readonly tamanioPagina = 25;
  registros = signal<Auditoria[]>([]);
  pagina = signal(0);
  // Filtros del navegador sobre los registros cargados (el filtro por usuario consulta al servidor)
  accionSel = signal('');
  busqueda = signal('');
  acciones = computed(() => [...new Set(this.registros().map((r) => r.accion))].sort());
  desde = signal('');
  hasta = signal('');
  filtrados = computed(() => {
    const accion = this.accionSel();
    const texto = this.busqueda().trim().toLowerCase();
    const desde = this.desde();
    const hasta = this.hasta();
    if (desde && hasta && desde > hasta) return [];
    return this.registros().filter((r) => {
      const dia = diaLocal(r.fechaHora);
      return (
        (!accion || r.accion === accion) &&
        (!desde || dia >= desde) &&
        (!hasta || dia <= hasta) &&
        (!texto || `${r.usuarioNombre ?? ''} ${r.entidadAfectada} ${r.detalle ?? ''}`.toLowerCase().includes(texto))
      );
    });
  });
  totalPaginas = computed(() => totalPaginasDe(this.filtrados().length, this.tamanioPagina));
  paginaActual = computed(() => paginaValida(this.pagina(), this.filtrados().length, this.tamanioPagina));
  visibles = computed(() => rebanar(this.filtrados(), this.pagina(), this.tamanioPagina));
  usuarioId = new FormControl<number | null>(null);

  ngOnInit(): void {
    this.verTodo();
  }

  /** Recarga respetando el filtro: si hay un ID de usuario escrito, vuelve a filtrar por él. */
  actualizar(): void {
    if (this.usuarioId.value) this.filtrar();
    else this.verTodo();
  }

  elegirAccion(valor: string): void {
    this.accionSel.set(valor);
    this.pagina.set(0);
  }

  buscar(texto: string): void {
    this.busqueda.set(texto);
    this.pagina.set(0);
  }

  limpiarFiltros(): void {
    this.accionSel.set('');
    this.busqueda.set('');
    this.desde.set('');
    this.hasta.set('');
    this.pagina.set(0);
  }

  cambiarFechas(rango: RangoFecha): void {
    this.desde.set(rango.desde);
    this.hasta.set(rango.hasta);
    this.pagina.set(0);
  }

  verTodo(): void {
    this.usuarioId.reset();
    this.api.listar().subscribe({
      next: (l) => {
        this.registros.set(l);
        this.pagina.set(0);
      },
      error: (e) => this.avisos.error(e),
    });
  }

  filtrar(): void {
    const id = this.usuarioId.value;
    if (!id) {
      this.avisos.info('Escribe el ID del usuario a filtrar');
      return;
    }
    this.api.listarPorUsuario(id).subscribe({
      next: (l) => {
        this.registros.set(l);
        this.pagina.set(0);
      },
      error: (e) => this.avisos.error(e),
    });
  }
}

function diaLocal(iso: string): string {
  const d = new Date(iso);
  const z = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;
}
