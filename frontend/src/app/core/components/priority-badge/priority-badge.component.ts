import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-priority-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="priority-badge" [ngClass]="badgeClass">
      {{ label }}
    </span>
  `,
  styles: [
    `
      .priority-badge {
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

      .priority-badge--low {
        background: #ecfdf5;
        color: #047857;
      }

      .priority-badge--medium {
        background: #fffbeb;
        color: #b45309;
      }

      .priority-badge--high {
        background: #fff7ed;
        color: #c2410c;
      }

      .priority-badge--critical {
        background: #fef2f2;
        color: #b91c1c;
      }

      .priority-badge--default {
        background: #f3f4f6;
        color: #374151;
      }
    `,
  ],
})
export class PriorityBadgeComponent {
  @Input() value = '';

  get label(): string {
    switch (this.value) {
      case 'LOW':
        return 'Low';
      case 'MEDIUM':
        return 'Medium';
      case 'HIGH':
        return 'High';
      case 'CRITICAL':
        return 'Critical';
      default:
        return this.value || '—';
    }
  }

  get badgeClass(): string {
    switch (this.value) {
      case 'LOW':
        return 'priority-badge--low';
      case 'MEDIUM':
        return 'priority-badge--medium';
      case 'HIGH':
        return 'priority-badge--high';
      case 'CRITICAL':
        return 'priority-badge--critical';
      default:
        return 'priority-badge--default';
    }
  }
}