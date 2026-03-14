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

import { TicketDetail, TicketHistoryEntry, UpdateTicketStatusRequest } from '../../models/ticket.models';
import { TicketService } from '../../services/ticket.service';

type TicketDetailVm = {
  loading: boolean;
  ticket: TicketDetail | null;
  history: TicketHistoryEntry[];
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
  private readonly reloadSubject = new BehaviorSubject<void>(undefined);

  savingStatus = false;
  statusErrorMessage = '';
  statusSuccessMessage = '';

  readonly statusForm = this.fb.nonNullable.group({
    status: ['OPEN', [Validators.required]],
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
          errorMessage: 'Ticket invalide.',
        } as TicketDetailVm);
      }

      return forkJoin({
        ticket: this.ticketService.getTicketById(ticketId),
        history: this.ticketService.getTicketHistory(ticketId),
      }).pipe(
        tap(({ ticket }) => {
          this.statusForm.patchValue(
            { status: ticket.status || 'OPEN' },
            { emitEvent: false }
          );
        }),
        map(({ ticket, history }) => ({
          loading: false,
          ticket,
          history,
          errorMessage: '',
        })),
        startWith({
          loading: true,
          ticket: null,
          history: [],
          errorMessage: '',
        } as TicketDetailVm),
        catchError((error) =>
          of({
            loading: false,
            ticket: null,
            history: [],
            errorMessage:
              error?.error?.message ||
              error?.message ||
              'Impossible de charger le ticket.',
          } as TicketDetailVm)
        )
      );
    })
  );

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

  get status() {
    return this.statusForm.controls.status;
  }
}
