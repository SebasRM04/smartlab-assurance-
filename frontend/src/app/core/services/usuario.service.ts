import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Auditoria, Usuario, UsuarioRequest, UsuarioUpdate } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/usuarios`;

  listar() { return this.http.get<Usuario[]>(this.base); }
  crear(dto: UsuarioRequest) { return this.http.post<Usuario>(this.base, dto); }
  actualizar(id: number, dto: UsuarioUpdate) { return this.http.put<Usuario>(`${this.base}/${id}`, dto); }
  eliminar(id: number) { return this.http.delete<void>(`${this.base}/${id}`); }
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/auditoria`;

  listar() { return this.http.get<Auditoria[]>(this.base); }
  listarPorUsuario(usuarioId: number) { return this.http.get<Auditoria[]>(`${this.base}/usuario/${usuarioId}`); }
}
