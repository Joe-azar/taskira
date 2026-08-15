export interface AuthUser {
  id: number;
  email: string;
  role: string;
  firstName?: string;
  lastName?: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}
