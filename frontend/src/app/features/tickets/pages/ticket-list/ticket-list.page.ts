import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  BehaviorSubject,
  catchError,
  forkJoin,
  map,
  of,
  startWith,
  switchMap,
} from 'rxjs';

import { PriorityBadgeComponent } from '../../../../core/components/priority-badge/priority-badge.component';
import { StatusBadgeComponent } from '../../../../core/components/status-badge/status-badge.component';
import { ProjectSummary } from '../../../projects/models/project.models';
import { ProjectService } from '../../../projects/services/project.service';
import { TicketSearchFilters, TicketSummary } from '../../models/ticket.models';
import { TicketService } from '../../services/ticket.service';

type TicketSortOption =
  | 'updatedDesc'
  | 'updatedAsc'
  | 'priorityDesc'
  | 'priorityAsc'
  | 'statusAsc'
  | 'titleAsc'
  | 'referenceAsc';

type TicketListVm = {
  loading: boolean;
  tickets: TicketSummary[];
  projects: ProjectSummary[];
  errorMessage: string;
};

@Component({
  selector: 'app-ticket-list-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    DatePipe,
    StatusBadgeComponent,
    PriorityBadgeComponent,
  ],
  templateUrl: './ticket-list.page.html',
  styleUrl: './ticket-list.page.scss',
})
export class TicketListPage {
  private readonly fb = inject(FormBuilder);
  private readonly ticketService = inject(TicketService);
  private readonly projectService = inject(ProjectService);
  private readonly reloadSubject = new BehaviorSubject<void>(undefined);

  readonly form = this.fb.nonNullable.group({
    q: [''],
    projectId: [0],
    status: [''],
    type: [''],
    unassigned: [false],
    sortBy: ['updatedDesc' as TicketSortOption],
  });

  readonly vm$ = this.reloadSubject.pipe(
    switchMap(() => {
      const raw = this.form.getRawValue();

      const filters: TicketSearchFilters = {
        q: raw.q.trim() || null,
        projectId: raw.projectId > 0 ? raw.projectId : null,
        status: raw.status || null,
        type: raw.type || null,
        unassigned: raw.unassigned || false,
      };

      return forkJoin({
        tickets: this.ticketService.searchTickets(filters),
        projects: this.projectService.getProjects(),
      }).pipe(
        map(({ tickets, projects }) => ({
          loading: false,
          tickets: this.sortTickets(tickets, raw.sortBy),
          projects,
          errorMessage: '',
        })),
        startWith({
          loading: true,
          tickets: [],
          projects: [],
          errorMessage: '',
        } as TicketListVm),
        catchError((error) =>
          of({
            loading: false,
            tickets: [],
            projects: [],
            errorMessage:
              error?.error?.message ||
              error?.message ||
              'Impossible de charger les tickets.',
          } as TicketListVm)
        )
      );
    })
  );

  applyFilters(): void {
    this.reloadSubject.next();
  }

  resetFilters(): void {
    this.form.reset({
      q: '',
      projectId: 0,
      status: '',
      type: '',
      unassigned: false,
      sortBy: 'updatedDesc',
    });

    this.reloadSubject.next();
  }

  trackByTicket(_: number, ticket: TicketSummary): number {
    return ticket.id;
  }

  private sortTickets(
    tickets: TicketSummary[],
    sortBy: TicketSortOption
  ): TicketSummary[] {
    const priorityOrder: Record<string, number> = {
      LOW: 1,
      MEDIUM: 2,
      HIGH: 3,
      CRITICAL: 4,
    };

    const statusOrder: Record<string, number> = {
      OPEN: 1,
      IN_PROGRESS: 2,
      DONE: 3,
      REVIEW: 4,
      BLOCKED: 5,
      CANCELLED: 6,
    };

    return [...tickets].sort((a, b) => {
      switch (sortBy) {
        case 'updatedAsc':
          return this.compareDates(a.updatedAt, b.updatedAt);

        case 'priorityDesc':
          return (priorityOrder[b.priority] ?? 0) - (priorityOrder[a.priority] ?? 0);

        case 'priorityAsc':
          return (priorityOrder[a.priority] ?? 0) - (priorityOrder[b.priority] ?? 0);

        case 'statusAsc':
          return (statusOrder[a.status] ?? 999) - (statusOrder[b.status] ?? 999);

        case 'titleAsc':
          return this.compareText(a.title, b.title);

        case 'referenceAsc':
          return this.compareText(a.reference, b.reference);

        case 'updatedDesc':
        default:
          return this.compareDates(b.updatedAt, a.updatedAt);
      }
    });
  }

  private compareDates(
    first?: string | null,
    second?: string | null
  ): number {
    const firstTime = first ? new Date(first).getTime() : 0;
    const secondTime = second ? new Date(second).getTime() : 0;
    return firstTime - secondTime;
  }

  private compareText(
    first?: string | null,
    second?: string | null
  ): number {
    return (first ?? '').localeCompare(second ?? '', 'fr', {
      sensitivity: 'base',
      numeric: true,
    });
  }
}