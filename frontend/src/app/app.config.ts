import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { appRoutes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(appRoutes),
    // No withXsrfConfiguration() here: it deliberately withholds the token for
    // cross-origin requests, which is exactly what every API call in this project is
    // (frontend and backend are always on different ports). authInterceptor attaches
    // the XSRF header itself instead - see its comment for why.
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    // Blocks the app from rendering any route - including the public /login page - until
    // the initial session check resolves. Without this, a fast form submission on first
    // load could race the XSRF-TOKEN cookie that check seeds and be rejected as missing
    // its CSRF token.
    provideAppInitializer(() => firstValueFrom(inject(AuthService).bootstrapSession())),
  ],
};
