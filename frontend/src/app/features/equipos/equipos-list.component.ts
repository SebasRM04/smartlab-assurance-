import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ESTADOS_EQUIPO, Equipo } from '../../core/models/api.models';
import { PERMISOS } from '../../core/permisos';
import { AuthService } from '../../core/services/auth.service';
import { EquipoService } from '../../core/services/equipo.service';
import { NotificationService } from '../../core/services/notification.service';
import { paginaValida, rebanar, totalPaginasDe } from '../../core/utils/paginacion';
import { EstadoBadgeComponent } from '../../shared/components/estado-badge.component';
import { PaginadorComponent } from '../../shared/components/paginador.component';
import { BotonActualizarComponent } from '../../shared/components/boton-actualizar.component';

@Component({
  selector: 'app-equipos-list',
  standalone: true,
  imports: [BotonActualizarComponent, RouterLink, EstadoBadgeComponent, PaginadorComponent],
  template: `
    <div class="page-header">
      <div><h1>Equipos</h1><p>Catálogo de equipos del laboratorio</p></div>
      <div class="toolbar">
        <app-boton-actualizar (actualizar)="cargar()" />
        @if (puedeCrear) {
          <a class="btn btn-primary" routerLink="/equipos/nuevo">Nuevo equipo</a>
        }
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <input #txt type="search" placeholder="Buscar por nombre o ubicación" style="min-width: 260px"
          (input)="buscar(txt.value)" />
        <select #sel (change)="elegirEstado(sel.value)">
          <option value="">Todos los estados</option>
          @for (e of estados; track e) { <option [value]="e">{{ e }}</option> }
        </select>
      </div>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>Nombre</th><th>Categoría</th><th>Ubicación</th><th>Estado</th><th>Horas de uso</th><th></th></tr>
          </thead>
          <tbody>
            @for (e of visibles(); track e.id) {
              <tr>
                <td><a [routerLink]="['/equipos', e.id]">{{ e.nombre }}</a></td>
                <td>{{ e.categoriaNombre }}</td>
                <td>{{ e.ubicacion || '—' }}</td>
                <td><app-estado-badge [estado]="e.estado" /></td>
                <td>{{ e.contadorUsoHoras }}</td>
                <td class="acciones">
                  <a class="btn btn-sm" [routerLink]="['/equipos', e.id]">Detalle</a>
                  @if (puedeCrear) {
                    <a class="btn btn-sm" [routerLink]="['/equipos', e.id, 'editar']">Editar</a>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="vacio">{{ cargando() ? 'Cargando…' : 'No hay equipos que coincidan' }}</td></tr>
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
    </div>
  `,
})
export class EquiposListComponent implements OnInit {
  private equiposApi = inject(EquipoService);
  private avisos = inject(NotificationService);
  private auth = inject(AuthService);

  readonly estados = ESTADOS_EQUIPO;
  readonly tamanioPagina = 20;
  readonly puedeCrear = this.auth.tieneRol(PERMISOS.equipoEscribir);

  equipos = signal<Equipo[]>([]);
  cargando = signal(true);
  busqueda = signal('');
  estadoSel = signal('');
  pagina = signal(0);

  filtrados = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const estado = this.estadoSel();
    return this.equipos().filter(
      (e) =>
        (!estado || e.estado === estado) &&
        (!texto || `${e.nombre} ${e.ubicacion ?? ''} ${e.categoriaNombre}`.toLowerCase().includes(texto)),
    );
  });

  totalPaginas = computed(() => totalPaginasDe(this.filtrados().length, this.tamanioPagina));
  paginaActual = computed(() => paginaValida(this.pagina(), this.filtrados().length, this.tamanioPagina));
  visibles = computed(() => rebanar(this.filtrados(), this.pagina(), this.tamanioPagina));

  buscar(texto: string): void {
    this.busqueda.set(texto);
    this.pagina.set(0);
  }

  elegirEstado(estado: string): void {
    this.estadoSel.set(estado);
    this.pagina.set(0);
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.equiposApi.listar().subscribe({
      next: (lista) => {
        this.equipos.set(lista);
        this.cargando.set(false);
      },
      error: (e) => {
        this.avisos.error(e);
        this.cargando.set(false);
      },
    });
  }
}
