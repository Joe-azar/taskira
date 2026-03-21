import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { AuthService } from '../auth/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.hasToken()) {
    return router.createUrlTree(['/login']);
  }

  const currentUser = authService.currentUser;
  if (currentUser) {
    return currentUser.role === 'ADMIN'
      ? true
      : router.createUrlTree(['/dashboard']);
  }

  // Fallback static route while user data load is pending.
  // On refresh, l’utilisateur sera chargé via fetchMe() et la protection se réappliquera.
  return router.createUrlTree(['/dashboard']);
};
