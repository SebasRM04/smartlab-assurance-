import { AbstractControl, ValidationErrors } from '@angular/forms';

/** "2026-11-03T09:00" (datetime-local) -> "2026-11-03T09:00:00" (LocalDateTime del backend). */
export function aIso(valor: string): string {
  return valor.length === 16 ? `${valor}:00` : valor;
}

/** Valida que la fecha sea futura (equivale a @Future del backend). */
export function fechaFutura(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  return new Date(control.value).getTime() > Date.now() ? null : { futura: true };
}

/** Validador de grupo: fechaFin debe ser posterior a fechaInicio. */
export function rangoValido(grupo: AbstractControl): ValidationErrors | null {
  const inicio = grupo.get('fechaInicio')?.value;
  const fin = grupo.get('fechaFin')?.value;
  if (!inicio || !fin) return null;
  return new Date(fin).getTime() > new Date(inicio).getTime() ? null : { rango: true };
}

/** Valor por defecto para un input datetime-local: ahora + N horas, redondeado a la hora. */
export function enHoras(horas: number): string {
  const d = new Date(Date.now() + horas * 3600_000);
  d.setMinutes(0, 0, 0);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:00`;
}
