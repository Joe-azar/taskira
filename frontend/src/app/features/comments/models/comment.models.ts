export interface TicketComment {
  id: number;
  content: string;
  authorId: number;
  authorName: string;
  authorEmail?: string;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateCommentRequest {
  content: string;
}

export interface UpdateCommentRequest {
  content: string;
}
