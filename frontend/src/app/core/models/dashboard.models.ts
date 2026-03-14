export interface RecentTicket {
  id: number;
  reference?: string;
  title: string;
  status: string;
  priority?: string;
}

export interface DashboardSummary {
  totalProjects: number;
  totalTickets: number;
  openTickets: number;
  inProgressTickets: number;
  doneTickets: number;
  myTickets: number;
  criticalTickets: number;
  recentTickets: RecentTicket[];
}
