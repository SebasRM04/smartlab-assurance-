import { Component, inject } from '@angular/core';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    <div class="toasts">
      @for (t of avisos.toasts(); track t.id) {
        <div class="toast" [class]="t.tipo" (click)="avisos.cerrar(t.id)">{{ t.texto }}</div>
      }
    </div>
  `,
  styleUrl: './toast.component.scss',
})
export class ToastComponent {
  avisos = inject(NotificationService);
}
