import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';

import { DashboardSummary } from '../../../../core/models/dashboard.models';
import { DashboardService } from '../../../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly cdr = inject(ChangeDetectorRef);

  loading = true;
  errorMessage = '';
  summary: DashboardSummary | null = null;

  ngOnInit(): void {
    this.dashboardService.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.errorMessage =
          error?.error?.message ||
          error?.message ||
          'Impossible de charger le dashboard.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }
}
