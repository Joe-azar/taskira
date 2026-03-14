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
} from 'rxjs';

import { UserOption } from '../../../../core/models/user.models';
import { UserService } from '../../../../core/services/user.service';
import { TicketService } from '../../../../features/tickets/services/ticket.service';
import { TicketSummary, CreateTicketRequest } from '../../../../features/tickets/models/ticket.models';
import {
  AddProjectMemberRequest,
  ProjectDetail,
  ProjectMember,
} from '../../models/project.models';
import { ProjectService } from '../../services/project.service';

type ProjectDetailVm = {
  loading: boolean;
  project: ProjectDetail | null;
  members: ProjectMember[];
  availableUsers: UserOption[];
  tickets: TicketSummary[];
  errorMessage: string;
};

@Component({
  selector: 'app-project-detail-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './project-detail.page.html',
  styleUrl: './project-detail.page.scss',
})
export class ProjectDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly projectService = inject(ProjectService);
  private readonly userService = inject(UserService);
  private readonly ticketService = inject(TicketService);
  private readonly reloadSubject = new BehaviorSubject<void>(undefined);

  savingMember = false;
  addMemberErrorMessage = '';
  memberSuccessMessage = '';

  savingTicket = false;
  createTicketErrorMessage = '';
  ticketSuccessMessage = '';

  readonly memberForm = this.fb.nonNullable.group({
    userId: [0, [Validators.required, Validators.min(1)]],
    projectRole: ['MEMBER', [Validators.required]],
  });

  readonly ticketForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(5)]],
    type: ['BUG', [Validators.required]],
    priority: ['HIGH', [Validators.required]],
    dueDate: [''],
  });

  private readonly projectId$ = this.route.paramMap.pipe(
    map((params) => Number(params.get('id') ?? 0))
  );

  readonly vm$ = combineLatest([this.projectId$, this.reloadSubject]).pipe(
    switchMap(([projectId]) => {
      if (!projectId) {
        return of({
          loading: false,
          project: null,
          members: [],
          availableUsers: [],
          tickets: [],
          errorMessage: 'Projet invalide.',
        } as ProjectDetailVm);
      }

      return forkJoin({
        project: this.projectService.getProjectById(projectId),
        members: this.projectService.getProjectMembers(projectId),
        users: this.userService.getUsers(),
        tickets: this.ticketService.getProjectTickets(projectId),
      }).pipe(
        map(({ project, members, users, tickets }) => {
          const memberIds = new Set(members.map((m) => m.userId));
          const availableUsers = users.filter((u) => !memberIds.has(u.id));

          return {
            loading: false,
            project,
            members,
            availableUsers,
            tickets,
            errorMessage: '',
          } as ProjectDetailVm;
        }),
        startWith({
          loading: true,
          project: null,
          members: [],
          availableUsers: [],
          tickets: [],
          errorMessage: '',
        } as ProjectDetailVm),
        catchError((error) =>
          of({
            loading: false,
            project: null,
            members: [],
            availableUsers: [],
            tickets: [],
            errorMessage:
              error?.error?.message ||
              error?.message ||
              'Impossible de charger le projet.',
          } as ProjectDetailVm)
        )
      );
    })
  );

  submitMember(): void {
    if (this.memberForm.invalid) {
      this.memberForm.markAllAsTouched();
      return;
    }

    const projectId = Number(this.route.snapshot.paramMap.get('id') ?? 0);
    if (!projectId) {
      this.addMemberErrorMessage = 'Projet invalide.';
      return;
    }

    this.savingMember = true;
    this.addMemberErrorMessage = '';
    this.memberSuccessMessage = '';

    const raw = this.memberForm.getRawValue();

    const payload: AddProjectMemberRequest = {
      userId: Number(raw.userId),
      projectRole: raw.projectRole,
    };

    this.projectService
      .addProjectMember(projectId, payload)
      .pipe(finalize(() => (this.savingMember = false)))
      .subscribe({
        next: () => {
          this.memberSuccessMessage = 'Membre ajouté avec succès.';
          this.memberForm.reset({
            userId: 0,
            projectRole: 'MEMBER',
          });
          this.reloadSubject.next();
        },
        error: (error) => {
          this.addMemberErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible d’ajouter le membre.';
        },
      });
  }

  submitTicket(): void {
    if (this.ticketForm.invalid) {
      this.ticketForm.markAllAsTouched();
      return;
    }

    const projectId = Number(this.route.snapshot.paramMap.get('id') ?? 0);
    if (!projectId) {
      this.createTicketErrorMessage = 'Projet invalide.';
      return;
    }

    this.savingTicket = true;
    this.createTicketErrorMessage = '';
    this.ticketSuccessMessage = '';

    const raw = this.ticketForm.getRawValue();

    const payload: CreateTicketRequest = {
      projectId,
      title: raw.title.trim(),
      description: raw.description.trim(),
      type: raw.type,
      priority: raw.priority,
      dueDate: raw.dueDate ? raw.dueDate : null,
    };

    this.ticketService
      .createTicket(payload)
      .pipe(finalize(() => (this.savingTicket = false)))
      .subscribe({
        next: () => {
          this.ticketSuccessMessage = 'Ticket créé avec succès.';
          this.ticketForm.reset({
            title: '',
            description: '',
            type: 'BUG',
            priority: 'HIGH',
            dueDate: '',
          });
          this.reloadSubject.next();
        },
        error: (error) => {
          this.createTicketErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible de créer le ticket.';
        },
      });
  }

  get userId() {
    return this.memberForm.controls.userId;
  }

  get projectRole() {
    return this.memberForm.controls.projectRole;
  }

  get ticketTitle() {
    return this.ticketForm.controls.title;
  }

  get ticketDescription() {
    return this.ticketForm.controls.description;
  }
}
