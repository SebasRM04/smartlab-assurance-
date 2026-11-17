import { HttpErrorResponse } from '@angular/common/http';
import { ErrorApi } from '../models/api.models';

/** Convierte cualquier error HTTP en un texto legible para el usuario. */
export function mensajeDeError(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución en el puerto 8080.';
    }
    const cuerpo = err.error as ErrorApi | string | null;
    if (cuerpo && typeof cuerpo === 'object' && cuerpo.message) {
      const detalles = cuerpo.detalles?.length ? ` (${cuerpo.detalles.join('; ')})` : '';
      return cuerpo.message + detalles;
    }
    return `Error ${err.status}: ${err.statusText || 'respuesta inesperada del servidor'}`;
  }
  return 'Ocurrió un error inesperado';
}
