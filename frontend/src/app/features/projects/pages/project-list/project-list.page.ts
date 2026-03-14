import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, finalize, map, of, startWith, switchMap } from 'rxjs';

import { CreateProjectRequest, ProjectSummary } from '../../models/project.models';
import { ProjectService } from '../../services/project.service';

type ProjectsVm = {
  loading: boolean;
  projects: ProjectSummary[];
  errorMessage: string;
};

@Component({
  selector: 'app-project-list-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './project-list.page.html',
  styleUrl: './project-list.page.scss',
})
export class ProjectListPage {
  private readonly fb = inject(FormBuilder);
  private readonly projectService = inject(ProjectService);
  private readonly reloadSubject = new BehaviorSubject<void>(undefined);

  saving = false;
  createErrorMessage = '';
  successMessage = '';

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    code: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(10)]],
    description: [''],
  });

  readonly vm$ = this.reloadSubject.pipe(
    switchMap(() =>
      this.projectService.getProjects().pipe(
        map((projects): ProjectsVm => ({
          loading: false,
          projects,
          errorMessage: '',
        })),
        startWith({
          loading: true,
          projects: [],
          errorMessage: '',
        } as ProjectsVm),
        catchError((error) =>
          of({
            loading: false,
            projects: [],
            errorMessage:
              error?.error?.message ||
              error?.message ||
              'Impossible de charger les projets.',
          } as ProjectsVm)
        )
      )
    )
  );

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.createErrorMessage = '';
    this.successMessage = '';

    const raw = this.form.getRawValue();

    const payload: CreateProjectRequest = {
      name: raw.name.trim(),
      code: raw.code.trim().toUpperCase(),
      description: raw.description.trim(),
    };

    this.projectService
      .createProject(payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.successMessage = 'Projet créé avec succès.';
          this.form.reset({
            name: '',
            code: '',
            description: '',
          });
          this.reloadSubject.next();
        },
        error: (error) => {
          this.createErrorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible de créer le projet.';
        },
      });
  }

  get name() {
    return this.form.controls.name;
  }

  get code() {
    return this.form.controls.code;
  }
}