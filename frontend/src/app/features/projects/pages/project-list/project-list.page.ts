import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, finalize, map } from 'rxjs';

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

  private readonly projectsSubject = new BehaviorSubject<ProjectSummary[]>([]);
  readonly projects$ = this.projectsSubject.asObservable();

  private readonly loadingSubject = new BehaviorSubject<boolean>(true);
  readonly loading$ = this.loadingSubject.asObservable();

  private readonly errorSubject = new BehaviorSubject<string>('');
  readonly error$ = this.errorSubject.asObservable();

  saving = false;
  createErrorMessage = '';
  successMessage = '';

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    code: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(10)]],
    description: [''],
  });

  readonly vm$ = combineLatest([this.projects$, this.loading$, this.error$]).pipe(
    map(([projects, loading, errorMessage]) => ({
      loading,
      projects,
      errorMessage,
    } as ProjectsVm))
  );

  constructor() {
    this.loadProjects();
  }

  private loadProjects(): void {
    this.loadingSubject.next(true);
    this.errorSubject.next('');

    this.projectService.getProjects().subscribe({
      next: (projects) => {
        this.projectsSubject.next(projects);
        this.loadingSubject.next(false);
      },
      error: (error) => {
        this.errorSubject.next(
          error?.error?.message || error?.message || 'Impossible de charger les projets.'
        );
        this.loadingSubject.next(false);
      },
    });
  }

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
        next: (newProject) => {
          this.successMessage = 'Projet créé avec succès.';
          this.projectsSubject.next([...this.projectsSubject.value, newProject]);
          this.form.reset({
            name: '',
            code: '',
            description: '',
          });
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