export interface ProjectSummary {
  id: number;
  code: string;
  name: string;
  description?: string;
  status?: string;
  createdAt?: string | null;
}

export interface ProjectDetail {
  id: number;
  code: string;
  name: string;
  description?: string;
  status: string;
  ownerId: number;
  ownerName: string;
  ownerEmail?: string;
  memberCount: number;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface ProjectMember {
  userId: number;
  displayName: string;
  email: string;
  projectRole: string;
  globalRole?: string;
  joinedAt?: string | null;
}

export interface CreateProjectRequest {
  name: string;
  code: string;
  description: string;
}

export interface UpdateProjectRequest {
  name: string;
  code: string;
  description: string | null;
}

export interface AddProjectMemberRequest {
  userId: number;
  projectRole: string;
}
