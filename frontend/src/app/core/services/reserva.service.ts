import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { EstadoReserva, PaginaApi, Reserva, ReservaRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ReservaService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/reservas`;

  /** Listado por página (el estudiante solo recibe las suyas). Con miles de reservas no se pide todo de una vez. */
  pagina(
    pagina: number,
    tamanio: number,
    estado: EstadoReserva | '' = '',
    equipoId: number | null = null,
    usuarioId: number | null = null,
    desde = '',
    hasta = '',
  ) {
    let params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    if (estado) params = params.set('estado', estado);
    if (equipoId) params = params.set('equipoId', equipoId);
    if (usuarioId) params = params.set('usuarioId', usuarioId);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    return this.http.get<PaginaApi<Reserva>>(`${this.base}/pagina`, { params });
  }

  obtener(id: number) { return this.http.get<Reserva>(`${this.base}/${id}`); }

  /** Cola del técnico: reservas confirmadas que ya empezaron o empiezan en las próximas `horas`. */
  pendientesEntrega(horas = 24) {
    const params = new HttpParams().set('horas', horas);
    return this.http.get<Reserva[]>(`${this.base}/pendientes-entrega`, { params });
  }

  crear(dto: ReservaRequest) { return this.http.post<Reserva>(this.base, dto); }
  cambiarEstado(id: number, estado: EstadoReserva) {
    return this.http.patch<Reserva>(`${this.base}/${id}/estado`, { estado });
  }
}
