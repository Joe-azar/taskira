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
import { TokenService } from './token.service';

describe('AuthService', () => {
  let authService: AuthService;
  let httpTesting: HttpTestingController;
  let tokenService: TokenService;

  const router = {
    navigate: vi.fn(),
  };

  beforeEach(() => {
    localStorage.clear();
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
    tokenService = TestBed.inject(TokenService);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('stores the JWT and exposes the user embedded in the login response', async () => {
    const resultPromise = firstValueFrom(
      authService.login({
        email: 'user@example.test',
        password: 'not-a-real-password',
      })
    );

    const request = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'user@example.test',
      password: 'not-a-real-password',
    });

    request.flush({
      token: 'test-jwt',
      user: {
        id: 7,
        email: 'user@example.test',
        firstName: 'Test',
        lastName: 'User',
        globalRole: 'ADMIN',
      },
    });

    await expect(resultPromise).resolves.toEqual({
      id: 7,
      email: 'user@example.test',
      firstName: 'Test',
      lastName: 'User',
      role: 'ADMIN',
      displayName: 'Test User',
    });
    expect(tokenService.getToken()).toBe('test-jwt');
    expect(authService.currentUser?.role).toBe('ADMIN');
  });

  it('loads /auth/me when the login response does not embed a user', async () => {
    const resultPromise = firstValueFrom(
      authService.login({
        email: 'user@example.test',
        password: 'not-a-real-password',
      })
    );

    httpTesting
      .expectOne(`${environment.apiUrl}/auth/login`)
      .flush({ accessToken: 'test-access-token' });

    const meRequest = httpTesting.expectOne(`${environment.apiUrl}/auth/me`);
    expect(meRequest.request.method).toBe('GET');
    meRequest.flush({
      id: 8,
      email: 'second@example.test',
      firstname: 'Second',
      lastname: 'User',
      role: 'USER',
    });

    await expect(resultPromise).resolves.toMatchObject({
      id: 8,
      email: 'second@example.test',
      displayName: 'Second User',
      role: 'USER',
    });
    expect(tokenService.getToken()).toBe('test-access-token');
  });

  it('rejects a login response that contains no JWT', async () => {
    const resultPromise = firstValueFrom(
      authService.login({
        email: 'user@example.test',
        password: 'not-a-real-password',
      })
    );

    httpTesting.expectOne(`${environment.apiUrl}/auth/login`).flush({
      user: { id: 9, email: 'user@example.test' },
    });

    await expect(resultPromise).rejects.toThrow('Token JWT absent');
    expect(tokenService.getToken()).toBeNull();
    expect(authService.currentUser).toBeNull();
  });

  it('finishes bootstrap immediately when no token is present', () => {
    let initialized = false;
    const subscription = authService.initialized$.subscribe(
      (value) => (initialized = value)
    );

    authService.bootstrapSession();

    expect(initialized).toBe(true);
    expect(authService.currentUser).toBeNull();
    subscription.unsubscribe();
  });

  it('clears an invalid persisted session when /auth/me fails', () => {
    tokenService.setToken('expired-test-token');
    let initialized = false;
    const subscription = authService.initialized$.subscribe(
      (value) => (initialized = value)
    );

    authService.bootstrapSession();
    httpTesting.expectOne(`${environment.apiUrl}/auth/me`).flush(
      { message: 'Unauthorized' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(initialized).toBe(true);
    expect(tokenService.getToken()).toBeNull();
    expect(authService.currentUser).toBeNull();
    subscription.unsubscribe();
  });

  it('clears the session and redirects on logout by default', () => {
    tokenService.setToken('test-jwt');

    authService.logout();

    expect(tokenService.getToken()).toBeNull();
    expect(authService.currentUser).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
