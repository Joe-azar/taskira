import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, finalize, forkJoin, map } from 'rxjs';

import { StatusBadgeComponent } from '../../../../core/components/status-badge/status-badge.component';
import { PriorityBadgeComponent } from '../../../../core/components/priority-badge/priority-badge.component';
import { UserOption } from '../../../../core/models/user.models';
import { UserService } from '../../../../core/services/user.service';
import { TicketService } from '../../../tickets/services/ticket.service';
import { TICKET_TYPES, TICKET_PRIORITIES } from '../../../tickets/ticket.constants';
import { TicketSummary, CreateTicketRequest } from '../../../../features/tickets/models/ticket.models';
import {
  AddProjectMemberRequest,
  ProjectDetail,
  ProjectMember,
  UpdateProjectRequest,
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
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    StatusBadgeComponent,
    PriorityBadgeComponent,
  ],
  templateUrl: './project-detail.page.html',
  styleUrl: './project-detail.page.scss',
})
export class ProjectDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly projectService = inject(ProjectService);
  private readonly userService = inject(UserService);
  private readonly ticketService = inject(TicketService);

  private readonly projectSubject = new BehaviorSubject<ProjectDetail | null>(null);
  readonly project$ = this.projectSubject.asObservable();

  private readonly membersSubject = new BehaviorSubject<ProjectMember[]>([]);
  readonly members$ = this.membersSubject.asObservable();

  private readonly availableUsersSubject = new BehaviorSubject<UserOption[]>([]);
  readonly availableUsers$ = this.availableUsersSubject.asObservable();

  private readonly ticketsSubject = new BehaviorSubject<TicketSummary[]>([]);
  readonly tickets$ = this.ticketsSubject.asObservable();

  private readonly loadingSubject = new BehaviorSubject<boolean>(true);
  readonly loading$ = this.loadingSubject.asObservable();

  private readonly errorSubject = new BehaviorSubject<string>('');
  readonly error$ = this.errorSubject.asObservable();

  savingMember = false;
  addMemberErrorMessage = '';
  memberSuccessMessage = '';

  savingTicket = false;
  createTicketErrorMessage = '';
  ticketSuccessMessage = '';

  removingMemberId: number | null = null;
  removeMemberErrorMessage = '';
  removeMemberSuccessMessage = '';

  archivingProject = false;
  archiveErrorMessage = '';
  archiveSuccessMessage = '';

  isEditMode = false;
  savingEdit = false;
  editErrorMessage = '';
  editSuccessMessage = '';

  readonly memberForm = this.fb.nonNullable.group({
    userId: [0, [Validators.required, Validators.min(1)]],
    projectRole: ['MEMBER', [Validators.required]],
  });

  readonly ticketForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    type: ['BUG', [Validators.required]],
    priority: ['MEDIUM', [Validators.required]],
    dueDate: [''],
  });

  readonly editForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    code: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(20)]],
    description: [''],
  });

  readonly ticketTypes = TICKET_TYPES;
  readonly ticketPriorities = TICKET_PRIORITIES;

  private readonly projectId$ = this.route.paramMap.pipe(
    map((params) => Number(params.get('id') ?? 0))
  );

  readonly vm$ = combineLatest([
    this.project$,
    this.members$,
    this.availableUsers$,
    this.tickets$,
    this.loading$,
    this.error$,
  ]).pipe(
    map(([project, members, availableUsers, tickets, loading, errorMessage]) => ({
      project,
      members,
      availableUsers,
      tickets,
      loading,
      errorMessage,
    } as ProjectDetailVm))
  );

  constructor() {
    this.projectId$.subscribe((projectId) => {
      if (projectId) {
        this.loadProject(projectId);
      } else {
        this.loadingSubject.next(false);
        this.errorSubject.next('Projet invalide.');
      }
    });
  }

  private loadProject(projectId: number): void {
    this.loadingSubject.next(true);
    this.errorSubject.next('');

    forkJoin({
      project: this.projectService.getProjectById(projectId),
      members: this.projectService.getProjectMembers(projectId),
      users: this.userService.getUsers(),
      tickets: this.ticketService.getProjectTickets(projectId),
    })
      .pipe(finalize(() => this.loadingSubject.next(false)))
      .subscribe({
        next: ({ project, members, users, tickets }) => {
          this.projectSubject.next(project);
          this.membersSubject.next(members);
          this.ticketsSubject.next(tickets);

          const memberIds = new Set(members.map((m) => m.userId));
          this.availableUsersSubject.next(users.filter((u) => !memberIds.has(u.id)));
        },
        error: (error) => {
          this.errorSubject.next(
            error?.error?.message || error?.message || 'Impossible de charger le projet.'
          );
        },
      });
  }


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
        next: (newMember) => {
          this.memberSuccessMessage = 'Membre ajouté avec succès.';
          this.membersSubject.next([...this.membersSubject.value, newMember]);

          const updatedProject = this.projectSubject.value
            ? { ...this.projectSubject.value, memberCount: this.projectSubject.value.memberCount + 1 }
            : null;

          if (updatedProject) {
            this.projectSubject.next(updatedProject);
          }

          this.availableUsersSubject.next(
            this.availableUsersSubject.value.filter((user) => user.id !== newMember.userId)
          );

          this.memberForm.reset({
            userId: 0,
            projectRole: 'MEMBER',
          });
        },
        error: (error) => {
          this.addMemberErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible d’ajouter le membre.';
        },
      });
  }

  removeMember(memberId: number): void {
    const projectId = Number(this.route.snapshot.paramMap.get('id') ?? 0);

    if (!projectId) {
      this.removeMemberErrorMessage = 'Projet invalide.';
      return;
    }

    const member = this.membersSubject.value.find((m) => m.userId === memberId);
    if (!member) {
      this.removeMemberErrorMessage = 'Membre introuvable.';
      return;
    }

    const project = this.projectSubject.value;
    if (!project) {
      this.removeMemberErrorMessage = 'Projet invalide.';
      return;
    }

    if (member.userId === project.ownerId) {
      this.removeMemberErrorMessage = 'Impossible de retirer le propriétaire du projet.';
      return;
    }

    if (!confirm(`Confirmez-vous la suppression de ${member.displayName} du projet ?`)) {
      return;
    }

    this.removingMemberId = memberId;
    this.removeMemberErrorMessage = '';
    this.removeMemberSuccessMessage = '';

    this.projectService.removeProjectMember(projectId, memberId)
      .pipe(finalize(() => (this.removingMemberId = null)))
      .subscribe({
        next: () => {
          this.removeMemberSuccessMessage = 'Membre retiré avec succès.';
          this.membersSubject.next(this.membersSubject.value.filter((m) => m.userId !== memberId));

          const removedMember = member;
          const userOption: UserOption = {
            id: removedMember.userId,
            displayName: removedMember.displayName,
            email: removedMember.email,
          };
          this.availableUsersSubject.next([...this.availableUsersSubject.value, userOption]);

          const updatedProject = this.projectSubject.value
            ? { ...this.projectSubject.value, memberCount: this.projectSubject.value.memberCount - 1 }
            : null;
          if (updatedProject) {
            this.projectSubject.next(updatedProject);
          }
        },
        error: (error) => {
          this.removeMemberErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible de retirer le membre.';
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
        next: (newTicket) => {
          this.ticketSuccessMessage = 'Ticket créé avec succès.';

          const newTicketSummary: TicketSummary = {
            id: newTicket.id,
            reference: newTicket.reference,
            title: newTicket.title,
            type: newTicket.type,
            status: newTicket.status,
            priority: newTicket.priority,
            assigneeFullName: newTicket.assigneeName ?? null,
            createdAt: newTicket.createdAt ?? null,
            updatedAt: newTicket.updatedAt ?? null,
          };

          this.ticketsSubject.next([...this.ticketsSubject.value, newTicketSummary]);
          this.ticketForm.reset({
            title: '',
            description: '',
            type: 'BUG',
            priority: 'HIGH',
            dueDate: '',
          });
        },
        error: (error) => {
          this.createTicketErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible de créer le ticket.';
        },
      });
  }

  startEdit(): void {
    const project = this.projectSubject.value;
    if (!project || project.status === 'ARCHIVED') {
      return;
    }

    this.isEditMode = true;
    this.editForm.patchValue({
      name: project.name,
      code: project.code,
      description: project.description || '',
    });
  }

  cancelEdit(): void {
    this.isEditMode = false;
    this.editErrorMessage = '';
    this.editSuccessMessage = '';
  }

  submitEdit(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const projectId = Number(this.route.snapshot.paramMap.get('id') ?? 0);
    if (!projectId) {
      this.editErrorMessage = 'Projet invalide.';
      return;
    }

    this.savingEdit = true;
    this.editErrorMessage = '';
    this.editSuccessMessage = '';

    const raw = this.editForm.getRawValue();

    const payload: UpdateProjectRequest = {
      name: raw.name.trim(),
      code: raw.code.trim().toUpperCase(),
      description: raw.description.trim() || null,
    };

    this.projectService
      .updateProject(projectId, payload)
      .pipe(finalize(() => (this.savingEdit = false)))
      .subscribe({
        next: (updatedProject) => {
          this.editSuccessMessage = 'Projet modifié avec succès.';
          this.projectSubject.next(updatedProject);
          this.isEditMode = false;
        },
        error: (error) => {
          this.editErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible de modifier le projet.';
        },
      });
  }

  archiveProject(): void {
    const project = this.projectSubject.value;
    if (!project) {
      return;
    }

    if (!confirm(`Êtes-vous sûr de vouloir archiver le projet "${project.name}" ? Cette action est irréversible.`)) {
      return;
    }

    const projectId = Number(this.route.snapshot.paramMap.get('id') ?? 0);
    if (!projectId) {
      return;
    }

    this.archivingProject = true;
    this.archiveErrorMessage = '';
    this.archiveSuccessMessage = '';

    this.projectService.archiveProject(projectId)
      .pipe(finalize(() => (this.archivingProject = false)))
      .subscribe({
        next: (updatedProject) => {
          this.projectSubject.next(updatedProject);
          this.archiveSuccessMessage = 'Projet archivé avec succès.';
        },
        error: (error) => {
          this.archiveErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible d\'archiver le projet.';
        },
      });
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

  get editName() {
    return this.editForm.controls.name;
  }

  get editCode() {
    return this.editForm.controls.code;
  }

  get editDescription() {
    return this.editForm.controls.description;
  }
}