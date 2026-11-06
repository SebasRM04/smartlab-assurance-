import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models/api.models';
import { AuthService } from '../services/auth.service';

/** Exige sesión iniciada; si no, manda al login recordando la ruta pedida. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.estaAutenticado()) {
    return true;
  }
  if (auth.sesion()) {
    auth.logout(false); // el token venció: se limpia la sesión guardada
  }
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/** Exige que el rol del usuario esté en route.data['roles']. */
export const rolGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = (route.data['roles'] ?? []) as Rol[];

  return auth.tieneRol(roles) ? true : router.createUrlTree(['/sin-permiso']);
};

/** Login y registro: si ya hay sesión, se va al panel. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.estaAutenticado() ? router.createUrlTree(['/dashboard']) : true;
};
