import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let authService: AuthService;
  let httpTesting: HttpTestingController;

  const router = {
    navigate: vi.fn(),
  };

  beforeEach(() => {
    router.navigate.mockReset();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });

    authService = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('logs in by posting credentials then fetching the session user', async () => {
    const resultPromise = firstValueFrom(
      authService.login({
        email: 'user@example.test',
        password: 'not-a-real-password',
      })
    );

    const loginRequest = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
    expect(loginRequest.request.method).toBe('POST');
    expect(loginRequest.request.body).toEqual({
      email: 'user@example.test',
      password: 'not-a-real-password',
    });
    loginRequest.flush({});

    const meRequest = httpTesting.expectOne(`${environment.apiUrl}/auth/me`);
    expect(meRequest.request.method).toBe('GET');
    meRequest.flush({
      id: 7,
      firstName: 'Test',
      lastName: 'User',
      fullName: 'Test User',
      email: 'user@example.test',
      globalRole: 'ADMIN',
      active: true,
    });

    await expect(resultPromise).resolves.toEqual({
      id: 7,
      email: 'user@example.test',
      role: 'ADMIN',
      firstName: 'Test',
      lastName: 'User',
      displayName: 'Test User',
    });
    expect(authService.currentUser?.role).toBe('ADMIN');
  });

  it('finishes bootstrap with a hydrated user when a session cookie is valid', async () => {
    let initialized = false;
    const subscription = authService.initialized$.subscribe(
      (value) => (initialized = value)
    );

    authService.bootstrapSession().subscribe();
    httpTesting.expectOne(`${environment.apiUrl}/auth/me`).flush({
      id: 8,
      firstName: 'Second',
      lastName: 'User',
      fullName: 'Second User',
      email: 'second@example.test',
      globalRole: 'USER',
      active: true,
    });

    expect(initialized).toBe(true);
    expect(authService.currentUser).toMatchObject({
      id: 8,
      email: 'second@example.test',
      role: 'USER',
    });
    subscription.unsubscribe();
  });

  it('finishes bootstrap with no user when there is no valid session', () => {
    let initialized = false;
    const subscription = authService.initialized$.subscribe(
      (value) => (initialized = value)
    );

    authService.bootstrapSession().subscribe();
    httpTesting.expectOne(`${environment.apiUrl}/auth/me`).flush(
      { detail: 'Unauthorized' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(initialized).toBe(true);
    expect(authService.currentUser).toBeNull();
    subscription.unsubscribe();
  });

  it('clears the session and redirects on logout by default', () => {
    authService.logout();

    httpTesting.expectOne(`${environment.apiUrl}/auth/logout`).flush(null);

    expect(authService.currentUser).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('does not redirect when logout is called with redirectToLogin=false', () => {
    authService.logout(false);

    httpTesting.expectOne(`${environment.apiUrl}/auth/logout`).flush(null);

    expect(authService.currentUser).toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('still clears the session locally even if the logout request fails', () => {
    authService.logout();

    httpTesting
      .expectOne(`${environment.apiUrl}/auth/logout`)
      .flush({ detail: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authService.currentUser).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
