import { apiClient } from './apiClient';
import { SupportTicket, CreateTicketRequest, UpdateTicketStatusRequest } from '../types/support';

class SupportService {
  async getAllTickets(_params?: { status?: string; category?: string; search?: string }): Promise<SupportTicket[]> {
    try {
      const res = await apiClient.get<{ content: SupportTicket[] } | SupportTicket[]>('/api/admin/support/tickets');
      if (res && typeof res === 'object' && 'content' in res && Array.isArray(res.content)) {
        return res.content;
      }
      if (Array.isArray(res)) {
        return res;
      }
    } catch (err) {
      console.warn('Failed to fetch real support tickets from backend:', err);
    }
    return [];
  }

  async getTicketsByUser(_userId?: string): Promise<SupportTicket[]> {
    try {
      const res = await apiClient.get<SupportTicket[]>('/api/support/my-tickets');
      if (Array.isArray(res)) {
        return res;
      }
    } catch (err) {
      console.warn('Failed to fetch user support tickets from backend:', err);
    }
    return [];
  }

  async createTicket(
    _user: { id: string; email: string; fullName: string },
    req: CreateTicketRequest
  ): Promise<SupportTicket> {
    const res = await apiClient.post<SupportTicket>('/api/support/tickets', req);
    return res;
  }

  async updateTicketStatus(id: string, req: UpdateTicketStatusRequest): Promise<SupportTicket> {
    const res = await apiClient.patch<SupportTicket>(`/api/admin/support/tickets/${id}/status`, req);
    return res;
  }
}

export const supportService = new SupportService();
