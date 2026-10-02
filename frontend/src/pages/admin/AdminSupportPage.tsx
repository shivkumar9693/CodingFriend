import React, { useState, useEffect } from 'react';
import { LifeBuoy, Bug, AlertTriangle, Sparkles, HelpCircle, CheckCircle2, Clock, Search, ShieldCheck, Code2, User as UserIcon, Send } from 'lucide-react';
import { supportService } from '../../services/supportService';
import { SupportTicket, TicketStatus, TicketCategory } from '../../types/support';
import { LoadingSpinner } from '../../components/LoadingSpinner';

export const AdminSupportPage: React.FC = () => {
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTicket, setSelectedTicket] = useState<SupportTicket | null>(null);
  const [adminNoteInput, setAdminNoteInput] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);

  const loadTickets = async () => {
    setIsLoading(true);
    try {
      const list = await supportService.getAllTickets();
      setTickets(list);
      if (list.length > 0 && !selectedTicket) {
        setSelectedTicket(list[0]);
        setAdminNoteInput(list[0].adminNotes || '');
      }
    } catch (err) {
      console.error('Failed to load support tickets:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadTickets();
  }, []);

  const handleSelectTicket = (ticket: SupportTicket) => {
    setSelectedTicket(ticket);
    setAdminNoteInput(ticket.adminNotes || '');
  };

  const handleStatusChange = async (newStatus: TicketStatus) => {
    if (!selectedTicket) return;
    setIsUpdating(true);
    try {
      const updated = await supportService.updateTicketStatus(selectedTicket.id, {
        status: newStatus,
        adminNotes: adminNoteInput.trim() || undefined,
      });
      setSelectedTicket(updated);
      setTickets((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
    } catch (err) {
      console.error('Failed to update ticket status:', err);
    } finally {
      setIsUpdating(false);
    }
  };

  const handleSaveNotes = async () => {
    if (!selectedTicket) return;
    setIsUpdating(true);
    try {
      const updated = await supportService.updateTicketStatus(selectedTicket.id, {
        status: selectedTicket.status,
        adminNotes: adminNoteInput.trim() || undefined,
      });
      setSelectedTicket(updated);
      setTickets((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
    } catch (err) {
      console.error('Failed to save admin note:', err);
    } finally {
      setIsUpdating(false);
    }
  };

  const filteredTickets = tickets.filter((t) => {
    const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
    const matchesCategory = categoryFilter === 'ALL' || t.category === categoryFilter;
    const matchesSearch =
      t.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.userFullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.userEmail.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (t.problemTitle && t.problemTitle.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesStatus && matchesCategory && matchesSearch;
  });

  const getStatusBadge = (status: TicketStatus) => {
    switch (status) {
      case 'OPEN':
        return <span className="badge badge-warning"><Clock size={12} /> Open</span>;
      case 'IN_PROGRESS':
        return <span className="badge badge-info"><Clock size={12} /> In Progress</span>;
      case 'RESOLVED':
        return <span className="badge badge-success"><CheckCircle2 size={12} /> Resolved</span>;
      case 'CLOSED':
        return <span className="badge badge-secondary">Closed</span>;
    }
  };

  const getCategoryIcon = (category: TicketCategory) => {
    switch (category) {
      case 'BUG_REPORT':
        return <Bug size={15} className="text-error" />;
      case 'PROBLEM_ISSUE':
        return <AlertTriangle size={15} className="text-warning" />;
      case 'MENTOR_FEEDBACK':
        return <Sparkles size={15} className="text-accent" />;
      default:
        return <HelpCircle size={15} className="text-info" />;
    }
  };

  if (isLoading) {
    return (
      <div className="page-container center-loader">
        <LoadingSpinner message="Loading student support and bug tickets..." />
      </div>
    );
  }

  return (
    <div className="page-container admin-support-page">
      <div className="admin-header">
        <div className="admin-badge">
          <ShieldCheck size={14} /> Student Support & Bug Inbox
        </div>
        <h1 className="page-title">Learner Bug Reports & Help Desk</h1>
        <p className="page-subtitle">
          Triage platform bugs, inspect test case disputes, and assist learners directly.
        </p>
      </div>

      {/* Top Filter Bar */}
      <div className="admin-support-toolbar">
        <div className="support-search-wrapper">
          <Search size={15} className="support-search-icon" />
          <input
            type="text"
            className="support-search-input"
            placeholder="Search by student name, email, subject, or problem..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div className="filter-select-group">
          <select
            className="support-select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="OPEN">Open</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
          </select>

          <select
            className="support-select"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
          >
            <option value="ALL">All Categories</option>
            <option value="BUG_REPORT">Bug Reports</option>
            <option value="PROBLEM_ISSUE">Problem / Test Flaws</option>
            <option value="MENTOR_FEEDBACK">AI Mentor Feedback</option>
            <option value="GENERAL_SUPPORT">General Support</option>
          </select>
        </div>
      </div>

      {/* Two Column Layout: Ticket List & Ticket Detail */}
      <div className="support-split-layout">
        {/* Left Column: Tickets Stream */}
        <div className="support-list-panel">
          <div className="panel-header-count">
            <span>{filteredTickets.length} Tickets Found</span>
          </div>

          <div className="support-tickets-stream">
            {filteredTickets.length === 0 ? (
              <div className="empty-stream-message">
                <LifeBuoy size={28} className="text-muted mb-2" />
                <p>No tickets matching selected filters.</p>
              </div>
            ) : (
              filteredTickets.map((ticket) => (
                <div
                  key={ticket.id}
                  className={`support-ticket-item ${selectedTicket?.id === ticket.id ? 'active-ticket' : ''}`}
                  onClick={() => handleSelectTicket(ticket)}
                >
                  <div className="ticket-item-header">
                    <div className="ticket-category-label">
                      {getCategoryIcon(ticket.category)}
                      <span>{ticket.category.replace('_', ' ')}</span>
                    </div>
                    {getStatusBadge(ticket.status)}
                  </div>

                  <h4 className="ticket-item-subject">{ticket.subject}</h4>

                  <div className="ticket-item-footer">
                    <span className="student-name">
                      <UserIcon size={12} /> {ticket.userFullName}
                    </span>
                    <span className="ticket-date">
                      {new Date(ticket.createdAt).toLocaleDateString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right Column: Active Ticket Inspection */}
        <div className="support-detail-panel">
          {selectedTicket ? (
            <div className="ticket-detail-view">
              <div className="detail-top-bar">
                <div>
                  <div className="ticket-tag-row">
                    <span className={`priority-tag priority-${selectedTicket.priority.toLowerCase()}`}>
                      {selectedTicket.priority} Priority
                    </span>
                    {selectedTicket.problemTitle && (
                      <span className="problem-ref-tag">
                        <Code2 size={13} /> {selectedTicket.problemTitle}
                      </span>
                    )}
                  </div>
                  <h2 className="detail-subject">{selectedTicket.subject}</h2>
                </div>

                <div className="status-action-buttons">
                  <button
                    className={`status-btn ${selectedTicket.status === 'OPEN' ? 'active-open' : ''}`}
                    onClick={() => handleStatusChange('OPEN')}
                    disabled={isUpdating}
                  >
                    Open
                  </button>
                  <button
                    className={`status-btn ${selectedTicket.status === 'IN_PROGRESS' ? 'active-prog' : ''}`}
                    onClick={() => handleStatusChange('IN_PROGRESS')}
                    disabled={isUpdating}
                  >
                    In Progress
                  </button>
                  <button
                    className={`status-btn ${selectedTicket.status === 'RESOLVED' ? 'active-res' : ''}`}
                    onClick={() => handleStatusChange('RESOLVED')}
                    disabled={isUpdating}
                  >
                    Resolved
                  </button>
                </div>
              </div>

              <div className="student-info-strip">
                <div className="student-meta">
                  <div className="student-avatar-letter">
                    {selectedTicket.userFullName.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <div className="student-full-name">{selectedTicket.userFullName}</div>
                    <div className="student-email-sub">{selectedTicket.userEmail}</div>
                  </div>
                </div>
                <div className="timestamp-badge">
                  Submitted: {new Date(selectedTicket.createdAt).toLocaleString()}
                </div>
              </div>

              <div className="detail-body-section">
                <h3>Description & Observed Behavior</h3>
                <p className="detail-description">{selectedTicket.description}</p>

                {selectedTicket.codeSnippet && (
                  <div className="detail-snippet-box">
                    <div className="snippet-box-header">
                      <Code2 size={14} /> Learner Code / Output Snapshot
                    </div>
                    <pre className="snippet-code-block">{selectedTicket.codeSnippet}</pre>
                  </div>
                )}
              </div>

              {/* Admin Notes & Resolution Section */}
              <div className="admin-resolution-box">
                <div className="resolution-header">
                  <ShieldCheck size={16} className="text-accent" />
                  <h4>Admin Resolution & Internal Notes</h4>
                </div>
                <textarea
                  className="form-textarea"
                  rows={3}
                  placeholder="Record investigation findings, root cause, or resolution steps taken..."
                  value={adminNoteInput}
                  onChange={(e) => setAdminNoteInput(e.target.value)}
                />
                <div className="resolution-footer">
                  <button
                    className="btn btn-primary btn-sm"
                    onClick={handleSaveNotes}
                    disabled={isUpdating}
                  >
                    <Send size={13} /> Save Resolution Notes
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="no-ticket-selected">
              <div className="empty-icon-circle">
                <LifeBuoy size={42} className="text-muted" />
              </div>
              <h3>Select a ticket from the left stream to inspect</h3>
              <p className="empty-subtitle">
                Review student bug reports, inspect problem context, and resolve issues.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
