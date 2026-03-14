import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  AddProjectMemberRequest,
  CreateProjectRequest,
  ProjectDetail,
  ProjectMember,
  ProjectSummary,
} from '../models/project.models';

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private readonly http = inject(HttpClient);

  getProjects(): Observable<ProjectSummary[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/projects`).pipe(
      map((items) => (items ?? []).map((item) => this.normalizeProjectSummary(item)))
    );
  }

  getProjectById(projectId: number): Observable<ProjectDetail> {
    return this.http.get<any>(`${environment.apiUrl}/projects/${projectId}`).pipe(
      map((item) => this.normalizeProjectDetail(item))
    );
  }

  getProjectMembers(projectId: number): Observable<ProjectMember[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/projects/${projectId}/members`).pipe(
      map((items) => (items ?? []).map((item) => this.normalizeProjectMember(item)))
    );
  }

  createProject(payload: CreateProjectRequest): Observable<ProjectSummary> {
    return this.http.post<any>(`${environment.apiUrl}/projects`, payload).pipe(
      map((item) => this.normalizeProjectSummary(item))
    );
  }

  addProjectMember(
    projectId: number,
    payload: AddProjectMemberRequest
  ): Observable<ProjectMember> {
    return this.http
      .post<any>(`${environment.apiUrl}/projects/${projectId}/members`, payload)
      .pipe(map((item) => this.normalizeProjectMember(item)));
  }

  private normalizeProjectSummary(raw: any): ProjectSummary {
    return {
      id: Number(raw?.id ?? 0),
      code: raw?.code ?? '',
      name: raw?.name ?? '',
      description: raw?.description ?? '',
      status: raw?.status ?? '',
      createdAt: raw?.createdAt ?? null,
    };
  }

  private normalizeProjectDetail(raw: any): ProjectDetail {
    const owner = raw?.owner ?? {};

    const ownerName =
      owner?.displayName ??
      owner?.fullName ??
      [owner?.firstName ?? '', owner?.lastName ?? ''].filter(Boolean).join(' ') ??
      owner?.email ??
      '—';

    return {
      id: Number(raw?.id ?? 0),
      code: raw?.code ?? '',
      name: raw?.name ?? '',
      description: raw?.description ?? '',
      status: raw?.status ?? '',
      ownerName,
      ownerEmail: owner?.email ?? '',
      memberCount: Number(raw?.memberCount ?? 0),
      createdAt: raw?.createdAt ?? null,
      updatedAt: raw?.updatedAt ?? null,
    };
  }

  private normalizeProjectMember(raw: any): ProjectMember {
    const user = raw?.user ?? raw;
    const firstName = user?.firstName ?? user?.firstname ?? '';
    const lastName = user?.lastName ?? user?.lastname ?? '';
    const email = user?.email ?? raw?.email ?? '';

    const displayName =
      user?.displayName ??
      user?.fullName ??
      [firstName, lastName].filter(Boolean).join(' ') ??
      email;

    return {
      userId: Number(user?.id ?? raw?.userId ?? raw?.id ?? 0),
      displayName: displayName || email || 'Utilisateur',
      email,
      projectRole: raw?.projectRole ?? raw?.role ?? '',
      globalRole: user?.globalRole ?? user?.role ?? raw?.globalRole ?? '',
      joinedAt: raw?.joinedAt ?? null,
    };
  }
}
