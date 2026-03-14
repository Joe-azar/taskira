import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreateTicketRequest,
  TicketDetail,
  TicketHistoryEntry,
  TicketSummary,
  UpdateTicketAssigneeRequest,
  UpdateTicketStatusRequest,
} from '../models/ticket.models';

@Injectable({
  providedIn: 'root',
})
export class TicketService {
  private readonly http = inject(HttpClient);

  getProjectTickets(projectId: number): Observable<TicketSummary[]> {
    return this.http
      .get<any[]>(`${environment.apiUrl}/projects/${projectId}/tickets`)
      .pipe(map((items) => (items ?? []).map((item) => this.normalizeTicketSummary(item))));
  }

  getTicketById(ticketId: number): Observable<TicketDetail> {
    return this.http
      .get<any>(`${environment.apiUrl}/tickets/${ticketId}`)
      .pipe(map((item) => this.normalizeTicketDetail(item)));
  }

  getTicketHistory(ticketId: number): Observable<TicketHistoryEntry[]> {
    return this.http
      .get<any[]>(`${environment.apiUrl}/tickets/${ticketId}/history`)
      .pipe(map((items) => (items ?? []).map((item) => this.normalizeTicketHistory(item))));
  }

  createTicket(payload: CreateTicketRequest): Observable<TicketDetail> {
    return this.http
      .post<any>(`${environment.apiUrl}/tickets`, payload)
      .pipe(map((item) => this.normalizeTicketDetail(item)));
  }

  updateTicketStatus(
    ticketId: number,
    payload: UpdateTicketStatusRequest
  ): Observable<TicketDetail> {
    return this.http
      .patch<any>(`${environment.apiUrl}/tickets/${ticketId}/status`, payload)
      .pipe(map((item) => this.normalizeTicketDetail(item)));
  }

  updateTicketAssignee(
    ticketId: number,
    payload: UpdateTicketAssigneeRequest
  ): Observable<TicketDetail> {
    return this.http
      .patch<any>(`${environment.apiUrl}/tickets/${ticketId}/assignee`, payload)
      .pipe(map((item) => this.normalizeTicketDetail(item)));
  }

  private normalizeTicketSummary(raw: any): TicketSummary {
    return {
      id: Number(raw?.id ?? 0),
      reference: raw?.reference ?? '',
      title: raw?.title ?? '',
      type: raw?.type ?? '',
      status: raw?.status ?? '',
      priority: raw?.priority ?? '',
      assigneeFullName: raw?.assigneeFullName ?? null,
      createdAt: raw?.createdAt ?? null,
      updatedAt: raw?.updatedAt ?? null,
    };
  }

  private normalizeTicketDetail(raw: any): TicketDetail {
    const creator = raw?.creator ?? {};
    const assignee = raw?.assignee ?? null;

    return {
      id: Number(raw?.id ?? 0),
      reference: raw?.reference ?? '',
      projectId: Number(raw?.projectId ?? 0),
      projectCode: raw?.projectCode ?? '',
      projectName: raw?.projectName ?? '',
      title: raw?.title ?? '',
      description: raw?.description ?? '',
      type: raw?.type ?? '',
      status: raw?.status ?? '',
      priority: raw?.priority ?? '',
      creatorName: creator?.fullName ?? creator?.displayName ?? creator?.email ?? '—',
      creatorEmail: creator?.email ?? '',
      creatorGlobalRole: creator?.globalRole ?? '',
      assigneeName: assignee?.fullName ?? assignee?.displayName ?? assignee?.email ?? null,
      assigneeEmail: assignee?.email ?? null,
      dueDate: raw?.dueDate ?? null,
      createdAt: raw?.createdAt ?? null,
      updatedAt: raw?.updatedAt ?? null,
    };
  }

  private normalizeTicketHistory(raw: any): TicketHistoryEntry {
    const changedBy = raw?.changedBy ?? {};

    return {
      id: Number(raw?.id ?? 0),
      fieldName: raw?.fieldName ?? '',
      oldValue: raw?.oldValue ?? null,
      newValue: raw?.newValue ?? null,
      changedByName: changedBy?.fullName ?? changedBy?.displayName ?? changedBy?.email ?? '—',
      changedByEmail: changedBy?.email ?? '',
      changedAt: raw?.changedAt ?? null,
    };
  }
}
