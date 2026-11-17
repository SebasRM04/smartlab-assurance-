import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import {
  EstadoMantenimiento, Mantenimiento, MantenimientoRequest, PaginaApi, TipoMantenimiento,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class MantenimientoService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/mantenimientos`;

  pagina(
    pagina: number,
    tamanio: number,
    estado: EstadoMantenimiento | '' = '',
    tipo: TipoMantenimiento | '' = '',
    equipoId: number | null = null,
    desde = '',
    hasta = '',
  ) {
    let params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    if (estado) params = params.set('estado', estado);
    if (tipo) params = params.set('tipo', tipo);
    if (equipoId) params = params.set('equipoId', equipoId);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<PaginaApi<Mantenimiento>>(`${this.base}/pagina`, { params });
  }

  programar(dto: MantenimientoRequest) { return this.http.post<Mantenimiento>(this.base, dto); }
  cerrar(id: number) { return this.http.patch<Mantenimiento>(`${this.base}/${id}/cerrar`, {}); }
}
