export interface TicketSummary {
  id: number;
  reference: string;
  title: string;
  type: string;
  status: string;
  priority: string;
  assigneeFullName?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface TicketDetail {
  id: number;
  reference: string;
  projectId: number;
  projectCode: string;
  projectName: string;
  title: string;
  description: string;
  type: string;
  status: string;
  priority: string;
  creatorName: string;
  creatorEmail?: string;
  creatorGlobalRole?: string;
  assigneeName?: string | null;
  assigneeEmail?: string | null;
  dueDate?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface TicketHistoryEntry {
  id: number;
  fieldName: string;
  oldValue?: string | null;
  newValue?: string | null;
  changedByName: string;
  changedByEmail?: string;
  changedAt?: string | null;
}

export interface CreateTicketRequest {
  projectId: number;
  title: string;
  description: string;
  type: string;
  priority: string;
  dueDate?: string | null;
}

export interface UpdateTicketStatusRequest {
  status: string;
}

export interface UpdateTicketAssigneeRequest {
  assigneeId: number | null;
}
