import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  finalize,
  forkJoin,
  map,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { ProjectMember } from '../../../projects/models/project.models';
import { ProjectService } from '../../../projects/services/project.service';
import {
  TicketComment,
  CreateCommentRequest,
  UpdateCommentRequest,
} from '../../../comments/models/comment.models';
import { CommentService } from '../../../comments/services/comment.service';
import {
  TicketDetail,
  TicketHistoryEntry,
  UpdateTicketAssigneeRequest,
  UpdateTicketStatusRequest,
} from '../../models/ticket.models';
import { TicketService } from '../../services/ticket.service';

type TicketDetailVm = {
  loading: boolean;
  ticket: TicketDetail | null;
  history: TicketHistoryEntry[];
  comments: TicketComment[];
  projectMembers: ProjectMember[];
  errorMessage: string;
};

@Component({
  selector: 'app-ticket-detail-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './ticket-detail.page.html',
  styleUrl: './ticket-detail.page.scss',
})
export class TicketDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly ticketService = inject(TicketService);
  private readonly projectService = inject(ProjectService);
  private readonly commentService = inject(CommentService);
  readonly authService = inject(AuthService);
  private readonly reloadSubject = new BehaviorSubject<void>(undefined);

  savingStatus = false;
  statusErrorMessage = '';
  statusSuccessMessage = '';

  savingAssignee = false;
  assigneeErrorMessage = '';
  assigneeSuccessMessage = '';

  savingComment = false;
  commentErrorMessage = '';
  commentSuccessMessage = '';

  savingEditedComment = false;
  editCommentErrorMessage = '';
  editCommentSuccessMessage = '';

  editingCommentId: number | null = null;

  readonly statusForm = this.fb.nonNullable.group({
    status: ['OPEN', [Validators.required]],
  });

  readonly assigneeForm = this.fb.nonNullable.group({
    assigneeId: [0],
  });

  readonly commentForm = this.fb.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(2)]],
  });

  readonly editCommentForm = this.fb.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(2)]],
  });

  private readonly ticketId$ = this.route.paramMap.pipe(
    map((params) => Number(params.get('id') ?? 0))
  );

  readonly vm$ = combineLatest([this.ticketId$, this.reloadSubject]).pipe(
    switchMap(([ticketId]) => {
      if (!ticketId) {
        return of({
          loading: false,
          ticket: null,
          history: [],
          comments: [],
          projectMembers: [],
          errorMessage: 'Ticket invalide.',
        } as TicketDetailVm);
      }

      return this.ticketService.getTicketById(ticketId).pipe(
        switchMap((ticket) =>
          forkJoin({
            ticket: of(ticket),
            history: this.ticketService.getTicketHistory(ticketId),
            comments: this.commentService.getTicketComments(ticketId),
            projectMembers: this.projectService.getProjectMembers(ticket.projectId),
          })
        ),
        tap(({ ticket, projectMembers }) => {
          this.statusForm.patchValue(
            { status: ticket.status || 'OPEN' },
            { emitEvent: false }
          );

          const currentAssignee = projectMembers.find(
            (member) =>
              (ticket.assigneeEmail && member.email === ticket.assigneeEmail) ||
              (!ticket.assigneeEmail &&
                ticket.assigneeName &&
                member.displayName === ticket.assigneeName)
          );

          this.assigneeForm.patchValue(
            { assigneeId: currentAssignee?.userId ?? 0 },
            { emitEvent: false }
          );
        }),
        map(({ ticket, history, comments, projectMembers }) => ({
          loading: false,
          ticket,
          history,
          comments,
          projectMembers,
          errorMessage: '',
        })),
        startWith({
          loading: true,
          ticket: null,
          history: [],
          comments: [],
          projectMembers: [],
          errorMessage: '',
        } as TicketDetailVm),
        catchError((error) =>
          of({
            loading: false,
            ticket: null,
            history: [],
            comments: [],
            projectMembers: [],
            errorMessage:
              error?.error?.message ||
              error?.message ||
              'Impossible de charger le ticket.',
          } as TicketDetailVm)
        )
      );
    })
  );

  get currentUserId(): number | null {
    return this.authService.currentUser?.id ?? null;
  }

  canManageComment(comment: TicketComment): boolean {
    return this.currentUserId === comment.authorId;
  }

  submitStatus(): void {
    if (this.statusForm.invalid) {
      this.statusForm.markAllAsTouched();
      return;
    }

    const ticketId = Number(this.route.snapshot.paramMap.get('id') ?? 0);
    if (!ticketId) {
      this.statusErrorMessage = 'Ticket invalide.';
      return;
    }

    this.savingStatus = true;
    this.statusErrorMessage = '';
    this.statusSuccessMessage = '';

    const payload: UpdateTicketStatusRequest = {
      status: this.statusForm.getRawValue().status,
    };

    this.ticketService
      .updateTicketStatus(ticketId, payload)
      .pipe(finalize(() => (this.savingStatus = false)))
      .subscribe({
        next: () => {
          this.statusSuccessMessage = 'Statut mis à jour avec succès.';
          this.reloadSubject.next();
        },
        error: (error) => {
          this.statusErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible de mettre à jour le statut.';
        },
      });
  }

  submitAssignee(): void {
    const ticketId = Number(this.route.snapshot.paramMap.get('id') ?? 0);
    if (!ticketId) {
      this.assigneeErrorMessage = 'Ticket invalide.';
      return;
    }

    this.savingAssignee = true;
    this.assigneeErrorMessage = '';
    this.assigneeSuccessMessage = '';

    const rawAssigneeId = Number(this.assigneeForm.getRawValue().assigneeId);

    const payload: UpdateTicketAssigneeRequest = {
      assigneeId: rawAssigneeId > 0 ? rawAssigneeId : null,
    };

    this.ticketService
      .updateTicketAssignee(ticketId, payload)
      .pipe(finalize(() => (this.savingAssignee = false)))
      .subscribe({
        next: () => {
          this.assigneeSuccessMessage = 'Assignation mise à jour avec succès.';
          this.reloadSubject.next();
        },
        error: (error) => {
          this.assigneeErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible de mettre à jour l’assignation.';
        },
      });
  }

  submitComment(): void {
    if (this.commentForm.invalid) {
      this.commentForm.markAllAsTouched();
      return;
    }

    const ticketId = Number(this.route.snapshot.paramMap.get('id') ?? 0);
    if (!ticketId) {
      this.commentErrorMessage = 'Ticket invalide.';
      return;
    }

    this.savingComment = true;
    this.commentErrorMessage = '';
    this.commentSuccessMessage = '';

    const payload: CreateCommentRequest = {
      content: this.commentForm.getRawValue().content.trim(),
    };

    this.commentService
      .createComment(ticketId, payload)
      .pipe(finalize(() => (this.savingComment = false)))
      .subscribe({
        next: () => {
          this.commentSuccessMessage = 'Commentaire ajouté avec succès.';
          this.commentForm.reset({ content: '' });
          this.reloadSubject.next();
        },
        error: (error) => {
          this.commentErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible d’ajouter le commentaire.';
        },
      });
  }

  startEditComment(comment: TicketComment): void {
    this.editingCommentId = comment.id;
    this.editCommentErrorMessage = '';
    this.editCommentSuccessMessage = '';
    this.editCommentForm.reset({
      content: comment.content,
    });
  }

  cancelEditComment(): void {
    this.editingCommentId = null;
    this.editCommentErrorMessage = '';
    this.editCommentSuccessMessage = '';
    this.editCommentForm.reset({
      content: '',
    });
  }

  submitEditComment(commentId: number): void {
    if (this.editCommentForm.invalid) {
      this.editCommentForm.markAllAsTouched();
      return;
    }

    this.savingEditedComment = true;
    this.editCommentErrorMessage = '';
    this.editCommentSuccessMessage = '';

    const payload: UpdateCommentRequest = {
      content: this.editCommentForm.getRawValue().content.trim(),
    };

    this.commentService
      .updateComment(commentId, payload)
      .pipe(finalize(() => (this.savingEditedComment = false)))
      .subscribe({
        next: () => {
          this.editCommentSuccessMessage = 'Commentaire modifié avec succès.';
          this.editingCommentId = null;
          this.reloadSubject.next();
        },
        error: (error) => {
          this.editCommentErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible de modifier le commentaire.';
        },
      });
  }

  deleteComment(commentId: number): void {
    const confirmed = window.confirm('Supprimer ce commentaire ?');
    if (!confirmed) {
      return;
    }

    this.commentService.deleteComment(commentId).subscribe({
      next: () => {
        this.reloadSubject.next();
      },
      error: (error) => {
        this.commentErrorMessage =
          error?.error?.message ||
          error?.message ||
          'Impossible de supprimer le commentaire.';
      },
    });
  }

  get status() {
    return this.statusForm.controls.status;
  }

  get commentContent() {
    return this.commentForm.controls.content;
  }

  get editCommentContent() {
    return this.editCommentForm.controls.content;
  }
}
