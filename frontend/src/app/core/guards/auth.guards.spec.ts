import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, firstValueFrom, of, throwError } from 'rxjs';

import { AuthUser } from '../models/auth.models';
import { AuthService } from '../auth/auth.service';
import { adminGuard } from './admin.guard';
import { authGuard } from './auth.guard';
import { guestGuard } from './guest.guard';

describe('authentication guards', () => {
  let router: Router;

  const admin: AuthUser = {
    id: 1,
    email: 'admin@example.test',
    role: 'ADMIN',
    displayName: 'Test Admin',
  };

  const user: AuthUser = {
    id: 2,
    email: 'user@example.test',
    role: 'USER',
    displayName: 'Test User',
  };

  const authService = {
    hasToken: vi.fn(),
    currentUser: null as AuthUser | null,
    fetchMe: vi.fn(),
  };

  const route = {} as ActivatedRouteSnapshot;

  beforeEach(() => {
    authService.hasToken.mockReset();
    authService.fetchMe.mockReset();
    authService.currentUser = null;

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    });

    router = TestBed.inject(Router);
  });

  it('allows an authenticated user through authGuard', () => {
    authService.hasToken.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard(route, { url: '/projects' } as RouterStateSnapshot)
    );

    expect(result).toBe(true);
  });

  it('redirects an anonymous user to login and preserves the requested URL', () => {
    authService.hasToken.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard(route, {
        url: '/tickets?mine=true',
      } as RouterStateSnapshot)
    ) as UrlTree;

    expect(result.queryParams['redirectTo']).toBe('/tickets?mine=true');
    expect(router.serializeUrl(result)).toContain('/login');
  });

  it('keeps an anonymous visitor on the login page', () => {
    authService.hasToken.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      guestGuard(route, { url: '/login' } as RouterStateSnapshot)
    );

    expect(result).toBe(true);
  });

  it('redirects an authenticated visitor away from the login page', () => {
    authService.hasToken.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      guestGuard(route, { url: '/login' } as RouterStateSnapshot)
    ) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/dashboard');
  });

  it('allows a cached admin through adminGuard', () => {
    authService.hasToken.mockReturnValue(true);
    authService.currentUser = admin;

    const result = TestBed.runInInjectionContext(() =>
      adminGuard(route, { url: '/users' } as RouterStateSnapshot)
    );

    expect(result).toBe(true);
    expect(authService.fetchMe).not.toHaveBeenCalled();
  });

  it('redirects a cached non-admin away from administration', () => {
    authService.hasToken.mockReturnValue(true);
    authService.currentUser = user;

    const result = TestBed.runInInjectionContext(() =>
      adminGuard(route, { url: '/users' } as RouterStateSnapshot)
    ) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/dashboard');
  });

  it('loads the session before allowing an admin with no cached user', async () => {
    authService.hasToken.mockReturnValue(true);
    authService.fetchMe.mockReturnValue(of(admin));

    const result = TestBed.runInInjectionContext(() =>
      adminGuard(route, { url: '/users' } as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>;

    await expect(firstValueFrom(result)).resolves.toBe(true);
  });

  it('redirects to login when loading the session fails', async () => {
    authService.hasToken.mockReturnValue(true);
    authService.fetchMe.mockReturnValue(
      throwError(() => new Error('Session unavailable'))
    );

    const result = TestBed.runInInjectionContext(() =>
      adminGuard(route, { url: '/users' } as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>;

    const resolved = await firstValueFrom(result);
    expect(router.serializeUrl(resolved as UrlTree)).toBe('/login');
  });
});
