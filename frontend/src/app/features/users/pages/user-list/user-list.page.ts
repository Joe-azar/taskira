import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BehaviorSubject, catchError, finalize, map, of, startWith, switchMap, tap } from 'rxjs';

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
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  selectedUser: UserSummary | null = null;
  saving = false;
  apiErrorMessage = '';
  successMessage = '';

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.minLength(8)]],
    globalRole: ['USER', [Validators.required]],
    active: [true],
    search: [''],
  });

  readonly vm$ = this.refresh$.pipe(
    switchMap(() =>
      this.userService.getUsers(this.form.controls.search.value).pipe(
        map((users) => ({ loading: false, users, errorMessage: '' } as UserListVm)),
        catchError((error) =>
          of({
            loading: false,
            users: [],
            errorMessage:
              error?.error?.message || error?.message || 'Impossible de charger les utilisateurs.',
          } as UserListVm)
        ),
        startWith({ loading: true, users: [], errorMessage: '' } as UserListVm)
      )
    )
  );

  constructor() {
    this.refresh$.next();
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
        this.apiErrorMessage = error?.error?.message || error?.message || 'Impossible de récupérer l’utilisateur.';
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
          next: () => {
            this.successMessage = 'Utilisateur mis à jour avec succès.';
            this.refresh$.next();
          },
          error: (error) => {
            this.apiErrorMessage = error?.error?.message || error?.message || 'Impossible de mettre à jour l’utilisateur.';
          },
        });
    } else {
      if (!raw.password?.trim()) {
        this.apiErrorMessage = 'Le mot de passe est requis pour créer un utilisateur.';
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
          next: () => {
            this.successMessage = 'Utilisateur créé avec succès.';
            this.resetForm();
            this.refresh$.next();
          },
          error: (error) => {
            this.apiErrorMessage = error?.error?.message || error?.message || 'Impossible de créer l’utilisateur.';
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
      .pipe(
        finalize(() => (this.saving = false)),
        tap(() => {
          this.successMessage = user.active ? 'Utilisateur désactivé.' : 'Utilisateur activé.';
          this.refresh$.next();
        })
      )
      .subscribe({
        error: (error) => {
          this.apiErrorMessage = error?.error?.message || error?.message || 'Impossible de mettre à jour le statut.';
        },
      });
  }

  search(): void {
    this.refresh$.next();
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
