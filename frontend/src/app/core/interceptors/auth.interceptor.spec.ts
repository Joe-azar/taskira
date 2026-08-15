import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;

  const authService = {
    logout: vi.fn(),
  };

  const router = {
    navigate: vi.fn(),
  };

  beforeEach(() => {
    authService.logout.mockReset();
    router.navigate.mockReset();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/';

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('sends the session cookie with every request', () => {
    httpClient.get('/api/v1/projects').subscribe();

    const request = httpTesting.expectOne('/api/v1/projects');
    expect(request.request.withCredentials).toBe(true);
    request.flush([]);
  });

  it('also sends the session cookie on auth requests, so the CSRF cookie flows too', () => {
    httpClient.post('/api/v1/auth/login', {}).subscribe();

    const request = httpTesting.expectOne('/api/v1/auth/login');
    expect(request.request.withCredentials).toBe(true);
    request.flush({});
  });

  it('attaches the XSRF-TOKEN cookie value as an X-XSRF-TOKEN header', () => {
    // Angular's own withXsrfConfiguration() withholds this for cross-origin requests,
    // which is what every request in this app is (the API is always on a different
    // port from the app) - so this interceptor reads the cookie and attaches the
    // header itself. This is the exact mechanism a real login form submission relies
    // on; regressing it silently breaks every mutating request with a 403.
    document.cookie = 'XSRF-TOKEN=test-csrf-token; path=/';

    httpClient.post('/api/v1/projects', {}).subscribe();

    const request = httpTesting.expectOne('/api/v1/projects');
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('test-csrf-token');
    request.flush({});
  });

  it('sends no X-XSRF-TOKEN header when the cookie has not been seeded yet', () => {
    httpClient.get('/api/v1/auth/me').subscribe({ error: () => undefined });

    const request = httpTesting.expectOne('/api/v1/auth/me');
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    request.flush(null, { status: 401, statusText: 'Unauthorized' });
  });

  it('clears the session and redirects after a protected request returns 401', () => {
    let receivedStatus: number | undefined;

    httpClient.get('/api/v1/projects').subscribe({
      error: (error) => (receivedStatus = error.status),
    });

    httpTesting.expectOne('/api/v1/projects').flush(
      { detail: 'Unauthorized' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(receivedStatus).toBe(401);
    expect(authService.logout).toHaveBeenCalledWith(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('leaves login failures to the login page', () => {
    let receivedStatus: number | undefined;

    httpClient.post('/api/v1/auth/login', {}).subscribe({
      error: (error) => (receivedStatus = error.status),
    });

    httpTesting.expectOne('/api/v1/auth/login').flush(
      { detail: 'Invalid credentials' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(receivedStatus).toBe(401);
    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does not treat a 401 from /auth/me as a session expiry (regression: this looped forever)', () => {
    // /auth/me now gets called unconditionally by every guard, since there is no
    // client-readable session signal anymore - a 401 from it just means "not logged
    // in", not "your session just expired". Reacting to it by logging out and
    // redirecting to /login previously created an infinite loop, because /login's own
    // guestGuard calls /auth/me again, which 401s again, which redirected again.
    let receivedStatus: number | undefined;

    httpClient.get('/api/v1/auth/me').subscribe({
      error: (error) => (receivedStatus = error.status),
    });

    httpTesting.expectOne('/api/v1/auth/me').flush(
      { detail: 'Unauthorized' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(receivedStatus).toBe(401);
    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
