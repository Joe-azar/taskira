import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BehaviorSubject, combineLatest, finalize, map } from 'rxjs';

import { extractErrorMessage } from '../../../../core/http/extract-error-message';
import {
  CreateUserRequest,
  UpdateUserRequest,
  UserSummary,
} from '../../models/user.models';
import { UsersService } from '../../services/user.service';

type UserListVm = {
  loading: boolean;
  users: UserSummary[];
  errorMessage: string;
};

@Component({
  selector: 'app-user-list-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-list.page.html',
  styleUrl: './user-list.page.scss',
})
export class UserListPage {
  private readonly userService = inject(UsersService);
  private readonly fb = inject(FormBuilder);

  private readonly usersSubject = new BehaviorSubject<UserSummary[]>([]);
  readonly users$ = this.usersSubject.asObservable();

  private readonly loadingSubject = new BehaviorSubject<boolean>(true);
  readonly loading$ = this.loadingSubject.asObservable();

  private readonly errorSubject = new BehaviorSubject<string>('');
  readonly error$ = this.errorSubject.asObservable();

  selectedUser: UserSummary | null = null;
  saving = false;
  apiErrorMessage = '';
  successMessage = '';

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    globalRole: ['USER', [Validators.required]],
    active: [true],
    search: [''],
  });

  readonly vm$ = combineLatest([this.users$, this.loading$, this.error$]).pipe(
    map(([users, loading, errorMessage]) => ({ loading, users, errorMessage } as UserListVm))
  );

  constructor() {
    this.loadUsers();
  }

  private loadUsers(): void {
    this.loadingSubject.next(true);
    this.errorSubject.next('');

    this.userService
      .getUsers(this.form.controls.search.value)
      .pipe(finalize(() => this.loadingSubject.next(false)))
      .subscribe({
        next: (users) => this.usersSubject.next(users),
        error: (error) =>
          this.errorSubject.next(
            extractErrorMessage(error, 'Impossible de charger les utilisateurs.')
          ),
      });
  }

  focusEdit(user: UserSummary): void {
    this.selectedUser = user;
    this.apiErrorMessage = '';
    this.successMessage = '';

    this.userService.getUserById(user.id).subscribe({
      next: (detail) => {
        this.form.patchValue({
          firstName: detail.firstName,
          lastName: detail.lastName,
          email: detail.email,
          password: '',
          globalRole: detail.globalRole,
          active: detail.active,
        });
      },
      error: (error) => {
        this.apiErrorMessage = extractErrorMessage(error, 'Impossible de récupérer l’utilisateur.');
      },
    });
  }

  resetForm(): void {
    this.selectedUser = null;
    this.apiErrorMessage = '';
    this.successMessage = '';
    this.form.reset({
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      globalRole: 'USER',
      active: true,
      search: this.form.controls.search.value,
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();

    this.saving = true;
    this.apiErrorMessage = '';
    this.successMessage = '';

    if (this.selectedUser) {
      const payload: UpdateUserRequest = {
        firstName: raw.firstName.trim(),
        lastName: raw.lastName.trim(),
        email: raw.email.trim().toLowerCase(),
        globalRole: raw.globalRole,
        active: raw.active,
      };
      this.userService
        .updateUser(this.selectedUser.id, payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: (updatedUser) => {
            this.successMessage = 'Utilisateur mis à jour avec succès.';
            this.usersSubject.next(
              this.usersSubject.value.map((u) => (u.id === updatedUser.id ? updatedUser : u))
            );
            this.selectedUser = null;
            this.form.reset({
              firstName: '',
              lastName: '',
              email: '',
              password: '',
              globalRole: 'USER',
              active: true,
              search: this.form.controls.search.value,
            });
          },
          error: (error) => {
            this.apiErrorMessage = extractErrorMessage(error, 'Impossible de mettre à jour l’utilisateur.');
          },
        });
    } else {
      if (!raw.password?.trim() || raw.password.trim().length < 8) {
        this.apiErrorMessage = 'Le mot de passe est requis et doit contenir au moins 8 caractères.';
        this.saving = false;
        return;
      }
      const payload: CreateUserRequest = {
        firstName: raw.firstName.trim(),
        lastName: raw.lastName.trim(),
        email: raw.email.trim().toLowerCase(),
        password: raw.password.trim(),
        globalRole: raw.globalRole,
        active: raw.active,
      };
      this.userService
        .createUser(payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: (createdUser) => {
            this.successMessage = 'Utilisateur créé avec succès.';
            this.usersSubject.next([...this.usersSubject.value, createdUser]);
            this.resetForm();
          },
          error: (error) => {
            this.apiErrorMessage = extractErrorMessage(error, 'Impossible de créer l’utilisateur.');
          },
        });
    }
  }

  toggleStatus(user: UserSummary): void {
    this.saving = true;
    this.apiErrorMessage = '';
    this.successMessage = '';
    this.userService
      .updateUserStatus(user.id, { active: !user.active })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (updatedUser) => {
          this.successMessage = updatedUser.active ? 'Utilisateur activé.' : 'Utilisateur désactivé.';
          this.usersSubject.next(
            this.usersSubject.value.map((u) => (u.id === updatedUser.id ? updatedUser : u))
          );
        },
        error: (error) => {
          this.apiErrorMessage = extractErrorMessage(error, 'Impossible de mettre à jour le statut.');
        },
      });
  }

  search(): void {
    this.loadUsers();
  }

  cancelEdit(): void {
    this.resetForm();
  }

  get firstName() {
    return this.form.controls.firstName;
  }

  get lastName() {
    return this.form.controls.lastName;
  }

  get email() {
    return this.form.controls.email;
  }

  get password() {
    return this.form.controls.password;
  }
}
