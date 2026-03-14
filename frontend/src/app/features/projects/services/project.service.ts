import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { CreateProjectRequest, ProjectSummary } from '../models/project.models';

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private readonly http = inject(HttpClient);

  getProjects(): Observable<ProjectSummary[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/projects`).pipe(
      map((items) => (items ?? []).map((item) => this.normalizeProject(item)))
    );
  }

  createProject(payload: CreateProjectRequest): Observable<ProjectSummary> {
    return this.http.post<any>(`${environment.apiUrl}/projects`, payload).pipe(
      map((item) => this.normalizeProject(item))
    );
  }

  private normalizeProject(raw: any): ProjectSummary {
    return {
      id: Number(raw?.id ?? 0),
      name: raw?.name ?? raw?.title ?? '',
      key: raw?.key ?? raw?.projectKey ?? raw?.code ?? '',
      description: raw?.description ?? '',
      createdAt: raw?.createdAt ?? null,
    };
  }
}
