import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreateCommentRequest,
  TicketComment,
  UpdateCommentRequest,
} from '../models/comment.models';

@Injectable({
  providedIn: 'root',
})
export class CommentService {
  private readonly http = inject(HttpClient);

  getTicketComments(ticketId: number): Observable<TicketComment[]> {
    return this.http
      .get<any[]>(`${environment.apiUrl}/tickets/${ticketId}/comments`)
      .pipe(map((items) => (items ?? []).map((item) => this.normalizeComment(item))));
  }

  createComment(
    ticketId: number,
    payload: CreateCommentRequest
  ): Observable<TicketComment> {
    return this.http
      .post<any>(`${environment.apiUrl}/tickets/${ticketId}/comments`, payload)
      .pipe(map((item) => this.normalizeComment(item)));
  }

  updateComment(
    commentId: number,
    payload: UpdateCommentRequest
  ): Observable<TicketComment> {
    return this.http
      .put<any>(`${environment.apiUrl}/comments/${commentId}`, payload)
      .pipe(map((item) => this.normalizeComment(item)));
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/comments/${commentId}`);
  }

  private normalizeComment(raw: any): TicketComment {
    const author = raw?.author ?? raw?.createdBy ?? raw?.user ?? {};

    return {
      id: Number(raw?.id ?? 0),
      content: raw?.content ?? raw?.text ?? '',
      authorId: Number(author?.id ?? raw?.authorId ?? 0),
      authorName: author?.fullName ?? author?.displayName ?? author?.email ?? '—',
      authorEmail: author?.email ?? '',
      createdAt: raw?.createdAt ?? null,
      updatedAt: raw?.updatedAt ?? null,
    };
  }
}
