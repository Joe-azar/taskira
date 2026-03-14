export interface ProjectSummary {
  id: number;
  name: string;
  key: string;
  description?: string;
  createdAt?: string | null;
}

export interface CreateProjectRequest {
  name: string;
  key: string;
  description: string;
}
