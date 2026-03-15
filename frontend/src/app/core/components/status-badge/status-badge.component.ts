import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="status-badge" [ngClass]="badgeClass">
      {{ label }}
    </span>
  `,
  styles: [
    `
      .status-badge {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border-radius: 999px;
        padding: 0.38rem 0.78rem;
        font-size: 0.82rem;
        font-weight: 700;
        line-height: 1;
        white-space: nowrap;
      }

      .status-badge--open {
        background: #eff6ff;
        color: #1d4ed8;
      }

      .status-badge--in-progress {
        background: #fff7ed;
        color: #c2410c;
      }

      .status-badge--done {
        background: #ecfdf5;
        color: #047857;
      }

      .status-badge--active {
        background: #ecfeff;
        color: #0f766e;
      }

      .status-badge--archived {
        background: #f3f4f6;
        color: #4b5563;
      }

      .status-badge--default {
        background: #f3f4f6;
        color: #374151;
      }
    `,
  ],
})
export class StatusBadgeComponent {
  @Input() value = '';

  get label(): string {
    switch (this.value) {
      case 'OPEN':
        return 'Open';
      case 'IN_PROGRESS':
        return 'In progress';
      case 'DONE':
        return 'Done';
      case 'ACTIVE':
        return 'Active';
      case 'ARCHIVED':
        return 'Archived';
      default:
        return this.value || '—';
    }
  }

  get badgeClass(): string {
    switch (this.value) {
      case 'OPEN':
        return 'status-badge--open';
      case 'IN_PROGRESS':
        return 'status-badge--in-progress';
      case 'DONE':
        return 'status-badge--done';
      case 'ACTIVE':
        return 'status-badge--active';
      case 'ARCHIVED':
        return 'status-badge--archived';
      default:
        return 'status-badge--default';
    }
  }
}