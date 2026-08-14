import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { TokenService } from '../auth/token.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let tokenService: TokenService;

  const authService = {
    logout: vi.fn(),
  };

  const router = {
    navigate: vi.fn(),
  };

  beforeEach(() => {
    localStorage.clear();
    authService.logout.mockReset();
    router.navigate.mockReset();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        TokenService,
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    tokenService = TestBed.inject(TokenService);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('adds the bearer token to protected API requests', () => {
    tokenService.setToken('test-jwt');

    httpClient.get('/api/projects').subscribe();

    const request = httpTesting.expectOne('/api/projects');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test-jwt');
    request.flush([]);
  });

  it('does not add the bearer token to login requests', () => {
    tokenService.setToken('test-jwt');

    httpClient.post('/api/auth/login', {}).subscribe();

    const request = httpTesting.expectOne('/api/auth/login');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({ token: 'new-test-jwt' });
  });

  it('clears the session and redirects after a protected request returns 401', () => {
    tokenService.setToken('expired-test-token');
    let receivedStatus: number | undefined;

    httpClient.get('/api/projects').subscribe({
      error: (error) => (receivedStatus = error.status),
    });

    httpTesting.expectOne('/api/projects').flush(
      { message: 'Unauthorized' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(receivedStatus).toBe(401);
    expect(authService.logout).toHaveBeenCalledWith(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('leaves login failures to the login page', () => {
    let receivedStatus: number | undefined;

    httpClient.post('/api/auth/login', {}).subscribe({
      error: (error) => (receivedStatus = error.status),
    });

    httpTesting.expectOne('/api/auth/login').flush(
      { message: 'Invalid credentials' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(receivedStatus).toBe(401);
    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
