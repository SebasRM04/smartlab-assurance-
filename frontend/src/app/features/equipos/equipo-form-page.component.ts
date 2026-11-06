import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Categoria, Equipo, EquipoRequest } from '../../core/models/api.models';
import { CategoriaService } from '../../core/services/categoria.service';
import { EquipoService } from '../../core/services/equipo.service';
import { NotificationService } from '../../core/services/notification.service';
import { EquipoFormComponent } from './equipo-form.component';

/** Página (smart) para crear o editar un equipo. Si llega :id en la ruta, edita. */
@Component({
  selector: 'app-equipo-form-page',
  standalone: true,
  imports: [EquipoFormComponent],
  template: `
    <div class="page-header">
      <h1>{{ id ? 'Editar equipo' : 'Nuevo equipo' }}</h1>
    </div>
    <div class="card">
      @if (categorias().length === 0) {
        <div class="alert alert-info">No hay categorías registradas. Pide a un administrador que cree al menos una.</div>
      }
      <app-equipo-form
        [categorias]="categorias()"
        [equipo]="equipo()"
        [guardando]="guardando()"
        (guardar)="guardar($event)"
        (cancelar)="volver()"
      />
    </div>
  `,
})
export class EquipoFormPageComponent implements OnInit {
  @Input() id?: string; // viene de la ruta (withComponentInputBinding)

  private equipos = inject(EquipoService);
  private categoriasApi = inject(CategoriaService);
  private avisos = inject(NotificationService);
  private router = inject(Router);

  categorias = signal<Categoria[]>([]);
  equipo = signal<Equipo | null>(null);
  guardando = signal(false);

  ngOnInit(): void {
    this.categoriasApi.listar().subscribe({
      next: (lista) => this.categorias.set(lista),
      error: (e) => this.avisos.error(e),
    });
    if (this.id) {
      this.equipos.obtener(+this.id).subscribe({
        next: (e) => this.equipo.set(e),
        error: (e) => this.avisos.error(e),
      });
    }
  }

  guardar(dto: EquipoRequest): void {
    this.guardando.set(true);
    const peticion = this.id ? this.equipos.actualizar(+this.id, dto) : this.equipos.crear(dto);
    peticion.subscribe({
      next: (e) => {
        this.avisos.exito(this.id ? 'Equipo actualizado' : 'Equipo registrado con estado DISPONIBLE');
        this.router.navigate(['/equipos', e.id]);
      },
      error: (e) => {
        this.avisos.error(e);
        this.guardando.set(false);
      },
    });
  }

  volver(): void {
    this.router.navigate(this.id ? ['/equipos', this.id] : ['/equipos']);
  }
}
