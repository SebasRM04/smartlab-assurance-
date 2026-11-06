import { Component, Input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

/** Muestra el primer error de validación de un control, una vez que el usuario lo tocó. */
@Component({
  selector: 'app-field-error',
  standalone: true,
  template: `@if (mensaje(); as m) { <small class="field-error">{{ m }}</small> }`,
})
export class FieldErrorComponent {
  @Input() control: AbstractControl | null = null;

  mensaje(): string | null {
    const c = this.control;
    if (!c || !c.errors || !(c.touched || c.dirty)) return null;
    const e = c.errors;
    if (e['required']) return 'Este campo es obligatorio';
    if (e['email']) return 'El correo no tiene un formato válido';
    if (e['minlength']) return `Debe tener al menos ${e['minlength'].requiredLength} caracteres`;
    if (e['maxlength']) return `No puede superar ${e['maxlength'].requiredLength} caracteres`;
    if (e['min']) return `El valor mínimo es ${e['min'].min}`;
    if (e['pattern']) return 'El formato no es válido';
    if (e['futura']) return 'La fecha debe ser futura';
    return 'Valor no válido';
  }
}
