export const TICKET_STATUSES = [
  'OPEN',
  'IN_PROGRESS',
  'REVIEW',
  'DONE',
  'BLOCKED',
  'CANCELLED',
] as const;

export const TICKET_TYPES = [
  'BUG',
  'TASK',
  'FEATURE',
  'SUPPORT',
  'IMPROVEMENT',
] as const;

export const TICKET_PRIORITIES = [
  'LOW',
  'MEDIUM',
  'HIGH',
  'CRITICAL',
] as const;