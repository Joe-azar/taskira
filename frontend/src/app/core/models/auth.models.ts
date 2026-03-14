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

export interface LoginResponse {
  token?: string;
  accessToken?: string;
  jwt?: string;
  user?: Partial<AuthUser>;
}
