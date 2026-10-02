export type TicketCategory = 'BUG_REPORT' | 'PROBLEM_ISSUE' | 'MENTOR_FEEDBACK' | 'FEATURE_REQUEST' | 'GENERAL_SUPPORT';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface SupportTicket {
  id: string;
  userId: string;
  userEmail: string;
  userFullName: string;
  category: TicketCategory;
  priority: TicketPriority;
  status: TicketStatus;
  subject: string;
  description: string;
  problemId?: string | null;
  problemTitle?: string | null;
  codeSnippet?: string | null;
  adminNotes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTicketRequest {
  category: TicketCategory;
  priority: TicketPriority;
  subject: string;
  description: string;
  problemId?: string | null;
  problemTitle?: string | null;
  codeSnippet?: string | null;
}

export interface UpdateTicketStatusRequest {
  status: TicketStatus;
  adminNotes?: string | null;
}
