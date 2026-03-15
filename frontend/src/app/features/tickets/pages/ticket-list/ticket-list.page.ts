import { CommonModule } from '@angular/common';
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

import { ProjectSummary } from '../../../projects/models/project.models';
import { ProjectService } from '../../../projects/services/project.service';
import { TicketSearchFilters, TicketSummary } from '../../models/ticket.models';
import { TicketService } from '../../services/ticket.service';

type TicketListVm = {
  loading: boolean;
  tickets: TicketSummary[];
  projects: ProjectSummary[];
  errorMessage: string;
};

@Component({
  selector: 'app-ticket-list-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
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
          tickets,
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
    });

    this.reloadSubject.next();
  }
}
