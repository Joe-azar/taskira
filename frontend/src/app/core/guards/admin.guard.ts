import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { catchError, map, of } from 'rxjs';

import { AuthService } from '../auth/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const currentUser = authService.currentUser;

  if (currentUser) {
    return currentUser.role === 'ADMIN'
      ? true
      : router.createUrlTree(['/dashboard']);
  }

  return authService.fetchMe().pipe(
    map((user) => {
      if (user.role === 'ADMIN') {
        return true;
      }
      return router.createUrlTree(['/dashboard']);
    }),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
