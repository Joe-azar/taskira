import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreateUserRequest,
  UpdateUserRequest,
  UpdateUserStatusRequest,
  UserDetail,
  UserSummary,
} from '../models/user.models';

@Injectable({
  providedIn: 'root',
})
export class UsersService {
  private readonly http = inject(HttpClient);

  getUsers(search?: string): Observable<UserSummary[]> {
    let params = new HttpParams();
    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http
      .get<any[]>(`${environment.apiUrl}/users`, { params })
      .pipe(map((items) => (items ?? []).map((item) => this.normalizeUser(item))));
  }

  getUserById(userId: number): Observable<UserDetail> {
    return this.http
      .get<any>(`${environment.apiUrl}/users/${userId}`)
      .pipe(map((item) => this.normalizeUser(item)));
  }

  createUser(payload: CreateUserRequest): Observable<UserSummary> {
    return this.http
      .post<any>(`${environment.apiUrl}/users`, payload)
      .pipe(map((item) => this.normalizeUser(item)));
  }

  updateUser(userId: number, payload: UpdateUserRequest): Observable<UserSummary> {
    return this.http
      .put<any>(`${environment.apiUrl}/users/${userId}`, payload)
      .pipe(map((item) => this.normalizeUser(item)));
  }

  updateUserStatus(userId: number, payload: UpdateUserStatusRequest): Observable<UserSummary> {
    return this.http
      .patch<any>(`${environment.apiUrl}/users/${userId}/status`, payload)
      .pipe(map((item) => this.normalizeUser(item)));
  }

  private normalizeUser(raw: any): UserSummary {
    const firstName = raw?.firstName ?? '';
    const lastName = raw?.lastName ?? '';
    const fullName = raw?.fullName ?? [firstName, lastName].filter(Boolean).join(' ') ?? raw?.email ?? '';
    return {
      id: Number(raw?.id ?? 0),
      firstName,
      lastName,
      fullName,
      email: raw?.email ?? '',
      globalRole: raw?.globalRole ?? raw?.role ?? 'USER',
      active: raw?.active ?? true,
      createdAt: raw?.createdAt ?? null,
      updatedAt: raw?.updatedAt ?? null,
    };
  }
}
