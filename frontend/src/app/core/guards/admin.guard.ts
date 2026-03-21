import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { AuthService } from '../auth/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const currentUser = authService.currentUser;
  if (currentUser?.role === 'ADMIN') {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
