import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Categoria, CategoriaRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/categorias`;

  listar() { return this.http.get<Categoria[]>(this.base); }
  crear(dto: CategoriaRequest) { return this.http.post<Categoria>(this.base, dto); }
  actualizar(id: number, dto: CategoriaRequest) { return this.http.put<Categoria>(`${this.base}/${id}`, dto); }
  eliminar(id: number) { return this.http.delete<void>(`${this.base}/${id}`); }
}
