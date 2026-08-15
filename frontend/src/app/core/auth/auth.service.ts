import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, map, switchMap, tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AuthUser, LoginRequest } from '../models/auth.models';

interface MeResponse {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  globalRole: string;
  active: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  private readonly initializedSubject = new BehaviorSubject<boolean>(false);

  readonly currentUser$ = this.currentUserSubject.asObservable();
  readonly initialized$ = this.initializedSubject.asObservable();

  get currentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  login(payload: LoginRequest): Observable<AuthUser> {
    return this.http
      .post(`${environment.apiUrl}/auth/login`, payload)
      .pipe(switchMap(() => this.fetchMe()));
  }

  fetchMe(): Observable<AuthUser> {
    return this.http.get<MeResponse>(`${environment.apiUrl}/auth/me`).pipe(
      map((raw) => this.normalizeUser(raw)),
      tap((user) => this.currentUserSubject.next(user))
    );
  }

  /**
   * There is no client-readable signal for an HttpOnly session cookie, so the only way to
   * know whether one exists is to ask the server. Wired into an APP_INITIALIZER so it
   * completes (and, as a side effect, seeds the XSRF-TOKEN cookie) before any route -
   * including /login - ever renders. Without that, a fast form submission could race the
   * cookie and get rejected as a missing CSRF token. Guards still fall back to their own
   * fetchMe() call for defense in depth, but shouldn't normally need to.
   */
  bootstrapSession(): Observable<AuthUser | null> {
    return this.fetchMe().pipe(
      catchError(() => {
        this.currentUserSubject.next(null);
        return of(null);
      }),
      finalize(() => this.initializedSubject.next(true))
    );
  }

  logout(redirectToLogin: boolean = true): void {
    this.http
      .post(`${environment.apiUrl}/auth/logout`, {})
      .pipe(
        catchError(() => of(null)),
        finalize(() => {
          this.currentUserSubject.next(null);
          if (redirectToLogin) {
            this.router.navigate(['/login']);
          }
        })
      )
      .subscribe();
  }

  private normalizeUser(raw: MeResponse): AuthUser {
    return {
      id: raw.id,
      email: raw.email,
      role: raw.globalRole,
      firstName: raw.firstName,
      lastName: raw.lastName,
      displayName: raw.fullName || raw.email,
    };
  }
}
