import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { EstadoIncidencia, Incidencia, IncidenciaRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class IncidenciaService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/incidencias`;

  listar() { return this.http.get<Incidencia[]>(this.base); }
  listarPorEquipo(equipoId: number) { return this.http.get<Incidencia[]>(`${this.base}/equipo/${equipoId}`); }
  registrar(dto: IncidenciaRequest) { return this.http.post<Incidencia>(this.base, dto); }
  cambiarEstado(id: number, estado: EstadoIncidencia) {
    return this.http.patch<Incidencia>(`${this.base}/${id}/estado`, { estado });
  }
}
