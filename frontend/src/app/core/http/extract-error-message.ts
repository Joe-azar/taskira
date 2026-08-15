import { ApiProblemDetail } from '../models/api-error.models';

export function extractErrorMessage(error: unknown, fallback: string): string {
  const httpError = error as { error?: ApiProblemDetail; message?: string } | undefined;

  return httpError?.error?.detail || httpError?.message || fallback;
}
