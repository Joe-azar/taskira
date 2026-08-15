import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';

const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';

function readCookie(name: string): string | null {
  const match = document.cookie.split('; ').find((row) => row.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.substring(name.length + 1)) : null;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Every /auth/* endpoint is excluded, not just login/register: /auth/me now gets called
  // unconditionally by every guard (there's no client-readable session signal anymore), so
  // a 401 from it just means "not logged in" - not "your session expired mid-use". Treating
  // it as the latter triggered logout() + a redirect to /login, whose own guestGuard then
  // called /auth/me again, 401'd again, and redirected again: an infinite loop that left the
  // app stuck on a blank page.
  const isAuthRequest = req.url.includes('/auth/');

  // The session lives in an HttpOnly cookie the browser manages on its own; this only
  // needs to make sure it (and the XSRF-TOKEN cookie) actually gets sent, since the API
  // is on a different origin from the app even in local dev.
  let outgoingReq = req.clone({ withCredentials: true });

  // Angular's built-in withXsrfConfiguration() deliberately withholds the token for
  // cross-origin requests, to avoid leaking it to some unrelated third-party origin the
  // app might also call - a sensible default, but it means it silently does nothing here,
  // since the API is genuinely a different origin from the app (different ports) in every
  // environment this project runs in today. Reading the cookie and attaching the header
  // ourselves works regardless of same- or cross-origin.
  const csrfToken = readCookie(CSRF_COOKIE_NAME);
  if (csrfToken) {
    outgoingReq = outgoingReq.clone({ setHeaders: { [CSRF_HEADER_NAME]: csrfToken } });
  }

  return next(outgoingReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isAuthRequest) {
        authService.logout(false);
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};
