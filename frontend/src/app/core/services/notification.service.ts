import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { mensajeDeError } from '../utils/errores';

export interface Toast {
  id: number;
  tipo: 'exito' | 'error' | 'info';
  texto: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private siguiente = 1;
  readonly toasts = signal<Toast[]>([]);

  exito(texto: string): void {
    this.mostrar('exito', texto);
  }

  info(texto: string): void {
    this.mostrar('info', texto);
  }

  /** Muestra el error del backend. Un 401 de sesión vencida ya lo atiende el interceptor. */
  error(causa: unknown): void {
    if (causa instanceof HttpErrorResponse && causa.status === 401 && !causa.url?.includes('/auth/')) {
      return;
    }
    this.mostrar('error', typeof causa === 'string' ? causa : mensajeDeError(causa));
  }

  cerrar(id: number): void {
    this.toasts.update((lista) => lista.filter((t) => t.id !== id));
  }

  private mostrar(tipo: Toast['tipo'], texto: string): void {
    const id = this.siguiente++;
    this.toasts.update((lista) => [...lista, { id, tipo, texto }]);
    setTimeout(() => this.cerrar(id), 6000);
  }
}
