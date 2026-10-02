import React, { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  BookOpen,
  Bookmark,
  BookmarkCheck,
  CheckCircle2,
  AlertTriangle,
  Search,
  RefreshCw,
  ExternalLink,
  Trash2,
  Save,
  Check,
  Zap,
  ArrowRight,
  Brain,
  Lightbulb,
  Award,
  Layers,
  Calendar,
  ChevronDown,
  ChevronUp,
  ChevronsUpDown,
} from 'lucide-react';
import { RevisionNote, RevisionNotesSummary, MasteryStatus } from '../types/notes';
import { revisionNotesService } from '../services/revisionNotesService';
import { LoadingSpinner } from '../components/LoadingSpinner';
import '../styles/notes.css';

export const RevisionNotesPage: React.FC = () => {
  const [notes, setNotes] = useState<RevisionNote[]>([]);
  const [summary, setSummary] = useState<RevisionNotesSummary | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'all' | 'due' | 'to_revise' | 'mastered' | 'bookmarked'>('all');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [expandedNotes, setExpandedNotes] = useState<Record<string, boolean>>({});
  const [editingReflections, setEditingReflections] = useState<Record<string, string>>({});
  const [savingReflectionId, setSavingReflectionId] = useState<string | null>(null);
  const [savedSuccessId, setSavedSuccessId] = useState<string | null>(null);

  const fetchNotesAndSummary = async () => {
    setIsLoading(true);
    try {
      const [fetchedNotes, fetchedSummary] = await Promise.all([
        revisionNotesService.getUserNotes(),
        revisionNotesService.getSummary(),
      ]);
      setNotes(fetchedNotes);
      setSummary(fetchedSummary);

      // Initialize reflections map
      const refMap: Record<string, string> = {};
      fetchedNotes.forEach((n) => {
        refMap[n.id] = n.userReflections || '';
      });
      setEditingReflections(refMap);
    } catch (err) {
      console.error('Failed to load revision notes:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchNotesAndSummary();
  }, []);

  const handleToggleBookmark = async (note: RevisionNote) => {
    try {
      const updated = await revisionNotesService.updateNote(note.id, {
        isBookmarked: !note.isBookmarked,
      });
      setNotes((prev) => prev.map((n) => (n.id === note.id ? updated : n)));
      const newSummary = await revisionNotesService.getSummary();
      setSummary(newSummary);
    } catch (err) {
      console.error('Failed to toggle bookmark:', err);
    }
  };

  const handleUpdateMastery = async (noteId: string, newStatus: MasteryStatus) => {
    try {
      const updated = await revisionNotesService.updateNote(noteId, {
        masteryStatus: newStatus,
      });
      setNotes((prev) => prev.map((n) => (n.id === noteId ? updated : n)));
      const newSummary = await revisionNotesService.getSummary();
      setSummary(newSummary);
    } catch (err) {
      console.error('Failed to update mastery:', err);
    }
  };

  const handleMarkReviewed = async (noteId: string) => {
    try {
      const updated = await revisionNotesService.markReviewed(noteId);
      setNotes((prev) => prev.map((n) => (n.id === noteId ? updated : n)));
      const newSummary = await revisionNotesService.getSummary();
      setSummary(newSummary);
    } catch (err) {
      console.error('Failed to mark reviewed:', err);
    }
  };

  const handleSaveReflection = async (noteId: string) => {
    setSavingReflectionId(noteId);
    try {
      const updated = await revisionNotesService.updateNote(noteId, {
        userReflections: editingReflections[noteId] || '',
      });
      setNotes((prev) => prev.map((n) => (n.id === noteId ? updated : n)));
      setSavedSuccessId(noteId);
      setTimeout(() => setSavedSuccessId(null), 2500);
    } catch (err) {
      console.error('Failed to save reflection:', err);
    } finally {
      setSavingReflectionId(null);
    }
  };

  const handleDeleteNote = async (noteId: string) => {
    if (!window.confirm('Are you sure you want to delete this revision note?')) return;
    try {
      await revisionNotesService.deleteNote(noteId);
      setNotes((prev) => prev.filter((n) => n.id !== noteId));
      const newSummary = await revisionNotesService.getSummary();
      setSummary(newSummary);
    } catch (err) {
      console.error('Failed to delete note:', err);
    }
  };

  const toggleExpandNote = (noteId: string) => {
    setExpandedNotes((prev) => ({
      ...prev,
      [noteId]: !prev[noteId],
    }));
  };

  const areAllExpanded = useMemo(() => {
    if (notes.length === 0) return false;
    return notes.every((n) => expandedNotes[n.id]);
  }, [notes, expandedNotes]);

  const toggleAllExpand = () => {
    const nextState = !areAllExpanded;
    const updated: Record<string, boolean> = {};
    notes.forEach((n) => {
      updated[n.id] = nextState;
    });
    setExpandedNotes(updated);
  };

  // Filter notes based on active tab, category, and search query
  const filteredNotes = useMemo(() => {
    const now = new Date().toISOString();
    return notes.filter((note) => {
      // Tab filter
      if (activeTab === 'bookmarked' && !note.isBookmarked) return false;
      if (activeTab === 'to_revise' && note.masteryStatus !== 'TO_REVISE') return false;
      if (activeTab === 'mastered' && note.masteryStatus !== 'MASTERED') return false;
      if (activeTab === 'due') {
        if (!note.nextReviewAt || note.nextReviewAt > now) return false;
      }

      // Category filter
      if (selectedCategory !== 'ALL' && note.mistakeCategory !== selectedCategory) {
        return false;
      }

      // Search filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchTitle = note.problemTitle?.toLowerCase().includes(q);
        const matchSummary = note.conceptSummary?.toLowerCase().includes(q);
        const matchTakeaway = note.keyTakeaway?.toLowerCase().includes(q);
        const matchReflection = note.userReflections?.toLowerCase().includes(q);
        if (!matchTitle && !matchSummary && !matchTakeaway && !matchReflection) return false;
      }

      return true;
    });
  }, [notes, activeTab, selectedCategory, searchQuery]);

  const formatCategoryBadge = (cat: string) => {
    switch (cat.toUpperCase()) {
      case 'OFF_BY_ONE':
        return { label: 'Off-by-One Boundary', class: 'cat-yellow' };
      case 'TIME_LIMIT_EXCEEDED':
        return { label: 'Time Complexity Detour', class: 'cat-red' };
      case 'RUNTIME_EXCEPTION':
        return { label: 'Runtime Bounds Fault', class: 'cat-orange' };
      case 'CORNER_CASE':
        return { label: 'Edge Case Invariant', class: 'cat-purple' };
      default:
        return { label: 'Algorithmic Discrepancy', class: 'cat-blue' };
    }
  };

  if (isLoading) {
    return (
      <div className="page-container center-loader">
        <LoadingSpinner message="Loading your AI Revision Notebook..." />
      </div>
    );
  }

  return (
    <div className="notes-page-container">
      {/* Page Header */}
      <div className="notes-header-hero">
        <div className="hero-content">
          <div className="hero-title-group">
            <div className="hero-icon-ring">
              <Brain size={28} className="text-yellow" />
            </div>
            <div>
              <h1 className="notes-page-title">AI Revision Notebook</h1>
              <p className="notes-page-subtitle">
                Master past failed test cases through concrete counter-examples, execution flow discrepancy traces, and spaced repetition.
              </p>
            </div>
          </div>
        </div>

        {/* Quick Refresh */}
        <button
          type="button"
          className="btn-refresh-notes"
          onClick={fetchNotesAndSummary}
          title="Refresh Notebook"
        >
          <RefreshCw size={15} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Metric Cards Banner */}
      <div className="notes-metrics-deck">
        <div className="note-metric-card">
          <div className="metric-icon-wrap bg-blue-soft">
            <BookOpen size={20} className="text-accent" />
          </div>
          <div className="metric-info">
            <span className="metric-number font-mono">{summary?.totalNotes || notes.length}</span>
            <span className="metric-text">Saved Weak Spots</span>
          </div>
        </div>

        <div className="note-metric-card">
          <div className="metric-icon-wrap bg-yellow-soft">
            <AlertTriangle size={20} className="text-yellow" />
          </div>
          <div className="metric-info">
            <span className="metric-number font-mono">{summary?.toReviseCount || 0}</span>
            <span className="metric-text">To Revise</span>
          </div>
        </div>

        <div className="note-metric-card">
          <div className="metric-icon-wrap bg-green-soft">
            <Award size={20} className="text-success" />
          </div>
          <div className="metric-info">
            <span className="metric-number font-mono">{summary?.masteredCount || 0}</span>
            <span className="metric-text">Mastered Invariants</span>
          </div>
        </div>

        <div className="note-metric-card">
          <div className="metric-icon-wrap bg-purple-soft">
            <Calendar size={20} className="text-purple" />
          </div>
          <div className="metric-info">
            <span className="metric-number font-mono">{summary?.dueTodayCount || 0}</span>
            <span className="metric-text">Due for Review Today</span>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="notes-controls-bar">
        {/* Navigation Tabs */}
        <div className="notes-tabs-cluster">
          <button
            type="button"
            className={`notes-tab-btn ${activeTab === 'all' ? 'active' : ''}`}
            onClick={() => setActiveTab('all')}
          >
            All ({notes.length})
          </button>
          <button
            type="button"
            className={`notes-tab-btn ${activeTab === 'due' ? 'active' : ''}`}
            onClick={() => setActiveTab('due')}
          >
            ⚡ Due Today ({summary?.dueTodayCount || 0})
          </button>
          <button
            type="button"
            className={`notes-tab-btn ${activeTab === 'to_revise' ? 'active' : ''}`}
            onClick={() => setActiveTab('to_revise')}
          >
            ⚠️ To Revise ({summary?.toReviseCount || 0})
          </button>
          <button
            type="button"
            className={`notes-tab-btn ${activeTab === 'mastered' ? 'active' : ''}`}
            onClick={() => setActiveTab('mastered')}
          >
            ⭐ Mastered ({summary?.masteredCount || 0})
          </button>
          <button
            type="button"
            className={`notes-tab-btn ${activeTab === 'bookmarked' ? 'active' : ''}`}
            onClick={() => setActiveTab('bookmarked')}
          >
            📌 Bookmarked ({summary?.bookmarkedCount || 0})
          </button>
        </div>

        {/* Search & Category Filter */}
        <div className="notes-search-cluster">
          <div className="notes-search-input-wrapper">
            <Search size={15} className="search-icon" />
            <input
              type="text"
              className="notes-search-input"
              placeholder="Search problems, concepts, or takeaways..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <select
            className="notes-category-select"
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            aria-label="Filter by mistake category"
          >
            <option value="ALL">All Categories</option>
            <option value="OFF_BY_ONE">Off-by-One Boundary</option>
            <option value="TIME_LIMIT_EXCEEDED">Time Limit Exceeded</option>
            <option value="LOGIC_ERROR">Logic Error</option>
            <option value="RUNTIME_EXCEPTION">Runtime Fault</option>
            <option value="CORNER_CASE">Corner Case</option>
            <option value="SYNTAX_ERROR">Syntax Invariant</option>
          </select>

          {notes.length > 0 && (
            <button
              type="button"
              className="btn-toggle-all-notes"
              onClick={toggleAllExpand}
              title={areAllExpanded ? 'Collapse all notes' : 'Expand all notes'}
            >
              <ChevronsUpDown size={14} />
              <span>{areAllExpanded ? 'Collapse All' : 'Expand All'}</span>
            </button>
          )}
        </div>
      </div>

      {/* Notes Deck */}
      {filteredNotes.length === 0 ? (
        <div className="notes-empty-state">
          <div className="empty-icon-circle">
            <BookOpen size={36} className="text-accent" />
          </div>
          <h3 className="empty-title">
            {notes.length === 0
              ? 'Your Revision Notebook is currently empty'
              : 'No notes match your active filter'}
          </h3>
          <p className="empty-subtitle">
            {notes.length === 0
              ? 'When your code fails test cases during practice or contests, click "Save to Revision Notes" to generate counter-example post-mortems and track your progress!'
              : 'Try clearing your search query or selecting "All Notes".'}
          </p>
          {notes.length === 0 && (
            <Link to="/problems" className="btn btn-primary">
              <Layers size={16} /> Explore Coding Problems
            </Link>
          )}
        </div>
      ) : (
        <div className="notes-cards-grid">
          {filteredNotes.map((note) => {
            const badge = formatCategoryBadge(note.mistakeCategory);
            const isSavingThis = savingReflectionId === note.id;
            const isSavedSuccess = savedSuccessId === note.id;
            const isExpanded = !!expandedNotes[note.id];

            return (
              <div key={note.id} className={`notebook-note-card ${isExpanded ? 'card-expanded' : 'card-collapsed'}`}>
                {/* Note Top Bar */}
                <div className="note-card-topbar">
                  <div className="note-problem-info">
                    <span className="language-badge">{note.language || 'JAVA'}</span>
                    <h3 className="note-problem-title">
                      {note.problemSlug ? (
                        <Link to={`/problems/${note.problemSlug}`} className="problem-link">
                          {note.problemTitle} <ExternalLink size={13} />
                        </Link>
                      ) : (
                        note.problemTitle
                      )}
                    </h3>
                  </div>

                  <div className="note-topbar-actions">
                    <span className={`mistake-pill ${badge.class}`}>{badge.label}</span>

                    <button
                      type="button"
                      className={`btn-fold-toggle ${isExpanded ? 'active' : ''}`}
                      onClick={() => toggleExpandNote(note.id)}
                      title={isExpanded ? 'Fold breakdown' : 'Expand full counter-example breakdown and execution traces'}
                    >
                      {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                      <span>{isExpanded ? 'Fold Solution' : 'Expand Solution'}</span>
                    </button>

                    <button
                      type="button"
                      className={`btn-icon-toggle ${note.isBookmarked ? 'bookmarked' : ''}`}
                      onClick={() => handleToggleBookmark(note)}
                      title={note.isBookmarked ? 'Remove bookmark' : 'Bookmark note'}
                    >
                      {note.isBookmarked ? <BookmarkCheck size={16} /> : <Bookmark size={16} />}
                    </button>

                    <button
                      type="button"
                      className="btn-icon-toggle btn-delete"
                      onClick={() => handleDeleteNote(note.id)}
                      title="Delete revision note"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>

                {/* Concept Diagnostic Summary */}
                <div className="note-concept-summary">
                  <p>{note.conceptSummary}</p>
                </div>

                {/* Key Takeaway Rule */}
                <div className="note-takeaway-strip">
                  <Lightbulb size={15} className="text-yellow flex-shrink-0" />
                  <div className="takeaway-text">
                    <strong>Rule of Thumb: </strong>
                    <span>{note.keyTakeaway}</span>
                  </div>
                </div>

                {/* Collapsed Preview Helper */}
                {!isExpanded && (
                  <div className="collapsed-peek-bar" onClick={() => toggleExpandNote(note.id)}>
                    <div className="peek-content">
                      <span className="peek-badge">⚡ Counter-Example:</span>
                      <code className="peek-code font-mono">{note.failedInput || 'Sample Test Case'}</code>
                    </div>
                    <span className="peek-expand-hint">
                      Click to expand full execution flow trace <ChevronDown size={13} />
                    </span>
                  </div>
                )}

                {/* Foldable Detailed Breakdown Body */}
                {isExpanded && (
                  <div className="note-expanded-body">
                    {/* Counter-Example Input/Output Diff */}
                    <div className="note-diff-box">
                      <div className="diff-input-row">
                        <span className="diff-label">Failed Input:</span>
                        <code className="diff-val font-mono">{note.failedInput || 'Sample Input'}</code>
                      </div>
                      <div className="diff-outputs-row">
                        <div className="diff-output-cell actual">
                          <span className="diff-sub-label text-danger">❌ Your Output:</span>
                          <code className="diff-val font-mono">{note.actualOutput || 'N/A'}</code>
                        </div>
                        <div className="diff-arrow">
                          <ArrowRight size={16} />
                        </div>
                        <div className="diff-output-cell expected">
                          <span className="diff-sub-label text-success">✅ Expected Invariant:</span>
                          <code className="diff-val font-mono">{note.expectedOutput || 'N/A'}</code>
                        </div>
                      </div>
                    </div>

                    {/* Execution Flow Trace Side-by-Side */}
                    <div className="note-traces-container">
                      <div className="trace-box student-trace">
                        <div className="trace-header">
                          <AlertTriangle size={13} className="text-warning" />
                          <span>Actual Execution Path (Your Code)</span>
                        </div>
                        <div className="trace-content font-mono">
                          {note.studentFlowTrace.split('\n').map((line, idx) => (
                            <p key={idx} className="trace-line">
                              {line}
                            </p>
                          ))}
                        </div>
                      </div>

                      <div className="trace-box correct-trace">
                        <div className="trace-header">
                          <CheckCircle2 size={13} className="text-success" />
                          <span>Correct Invariant Logic Flow</span>
                        </div>
                        <div className="trace-content font-mono">
                          {note.correctFlowTrace.split('\n').map((line, idx) => (
                            <p key={idx} className="trace-line">
                              {line}
                            </p>
                          ))}
                        </div>
                      </div>
                    </div>

                    {/* Personal Reflection & Notes Editor */}
                    <div className="note-reflection-box">
                      <div className="reflection-header">
                        <span className="reflection-title">Personal Reflections & Mnemonics:</span>
                        {isSavedSuccess && (
                          <span className="saved-pill">
                            <Check size={12} /> Saved
                          </span>
                        )}
                      </div>
                      <textarea
                        className="reflection-inline-textarea"
                        rows={2}
                        placeholder="Add your own mental tricks or notes on this problem..."
                        value={editingReflections[note.id] || ''}
                        onChange={(e) =>
                          setEditingReflections((prev) => ({
                            ...prev,
                            [note.id]: e.target.value,
                          }))
                        }
                      />
                      <div className="reflection-actions">
                        <button
                          type="button"
                          className="btn-save-reflection"
                          onClick={() => handleSaveReflection(note.id)}
                          disabled={isSavingThis}
                        >
                          <Save size={13} />
                          <span>{isSavingThis ? 'Saving...' : 'Save Note'}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {/* Bottom Bar: Spaced Repetition Review & Re-attempt */}
                <div className="note-card-footer">
                  <div className="spaced-review-info">
                    <span className="review-badge font-mono">
                      <RefreshCw size={11} /> Review #{note.reviewCount}
                    </span>
                    {note.nextReviewAt && (
                      <span className="next-review-text">
                        Next:{' '}
                        {new Date(note.nextReviewAt).toLocaleDateString(undefined, {
                          month: 'short',
                          day: 'numeric',
                        })}
                      </span>
                    )}
                  </div>

                  <div className="footer-actions">
                    {/* Mastery status selector */}
                    <select
                      className="mastery-select font-mono"
                      value={note.masteryStatus}
                      onChange={(e) => handleUpdateMastery(note.id, e.target.value as MasteryStatus)}
                      aria-label="Update mastery status"
                    >
                      <option value="TO_REVISE">⚠️ Needs Revision</option>
                      <option value="REVISED">🔄 Revised</option>
                      <option value="MASTERED">⭐ Mastered</option>
                      <option value="ARCHIVED">📦 Archived</option>
                    </select>

                    <button
                      type="button"
                      className="btn-advance-review"
                      onClick={() => handleMarkReviewed(note.id)}
                      title="Advance spaced repetition interval"
                    >
                      <CheckCircle2 size={13} />
                      <span>Mark Reviewed</span>
                    </button>

                    {note.problemSlug && (
                      <Link
                        to={`/problems/${note.problemSlug}`}
                        className="btn-solve-again"
                        title="Re-attempt this problem in the code editor"
                      >
                        <Zap size={13} />
                        <span>Solve Again</span>
                      </Link>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
