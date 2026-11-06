import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  ConflictoMetricas, DisponibilidadMetricas, MantenimientoMetricas, PrestamoMetricas, ReservaMetricas,
} from '../models/api.models';

/** Indicadores de la matriz de consistencia (los que expone el backend como /reportes/...). */
@Injectable({ providedIn: 'root' })
export class ReporteService {
  private http = inject(HttpClient);
  private api = environment.apiUrl;

  noShow() { return this.http.get<ReservaMetricas>(`${this.api}/reservas/reportes/no-show`); }
  conflictos() { return this.http.get<ConflictoMetricas>(`${this.api}/reservas/reportes/conflictos`); }
  mora() { return this.http.get<PrestamoMetricas>(`${this.api}/prestamos/reportes/mora`); }
  mantenimientoTardio() { return this.http.get<MantenimientoMetricas>(`${this.api}/mantenimientos/reportes/mantenimiento`); }
  exactitudDisponibilidad() { return this.http.get<DisponibilidadMetricas>(`${this.api}/equipos/reportes/disponibilidad`); }
}
