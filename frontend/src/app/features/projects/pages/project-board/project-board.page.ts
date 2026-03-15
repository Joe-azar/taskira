import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, startWith, switchMap } from 'rxjs';

import { TicketSummary } from '../../../tickets/models/ticket.models';
import { TicketService } from '../../../tickets/services/ticket.service';
import { ProjectDetail } from '../../models/project.models';
import { ProjectService } from '../../services/project.service';

type BoardColumnKey = 'OPEN' | 'IN_PROGRESS' | 'DONE';

type BoardColumn = {
  key: BoardColumnKey;
  label: string;
  tickets: TicketSummary[];
};

type ProjectBoardVm = {
  loading: boolean;
  project: ProjectDetail | null;
  columns: BoardColumn[];
  totalTickets: number;
  errorMessage: string;
};

@Component({
  selector: 'app-project-board-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './project-board.page.html',
  styleUrl: './project-board.page.scss',
})
export class ProjectBoardPage {
  private readonly route = inject(ActivatedRoute);
  private readonly projectService = inject(ProjectService);
  private readonly ticketService = inject(TicketService);

  private readonly projectId$ = this.route.paramMap.pipe(
    map((params) => Number(params.get('id') ?? 0))
  );

  readonly vm$ = this.projectId$.pipe(
    switchMap((projectId) => {
      if (!projectId) {
        return of({
          loading: false,
          project: null,
          columns: this.buildColumns([]),
          totalTickets: 0,
          errorMessage: 'Projet invalide.',
        } as ProjectBoardVm);
      }

      return forkJoin({
        project: this.projectService.getProjectById(projectId),
        tickets: this.ticketService.getProjectTickets(projectId),
      }).pipe(
        map(({ project, tickets }) => ({
          loading: false,
          project,
          columns: this.buildColumns(tickets),
          totalTickets: tickets.length,
          errorMessage: '',
        })),
        startWith({
          loading: true,
          project: null,
          columns: this.buildColumns([]),
          totalTickets: 0,
          errorMessage: '',
        } as ProjectBoardVm),
        catchError((error) =>
          of({
            loading: false,
            project: null,
            columns: this.buildColumns([]),
            totalTickets: 0,
            errorMessage:
              error?.error?.message ||
              error?.message ||
              'Impossible de charger le board du projet.',
          } as ProjectBoardVm)
        )
      );
    })
  );

  trackByColumn(_: number, column: BoardColumn): string {
    return column.key;
  }

  trackByTicket(_: number, ticket: TicketSummary): number {
    return ticket.id;
  }

  formatDate(value?: string | null): string {
    if (!value) {
      return '—';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return '—';
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    }).format(date);
  }

  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'LOW':
        return 'badge--priority-low';
      case 'MEDIUM':
        return 'badge--priority-medium';
      case 'HIGH':
        return 'badge--priority-high';
      case 'CRITICAL':
        return 'badge--priority-critical';
      default:
        return '';
    }
  }

  private buildColumns(tickets: TicketSummary[]): BoardColumn[] {
    return [
      {
        key: 'OPEN',
        label: 'Open',
        tickets: tickets.filter((ticket) => ticket.status === 'OPEN'),
      },
      {
        key: 'IN_PROGRESS',
        label: 'In progress',
        tickets: tickets.filter((ticket) => ticket.status === 'IN_PROGRESS'),
      },
      {
        key: 'DONE',
        label: 'Done',
        tickets: tickets.filter((ticket) => ticket.status === 'DONE'),
      },
    ];
  }
}
