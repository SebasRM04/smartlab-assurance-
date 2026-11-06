import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  Equipo, EquipoRecomendacion, EquipoRequest, EstadoEquipo, HistorialEstado, UtilizacionDashboard,
  UtilizacionEquipo,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class EquipoService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/equipos`;

  listar() { return this.http.get<Equipo[]>(this.base); }
  obtener(id: number) { return this.http.get<Equipo>(`${this.base}/${id}`); }
  crear(dto: EquipoRequest) { return this.http.post<Equipo>(this.base, dto); }
  actualizar(id: number, dto: EquipoRequest) { return this.http.put<Equipo>(`${this.base}/${id}`, dto); }
  eliminar(id: number) { return this.http.delete<void>(`${this.base}/${id}`); }

  disponibilidad(id: number) { return this.http.get<{ estado: EstadoEquipo }>(`${this.base}/${id}/disponibilidad`); }
  historial(id: number) { return this.http.get<HistorialEstado[]>(`${this.base}/${id}/historial`); }
  utilizacion(id: number) { return this.http.get<UtilizacionEquipo[]>(`${this.base}/${id}/utilizacion`); }

  dashboardUtilizacion(umbralSobreutilizacionHoras: number) {
    const params = new HttpParams().set('umbralSobreutilizacionHoras', umbralSobreutilizacionHoras);
    return this.http.get<UtilizacionDashboard>(`${this.base}/reportes/utilizacion`, { params });
  }

  recomendaciones(umbralHoras: number) {
    const params = new HttpParams().set('umbralHoras', umbralHoras);
    return this.http.get<EquipoRecomendacion[]>(`${this.base}/recomendaciones-mantenimiento`, { params });
  }
}
