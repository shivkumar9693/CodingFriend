import React, { useState, useEffect } from 'react';
import { 
  X, 
  LifeBuoy, 
  Bug, 
  Sparkles, 
  HelpCircle, 
  AlertTriangle, 
  CheckCircle2, 
  Send, 
  Layers, 
  Check, 
  Terminal,
  FileText,
  LucideIcon
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { supportService } from '../services/supportService';
import { TicketCategory, TicketPriority } from '../types/support';
import { problemService } from '../services/problemService';
import { ProblemSummaryResponse } from '../types/problem';
import { LoadingSpinner } from './LoadingSpinner';

interface SupportModalProps {
  isOpen: boolean;
  onClose: () => void;
  defaultProblemId?: string;
  defaultProblemTitle?: string;
  defaultCodeSnippet?: string;
}

const CATEGORIES: Array<{
  id: TicketCategory;
  title: string;
  desc: string;
  icon: LucideIcon;
  accentColor: string;
}> = [
  {
    id: 'BUG_REPORT',
    title: 'Bug Report',
    desc: 'Editor freeze, visual glitch, or crash',
    icon: Bug,
    accentColor: '#ef4444',
  },
  {
    id: 'PROBLEM_ISSUE',
    title: 'Problem / Test Suite',
    desc: 'Incorrect test case or problem flaw',
    icon: AlertTriangle,
    accentColor: '#f59e0b',
  },
  {
    id: 'MENTOR_FEEDBACK',
    title: 'AI Mentor Feedback',
    desc: 'Socratic hint quality or suggestions',
    icon: Sparkles,
    accentColor: '#8b5cf6',
  },
  {
    id: 'GENERAL_SUPPORT',
    title: 'General Support',
    desc: 'Account or feature inquiries',
    icon: HelpCircle,
    accentColor: '#3b82f6',
  },
];

const PRIORITIES: Array<{ id: TicketPriority; label: string; dotColor: string }> = [
  { id: 'LOW', label: 'Low', dotColor: '#10b981' },
  { id: 'MEDIUM', label: 'Medium', dotColor: '#f59e0b' },
  { id: 'HIGH', label: 'High', dotColor: '#f97316' },
  { id: 'CRITICAL', label: 'Critical', dotColor: '#ef4444' },
];

export const SupportModal: React.FC<SupportModalProps> = ({
  isOpen,
  onClose,
  defaultProblemId,
  defaultProblemTitle,
  defaultCodeSnippet,
}) => {
  const { user } = useAuth();
  const [category, setCategory] = useState<TicketCategory>('BUG_REPORT');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [selectedProblemId, setSelectedProblemId] = useState(defaultProblemId || '');
  const [problems, setProblems] = useState<ProblemSummaryResponse[]>([]);
  const [codeSnippet, setCodeSnippet] = useState(defaultCodeSnippet || '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdTicketId, setCreatedTicketId] = useState<string | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setError('');
      setCreatedTicketId(null);
      if (defaultProblemId) setSelectedProblemId(defaultProblemId);
      if (defaultCodeSnippet) setCodeSnippet(defaultCodeSnippet);

      problemService.getProblems({ page: 0, size: 50 })
        .then((res) => setProblems(res.content || []))
        .catch(() => {});
    }
  }, [isOpen, defaultProblemId, defaultCodeSnippet]);

  // Handle escape key to close
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isSubmitting) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isSubmitting, onClose]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subject.trim() || !description.trim()) {
      setError('Please provide a subject summary and detailed description.');
      return;
    }

    if (!user) {
      setError('You must be logged in to submit a ticket.');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      const matchedProblem = problems.find((p) => p.id === selectedProblemId || p.slug === selectedProblemId);
      const ticket = await supportService.createTicket(
        {
          id: user.id,
          email: user.email,
          fullName: user.fullName || user.email,
        },
        {
          category,
          priority,
          subject: subject.trim(),
          description: description.trim(),
          problemId: selectedProblemId || null,
          problemTitle: matchedProblem?.title || defaultProblemTitle || null,
          codeSnippet: codeSnippet.trim() || null,
        }
      );

      setCreatedTicketId(ticket.id);
      setTimeout(() => {
        setCreatedTicketId(null);
        onClose();
        setSubject('');
        setDescription('');
        setCodeSnippet('');
      }, 2600);
    } catch (err: unknown) {
      const e = err as Error;
      setError(e.message || 'Failed to submit ticket. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="support-modal-backdrop" onClick={onClose}>
      <div 
        className="support-modal-window" 
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        {/* Modal Glow Accent */}
        <div className="modal-glow-top"></div>

        {/* Modal Header */}
        <div className="support-modal-header">
          <div className="header-badge-group">
            <div className="header-icon-box">
              <LifeBuoy size={22} className="text-accent" />
            </div>
            <div>
              <div className="header-tag">Student Help & Issue Center</div>
              <h2 className="header-headline">Report an Issue or Request Help</h2>
            </div>
          </div>
          <button 
            className="modal-icon-close" 
            onClick={onClose} 
            title="Close dialog (Esc)"
            aria-label="Close dialog"
          >
            <X size={18} />
          </button>
        </div>

        {createdTicketId ? (
          <div className="support-success-view">
            <div className="success-lottie-circle">
              <CheckCircle2 size={56} className="text-success animate-bounce" />
            </div>
            <h3 className="success-headline">Ticket Dispatched to Maintainers</h3>
            <p className="success-subtext">
              Reference ID: <code className="ticket-ref-pill font-mono">{createdTicketId}</code>
            </p>
            <p className="success-note">
              Admins have been notified. You can review resolution progress in your account center.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="support-modal-form">
            {error && (
              <div className="modal-alert-error">
                <AlertTriangle size={16} />
                <span>{error}</span>
              </div>
            )}

            {/* Step 1: Category Grid Cards */}
            <div className="form-section">
              <label className="section-label">Select Issue Category</label>
              <div className="category-cards-grid">
                {CATEGORIES.map((cat) => {
                  const Icon = cat.icon;
                  const isSelected = category === cat.id;
                  return (
                    <div
                      key={cat.id}
                      className={`category-card ${isSelected ? 'selected' : ''}`}
                      onClick={() => setCategory(cat.id)}
                      style={{ '--cat-accent': cat.accentColor } as React.CSSProperties}
                    >
                      <div className="cat-card-top">
                        <div className="cat-icon-wrap" style={{ color: cat.accentColor }}>
                          <Icon size={18} />
                        </div>
                        {isSelected && (
                          <div className="cat-check-badge">
                            <Check size={12} />
                          </div>
                        )}
                      </div>
                      <div className="cat-card-title">{cat.title}</div>
                      <div className="cat-card-desc">{cat.desc}</div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Step 2: Priority & Problem Attachment Row */}
            <div className="form-two-col">
              <div className="form-col">
                <label className="section-label">Urgency Priority</label>
                <div className="priority-segmented-control">
                  {PRIORITIES.map((p) => {
                    const isSelected = priority === p.id;
                    return (
                      <button
                        key={p.id}
                        type="button"
                        className={`priority-seg-btn ${isSelected ? 'active' : ''}`}
                        onClick={() => setPriority(p.id)}
                      >
                        <span className="priority-dot" style={{ backgroundColor: p.dotColor }} />
                        <span>{p.label}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="form-col">
                <label className="section-label">Attached Problem (Optional)</label>
                <div className="problem-dropdown-wrap">
                  <Layers size={15} className="dropdown-prefix-icon" />
                  <select
                    className="modal-select has-prefix"
                    value={selectedProblemId}
                    onChange={(e) => setSelectedProblemId(e.target.value)}
                  >
                    <option value="">General (Not Problem Specific)</option>
                    {problems.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.title} • {p.difficulty}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* Step 3: Subject Line */}
            <div className="form-section">
              <label className="section-label">
                Subject Summary <span className="text-accent">*</span>
              </label>
              <div className="input-with-icon">
                <FileText size={16} className="input-prefix-icon" />
                <input
                  type="text"
                  className="modal-input has-prefix"
                  placeholder="e.g. Hidden test case 4 fails on large prime inputs"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  required
                />
              </div>
            </div>

            {/* Step 4: Description */}
            <div className="form-section">
              <div className="label-row-between">
                <label className="section-label">
                  Detailed Explanation <span className="text-accent">*</span>
                </label>
                <span className="char-indicator">{description.length}/1000</span>
              </div>
              <textarea
                className="modal-textarea"
                rows={2}
                maxLength={1000}
                placeholder="What happened? What was expected? Brief reproduction..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>

            {/* Step 5: Code Snapshot / Output */}
            <div className="form-section">
              <div className="code-editor-card">
                <div className="code-editor-header">
                  <div className="code-header-left">
                    <Terminal size={13} className="text-accent" />
                    <span>Relevant Code / Output Snapshot (Optional)</span>
                  </div>
                  {codeSnippet && (
                    <button 
                      type="button" 
                      className="btn-clear-code"
                      onClick={() => setCodeSnippet('')}
                    >
                      Clear
                    </button>
                  )}
                </div>
                <textarea
                  className="code-textarea font-mono"
                  rows={2}
                  placeholder="// Paste code or compiler error here..."
                  value={codeSnippet}
                  onChange={(e) => setCodeSnippet(e.target.value)}
                />
              </div>
            </div>

            {/* Modal Actions Footer */}
            <div className="support-modal-footer">
              <div className="footer-shortcut-tip">
                <span>Press</span> <kbd>Esc</kbd> <span>to cancel</span>
              </div>
              <div className="footer-btn-group">
                <button 
                  type="button" 
                  className="btn btn-secondary btn-sm" 
                  onClick={onClose} 
                  disabled={isSubmitting}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="btn btn-primary btn-sm submit-ticket-btn" 
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <LoadingSpinner size="sm" />
                  ) : (
                    <>
                      <Send size={14} /> Send Ticket to Admin
                    </>
                  )}
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
