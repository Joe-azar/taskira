import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import { CreateProjectRequest, ProjectSummary } from '../../models/project.models';
import { ProjectService } from '../../services/project.service';

@Component({
  selector: 'app-project-list-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './project-list.page.html',
  styleUrl: './project-list.page.scss',
})
export class ProjectListPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly projectService = inject(ProjectService);

  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';
  projects: ProjectSummary[] = [];

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    key: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(10)]],
    description: [''],
  });

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading = true;
    this.errorMessage = '';

    this.projectService.getProjects().subscribe({
      next: (projects) => {
        this.projects = projects;
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage =
          error?.error?.message ||
          error?.message ||
          'Impossible de charger les projets.';
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const raw = this.form.getRawValue();

    const payload: CreateProjectRequest = {
      name: raw.name.trim(),
      key: raw.key.trim().toUpperCase(),
      description: raw.description.trim(),
    };

    this.projectService
      .createProject(payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (project) => {
          this.projects = [project, ...this.projects];
          this.successMessage = 'Projet créé avec succès.';
          this.form.reset({
            name: '',
            key: '',
            description: '',
          });
        },
        error: (error) => {
          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Impossible de créer le projet.';
        },
      });
  }

  get name() {
    return this.form.controls.name;
  }

  get key() {
    return this.form.controls.key;
  }
}
