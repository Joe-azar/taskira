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
  private readonly reloadSubject = new BehaviorSubject<void>(undefined);

  saving = false;
  addMemberErrorMessage = '';
  successMessage = '';

  readonly form = this.fb.nonNullable.group({
    userId: [0, [Validators.required, Validators.min(1)]],
    projectRole: ['MEMBER', [Validators.required]],
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
          errorMessage: 'Projet invalide.',
        } as ProjectDetailVm);
      }

      return forkJoin({
        project: this.projectService.getProjectById(projectId),
        members: this.projectService.getProjectMembers(projectId),
        users: this.userService.getUsers(),
      }).pipe(
        map(({ project, members, users }) => {
          const memberIds = new Set(members.map((m) => m.userId));
          const availableUsers = users.filter((u) => !memberIds.has(u.id));

          return {
            loading: false,
            project,
            members,
            availableUsers,
            errorMessage: '',
          } as ProjectDetailVm;
        }),
        startWith({
          loading: true,
          project: null,
          members: [],
          availableUsers: [],
          errorMessage: '',
        } as ProjectDetailVm),
        catchError((error) =>
          of({
            loading: false,
            project: null,
            members: [],
            availableUsers: [],
            errorMessage:
              error?.error?.message ||
              error?.message ||
              'Impossible de charger le projet.',
          } as ProjectDetailVm)
        )
      );
    })
  );

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const projectId = Number(this.route.snapshot.paramMap.get('id') ?? 0);
    if (!projectId) {
      this.addMemberErrorMessage = 'Projet invalide.';
      return;
    }

    this.saving = true;
    this.addMemberErrorMessage = '';
    this.successMessage = '';

    const raw = this.form.getRawValue();

    const payload: AddProjectMemberRequest = {
      userId: Number(raw.userId),
      projectRole: raw.projectRole,
    };

    this.projectService
      .addProjectMember(projectId, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Membre ajouté avec succès.';
          this.form.reset({
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

  get userId() {
    return this.form.controls.userId;
  }

  get projectRole() {
    return this.form.controls.projectRole;
  }
}
