import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { JwtResponse, LoginRequest, RegistroRequest, Rol, Sesion, Usuario } from '../models/api.models';

const CLAVE_SESION = 'smartlab.sesion';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private base = `${environment.apiUrl}/auth`;

  private _sesion = signal<Sesion | null>(this.leerSesion());
  readonly sesion = this._sesion.asReadonly();
  readonly rol = computed(() => this._sesion()?.rol ?? null);
  readonly nombre = computed(() => this._sesion()?.nombre ?? '');

  login(dto: LoginRequest): Observable<Sesion> {
    return this.http.post<JwtResponse>(`${this.base}/login`, dto).pipe(
      map((r) => ({ token: r.token, usuarioId: r.usuarioId, nombre: r.nombre, email: r.email, rol: r.rol })),
      tap((sesion) => this.guardar(sesion)),
    );
  }

  registrar(dto: RegistroRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.base}/registro`, dto);
  }

  /** Cierra la sesión local. Con redirigir=true lleva al login. */
  logout(redirigir = true): void {
    localStorage.removeItem(CLAVE_SESION);
    this._sesion.set(null);
    if (redirigir) {
      this.router.navigate(['/login']);
    }
  }

  token(): string | null {
    return this._sesion()?.token ?? null;
  }

  /** Hay sesión y el JWT no ha vencido. No modifica el estado. */
  estaAutenticado(): boolean {
    const sesion = this._sesion();
    return !!sesion && !this.tokenVencido(sesion.token);
  }

  tieneRol(roles: readonly Rol[]): boolean {
    const rol = this._sesion()?.rol;
    return !!rol && roles.includes(rol);
  }

  private guardar(sesion: Sesion): void {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    this._sesion.set(sesion);
  }

  private leerSesion(): Sesion | null {
    try {
      const bruto = localStorage.getItem(CLAVE_SESION);
      return bruto ? (JSON.parse(bruto) as Sesion) : null;
    } catch {
      return null;
    }
  }

  private tokenVencido(token: string): boolean {
    try {
      const carga = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const { exp } = JSON.parse(atob(carga)) as { exp?: number };
      return !!exp && exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}
