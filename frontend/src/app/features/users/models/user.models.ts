export interface UserSummary {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  globalRole: 'ADMIN' | 'USER' | string;
  active: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export type UserDetail = UserSummary;

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  globalRole: 'ADMIN' | 'USER' | string;
  active: boolean;
}

export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  globalRole: 'ADMIN' | 'USER' | string;
  active: boolean;
}

export interface UpdateUserStatusRequest {
  active: boolean;
}
