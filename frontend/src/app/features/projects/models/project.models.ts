export interface ProjectSummary {
  id: number;
  name: string;
  code: string;
  description?: string;
  createdAt?: string | null;
}

export interface CreateProjectRequest {
  name: string;
  code: string;
  description: string;
}
