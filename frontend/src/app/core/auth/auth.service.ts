import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, map, switchMap, tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AuthUser, LoginRequest } from '../models/auth.models';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenService = inject(TokenService);

  private readonly currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  private readonly initializedSubject = new BehaviorSubject<boolean>(false);

  readonly currentUser$ = this.currentUserSubject.asObservable();
  readonly initialized$ = this.initializedSubject.asObservable();

  get currentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  hasToken(): boolean {
    return this.tokenService.hasToken();
  }

  login(payload: LoginRequest): Observable<AuthUser> {
    return this.http.post<any>(`${environment.apiUrl}/auth/login`, payload).pipe(
      map((response) => {
        const token = this.extractToken(response);
        if (!token) {
          throw new Error('Token JWT absent de la réponse de login');
        }
        this.tokenService.setToken(token);
        return response;
      }),
      switchMap((response) => {
        const embeddedUser = this.extractEmbeddedUser(response);
        if (embeddedUser) {
          this.currentUserSubject.next(embeddedUser);
          return of(embeddedUser);
        }
        return this.fetchMe();
      })
    );
  }

  fetchMe(): Observable<AuthUser> {
    return this.http.get<any>(`${environment.apiUrl}/auth/me`).pipe(
      map((raw) => this.normalizeUser(raw)),
      tap((user) => this.currentUserSubject.next(user))
    );
  }

  bootstrapSession(): void {
    if (!this.hasToken()) {
      this.initializedSubject.next(true);
      return;
    }

    this.fetchMe()
      .pipe(
        catchError(() => {
          this.clearSession();
          return of(null);
        }),
        finalize(() => this.initializedSubject.next(true))
      )
      .subscribe();
  }

  logout(redirectToLogin: boolean = true): void {
    this.clearSession();
    this.initializedSubject.next(true);

    if (redirectToLogin) {
      this.router.navigate(['/login']);
    }
  }

  private clearSession(): void {
    this.tokenService.clearToken();
    this.currentUserSubject.next(null);
  }

  private extractToken(response: any): string | null {
    return (
      response?.token ??
      response?.accessToken ??
      response?.jwt ??
      response?.data?.token ??
      null
    );
  }

  private extractEmbeddedUser(response: any): AuthUser | null {
    const candidate = response?.user ?? response?.me ?? null;
    return candidate ? this.normalizeUser(candidate) : null;
  }

  private normalizeUser(raw: any): AuthUser {
    const source = raw?.user ?? raw;

    const firstName = source?.firstName ?? source?.firstname ?? '';
    const lastName = source?.lastName ?? source?.lastname ?? '';
    const email = source?.email ?? '';
    const role = String(source?.role ?? source?.globalRole ?? 'USER');

    const displayName =
      source?.displayName ??
      source?.fullName ??
      [firstName, lastName].filter(Boolean).join(' ') ??
      email;

    return {
      id: Number(source?.id ?? 0),
      email,
      role,
      firstName,
      lastName,
      displayName: displayName || email || 'Utilisateur',
    };
  }
}
