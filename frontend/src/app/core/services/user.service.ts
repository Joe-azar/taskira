import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { UserOption } from '../models/user.models';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient);

  getUsers(): Observable<UserOption[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/users`).pipe(
      map((items) => (items ?? []).map((item) => this.normalizeUser(item)))
    );
  }

  private normalizeUser(raw: any): UserOption {
    const firstName = raw?.firstName ?? raw?.firstname ?? '';
    const lastName = raw?.lastName ?? raw?.lastname ?? '';
    const email = raw?.email ?? '';

    const displayName =
      raw?.displayName ??
      raw?.fullName ??
      [firstName, lastName].filter(Boolean).join(' ') ??
      email;

    return {
      id: Number(raw?.id ?? 0),
      email,
      displayName: displayName || email || 'Utilisateur',
      role: raw?.role ?? raw?.globalRole ?? '',
      active: raw?.active ?? true,
    };
  }
}
