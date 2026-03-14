import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { catchError, map, of, startWith } from 'rxjs';

import { DashboardSummary } from '../../../../core/models/dashboard.models';
import { DashboardService } from '../../../../core/services/dashboard.service';

type DashboardVm = {
  loading: boolean;
  summary: DashboardSummary | null;
  errorMessage: string;
};

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage {
  private readonly dashboardService = inject(DashboardService);

  readonly vm$ = this.dashboardService.getSummary().pipe(
    map((summary): DashboardVm => ({
      loading: false,
      summary,
      errorMessage: '',
    })),
    startWith({
      loading: true,
      summary: null,
      errorMessage: '',
    } as DashboardVm),
    catchError((error) =>
      of({
        loading: false,
        summary: null,
        errorMessage:
          error?.error?.message ||
          error?.message ||
          'Impossible de charger le dashboard.',
      } as DashboardVm)
    )
  );
}
