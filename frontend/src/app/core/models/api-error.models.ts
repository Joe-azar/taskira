export interface ApiProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Correlation id also present as the X-Request-Id response header; for support/log lookup, never shown to the end user. */
  requestId?: string;
}
