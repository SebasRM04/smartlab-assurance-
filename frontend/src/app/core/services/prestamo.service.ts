import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { EstadoPrestamo, PaginaApi, Prestamo, PrestamoCheckoutRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PrestamoService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/prestamos`;

  pagina(pagina: number, tamanio: number, estado: EstadoPrestamo | '' = '') {
    let params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    if (estado) params = params.set('estado', estado);
    return this.http.get<PaginaApi<Prestamo>>(`${this.base}/pagina`, { params });
  }

  vencidos() { return this.http.get<Prestamo[]>(`${this.base}/vencidos`); }
  checkout(dto: PrestamoCheckoutRequest) { return this.http.post<Prestamo>(`${this.base}/checkout`, dto); }
  checkin(id: number) { return this.http.post<Prestamo>(`${this.base}/${id}/checkin`, {}); }
}
