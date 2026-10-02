import { describe, it, expect, vi, beforeEach } from 'vitest';
import { supportService } from './supportService';
import { apiClient } from './apiClient';

describe('supportService', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('should fetch tickets from backend endpoint', async () => {
    const mockTickets = [
      {
        id: 't-1',
        userId: 'u-1',
        userEmail: 'alex@example.com',
        userFullName: 'Alex',
        category: 'BUG_REPORT',
        priority: 'HIGH',
        status: 'OPEN',
        subject: 'Syntax issue',
        description: 'Detail',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    ];

    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ content: mockTickets });

    const tickets = await supportService.getAllTickets();
    expect(tickets.length).toBe(1);
    expect(tickets[0].id).toBe('t-1');
    expect(tickets[0].subject).toBe('Syntax issue');
  });

  it('should create a support ticket via POST API', async () => {
    const user = { id: 'test-user-123', email: 'test@example.com', fullName: 'Test Learner' };
    const mockCreated = {
      id: 'ticket-created-999',
      userId: user.id,
      userEmail: user.email,
      userFullName: user.fullName,
      category: 'BUG_REPORT' as const,
      priority: 'HIGH' as const,
      status: 'OPEN' as const,
      subject: 'Compiler timeout on prime sieve',
      description: 'Execution hangs for large arrays',
      problemId: 'two-sum',
      problemTitle: 'Two Sum',
      codeSnippet: 'int[] primes = new int[1000];',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    vi.spyOn(apiClient, 'post').mockResolvedValueOnce(mockCreated);

    const created = await supportService.createTicket(user, {
      category: 'BUG_REPORT',
      priority: 'HIGH',
      subject: 'Compiler timeout on prime sieve',
      description: 'Execution hangs for large arrays',
      problemId: 'two-sum',
      problemTitle: 'Two Sum',
      codeSnippet: 'int[] primes = new int[1000];',
    });

    expect(created.id).toBe('ticket-created-999');
    expect(created.status).toBe('OPEN');
    expect(created.userId).toBe('test-user-123');
  });

  it('should update ticket status via PATCH API', async () => {
    const mockUpdated = {
      id: 't-1',
      userId: 'u-1',
      userEmail: 'alex@example.com',
      userFullName: 'Alex',
      category: 'BUG_REPORT' as const,
      priority: 'HIGH' as const,
      status: 'RESOLVED' as const,
      subject: 'Syntax issue',
      description: 'Detail',
      adminNotes: 'Adjusted JVM memory limit',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    vi.spyOn(apiClient, 'patch').mockResolvedValueOnce(mockUpdated);

    const updated = await supportService.updateTicketStatus('t-1', {
      status: 'RESOLVED',
      adminNotes: 'Adjusted JVM memory limit',
    });

    expect(updated.status).toBe('RESOLVED');
    expect(updated.adminNotes).toBe('Adjusted JVM memory limit');
  });
});
