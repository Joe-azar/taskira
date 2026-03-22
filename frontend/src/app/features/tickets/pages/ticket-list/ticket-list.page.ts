import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, forkJoin, map } from 'rxjs';

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
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
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

  private readonly ticketsSubject = new BehaviorSubject<TicketSummary[]>([]);
  readonly tickets$ = this.ticketsSubject.asObservable();

  private readonly projectsSubject = new BehaviorSubject<ProjectSummary[]>([]);
  readonly projects$ = this.projectsSubject.asObservable();

  private readonly loadingSubject = new BehaviorSubject<boolean>(true);
  readonly loading$ = this.loadingSubject.asObservable();

  private readonly errorSubject = new BehaviorSubject<string>('');
  readonly error$ = this.errorSubject.asObservable();

  page = 0;
  size = 20;
  totalPages = 0;
  totalElements = 0;

  readonly form = this.fb.nonNullable.group({
    q: [''],
    projectId: [0],
    status: [''],
    type: [''],
    unassigned: [false],
    sortBy: ['updatedDesc' as TicketSortOption],
  });

  readonly vm$ = combineLatest([
    this.tickets$,
    this.projects$,
    this.loading$,
    this.error$,
  ]).pipe(
    map(([tickets, projects, loading, errorMessage]) => ({
      loading,
      tickets,
      projects,
      errorMessage,
      page: this.page,
      size: this.size,
      totalPages: this.totalPages,
      totalElements: this.totalElements,
    } as TicketListVm))
  );

  constructor() {
    this.loadTickets();
  }

  private loadTickets(): void {
    const raw = this.form.getRawValue();

    const filters: TicketSearchFilters = {
      q: raw.q.trim() || null,
      projectId: raw.projectId > 0 ? raw.projectId : null,
      status: raw.status || null,
      type: raw.type || null,
      unassigned: raw.unassigned || false,
    };

    this.loadingSubject.next(true);
    this.errorSubject.next('');

    forkJoin({
      ticketsPage: this.ticketService.searchTicketsPage(filters, this.page, this.size, this.toBackendSort(raw.sortBy)),
      projects: this.projectService.getProjects(),
    }).subscribe({
      next: ({ ticketsPage, projects }) => {
        this.ticketsSubject.next(ticketsPage.content);
        this.totalPages = ticketsPage.totalPages;
        this.totalElements = ticketsPage.totalElements;
        this.projectsSubject.next(projects);
        this.loadingSubject.next(false);
      },
      error: (error) => {
        this.errorSubject.next(
          error?.error?.message || error?.message || 'Impossible de charger les tickets.'
        );
        this.loadingSubject.next(false);
      },
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.loadTickets();
  }

  changePage(page: number): void {
    if (page < 0 || page >= this.totalPages) {
      return;
    }
    this.page = page;
    this.loadTickets();
  }

  resetFilters(): void {
    this.page = 0;
    this.form.reset({
      q: '',
      projectId: 0,
      status: '',
      type: '',
      unassigned: false,
      sortBy: 'updatedDesc',
    });

    this.loadTickets();
  }

  trackByTicket(_: number, ticket: TicketSummary): number {
    return ticket.id;
  }



  private toBackendSort(sort: TicketSortOption): string {
    switch (sort) {
      case 'updatedAsc':
        return 'updatedAt,asc';
      case 'updatedDesc':
        return 'updatedAt,desc';
      case 'priorityAsc':
        return 'priority,asc';
      case 'priorityDesc':
        return 'priority,desc';
      case 'statusAsc':
        return 'status,asc';
      case 'titleAsc':
        return 'title,asc';
      case 'referenceAsc':
        return 'reference,asc';
      default:
        return 'updatedAt,desc';
    }
  }
}