import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Auditoria } from '../../core/models/api.models';
import { AuditoriaService } from '../../core/services/usuario.service';
import { NotificationService } from '../../core/services/notification.service';
import { paginaValida, rebanar, totalPaginasDe } from '../../core/utils/paginacion';
import { PaginadorComponent } from '../../shared/components/paginador.component';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, PaginadorComponent],
  template: `
    <div class="page-header">
      <div><h1>Auditoría</h1><p>Acciones registradas por el sistema</p></div>
    </div>

    <section class="card">
      <div class="toolbar">
        <input type="number" min="1" placeholder="ID de usuario" [formControl]="usuarioId" style="width: 150px" />
        <button class="btn btn-primary btn-sm" type="button" (click)="filtrar()">Filtrar por usuario</button>
        <button class="btn btn-sm" type="button" (click)="verTodo()">Ver todo</button>
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
              <tr><td colspan="5" class="vacio">Sin registros</td></tr>
            }
          </tbody>
        </table>
      </div>
      <app-paginador
        [pagina]="paginaActual()"
        [totalPaginas]="totalPaginas()"
        [totalElementos]="registros().length"
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
  totalPaginas = computed(() => totalPaginasDe(this.registros().length, this.tamanioPagina));
  paginaActual = computed(() => paginaValida(this.pagina(), this.registros().length, this.tamanioPagina));
  visibles = computed(() => rebanar(this.registros(), this.pagina(), this.tamanioPagina));
  usuarioId = new FormControl<number | null>(null);

  ngOnInit(): void {
    this.verTodo();
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
