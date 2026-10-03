import React, { useState } from 'react';
import {
  Sparkles,
  Bookmark,
  BookmarkCheck,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  Save,
  BookOpen,
  Loader2,
  Lightbulb,
  ExternalLink,
  Clock,
  Compass,
  ChevronDown,
  ChevronUp,
  LogIn,
  UserPlus,
  Copy,
  Check,
} from 'lucide-react';
import { PostMortemAnalysis } from '../types/notes';
import { revisionNotesService } from '../services/revisionNotesService';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import '../styles/notes.css';

interface AiPostMortemCardProps {
  analysis: PostMortemAnalysis;
  problemId?: string;
  submissionId?: string;
  problemTitle: string;
  problemSlug?: string;
  language?: string;
  submittedCode?: string;
  onSaved?: () => void;
}

export const AiPostMortemCard: React.FC<AiPostMortemCardProps> = ({
  analysis,
  problemId,
  submissionId,
  problemTitle,
  problemSlug,
  language = 'JAVA',
  submittedCode,
  onSaved,
}) => {
  const { isAuthenticated } = useAuth();
  const [userReflections, setUserReflections] = useState('');
  const [isBookmarked, setIsBookmarked] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isSaved, setIsSaved] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [isFlowExpanded, setIsFlowExpanded] = useState<boolean>(true);
  const [isCopied, setIsCopied] = useState<boolean>(false);

  const handleSaveToNotes = async () => {
    if (!isAuthenticated) return;
    setIsSaving(true);
    setSaveError(null);
    try {
      await revisionNotesService.createNote({
        problemId,
        submissionId,
        problemTitle,
        problemSlug,
        language,
        mistakeCategory: analysis.mistakeCategory,
        conceptSummary: analysis.conceptSummary,
        failedInput: analysis.failedInput,
        actualOutput: analysis.actualOutput,
        expectedOutput: analysis.expectedOutput,
        studentFlowTrace: analysis.studentFlowTrace,
        correctFlowTrace: analysis.correctFlowTrace,
        keyTakeaway: analysis.keyTakeaway,
        submittedCode,
        userReflections: userReflections.trim() || undefined,
        isBookmarked,
      });

      setIsSaved(true);
      if (onSaved) onSaved();
    } catch (err: unknown) {
      setSaveError(err instanceof Error ? err.message : 'Failed to save note.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleCopySummary = async () => {
    const summaryText = `[AI Post-Mortem: ${problemTitle}]\n` +
      `Category: ${analysis.mistakeCategory}\n` +
      `Concept: ${analysis.conceptSummary}\n` +
      `Failed Input: ${analysis.failedInput}\n` +
      `Your Output: ${analysis.actualOutput}\n` +
      `Expected: ${analysis.expectedOutput}\n` +
      `Key Takeaway: ${analysis.keyTakeaway}`;

    try {
      await navigator.clipboard.writeText(summaryText);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    } catch {
      // Ignore clipboard error
    }
  };

  const formatCategoryBadge = (cat: string) => {
    switch (cat.toUpperCase()) {
      case 'SYNTAX_ERROR':
        return { label: 'Syntax & Compiler Diagnostic', icon: <AlertTriangle size={13} className="text-danger" /> };
      case 'OFF_BY_ONE':
        return { label: 'Off-by-One Boundary', icon: <AlertTriangle size={13} className="text-yellow" /> };
      case 'TIME_LIMIT_EXCEEDED':
        return { label: 'Time Complexity Detour', icon: <Clock size={13} className="text-accent" /> };
      case 'RUNTIME_EXCEPTION':
        return { label: 'Runtime Fault / Bounds', icon: <AlertTriangle size={13} className="text-danger" /> };
      case 'CORNER_CASE':
        return { label: 'Edge Case Invariant', icon: <Compass size={13} className="text-purple" /> };
      case 'TYPE_MISMATCH':
        return { label: 'Type Signature Mismatch', icon: <AlertTriangle size={13} className="text-danger" /> };
      default:
        return { label: 'Algorithmic Discrepancy', icon: <Lightbulb size={13} className="text-yellow" /> };
    }
  };

  const isSyntax = analysis.mistakeCategory.toUpperCase() === 'SYNTAX_ERROR';
  const badgeInfo = formatCategoryBadge(analysis.mistakeCategory);

  return (
    <div className="post-mortem-card">
      {/* Header */}
      <div className="post-mortem-header">
        <div className="post-mortem-title-group">
          <div className="post-mortem-icon-badge">
            <Sparkles size={16} className="text-yellow" />
          </div>
          <div>
            <h4 className="post-mortem-title">
              {isSyntax ? 'AI Socratic Compiler Post-Mortem' : 'AI Socratic Post-Mortem & Flow Discrepancy'}
            </h4>
            <span className="post-mortem-subtitle">
              {isSyntax
                ? 'Syntax diagnostic breakdown • Token mismatch vs. Correct syntax invariant'
                : 'Counter-example breakdown • What your code did vs. Correct invariant'}
            </span>
          </div>
        </div>

        <div className="post-mortem-badges">
          <span className="category-pill">
            {badgeInfo.icon}
            <span>{badgeInfo.label}</span>
          </span>

          <button
            type="button"
            className="btn-fold-flow"
            onClick={() => setIsFlowExpanded(!isFlowExpanded)}
            title={isFlowExpanded ? 'Collapse execution flow trace' : 'Expand execution flow trace'}
          >
            {isFlowExpanded ? (
              <>
                <ChevronUp size={13} /> <span>Fold Flow</span>
              </>
            ) : (
              <>
                <ChevronDown size={13} /> <span>Expand Flow</span>
              </>
            )}
          </button>

          <button
            type="button"
            className="btn-fold-flow btn-copy-summary"
            onClick={handleCopySummary}
            title="Copy post-mortem summary"
          >
            {isCopied ? <Check size={13} className="text-success" /> : <Copy size={13} />}
            <span>{isCopied ? 'Copied' : 'Copy'}</span>
          </button>

          {isAuthenticated && (
            <button
              type="button"
              className={`btn-bookmark-toggle ${isBookmarked ? 'active' : ''}`}
              onClick={() => setIsBookmarked(!isBookmarked)}
              title={isBookmarked ? 'Bookmarked for high-priority revision' : 'Bookmark this note'}
            >
              {isBookmarked ? <BookmarkCheck size={16} /> : <Bookmark size={16} />}
            </button>
          )}
        </div>
      </div>

      {/* Summary statement */}
      <div className="post-mortem-summary-callout">
        <p className="summary-text">{analysis.conceptSummary}</p>
      </div>

      {/* Counter-Example Input/Output Diff */}
      <div className="counter-example-grid">
        <div className="example-block failed-input-block">
          <span className="example-tag">
            {isSyntax ? '⚡ Evaluation Stage' : '⚡ Failed Counter-Example Input'}
          </span>
          <code className="example-code font-mono">{analysis.failedInput || 'N/A'}</code>
        </div>

        <div className="example-outputs-row">
          <div className="example-block actual-output-block">
            <span className="example-tag text-danger">
              {isSyntax ? '❌ Compiler Diagnostics' : '❌ Your Code Output'}
            </span>
            <code className="example-code font-mono">{analysis.actualOutput || 'Empty / Error'}</code>
          </div>
          <div className="flow-arrow-separator">
            <ArrowRight size={18} />
          </div>
          <div className="example-block expected-output-block">
            <span className="example-tag text-success">
              {isSyntax ? '✅ Expected Build State' : '✅ Expected Invariant Output'}
            </span>
            <code className="example-code font-mono">{analysis.expectedOutput || 'N/A'}</code>
          </div>
        </div>
      </div>

      {/* Flow Comparison Grid: Student Execution vs Correct Invariant (Foldable) */}
      {isFlowExpanded ? (
        <div className="flow-comparison-grid">
          {/* Actual Flow */}
          <div className="flow-col flow-actual">
            <div className="flow-col-header">
              <AlertTriangle size={14} className="text-warning" />
              <span>
                {isSyntax
                  ? '🔍 Compilation Failure Sequence (What javac Encountered)'
                  : '🔍 Actual Execution Path (What Your Code Did)'}
              </span>
            </div>
            <div className="flow-col-body font-mono">
              {analysis.studentFlowTrace.split('\n').map((line, i) => (
                <p key={i} className="flow-step-line">
                  {line}
                </p>
              ))}
            </div>
          </div>

          {/* Correct Invariant Flow */}
          <div className="flow-col flow-correct">
            <div className="flow-col-header">
              <CheckCircle2 size={14} className="text-success" />
              <span>
                {isSyntax
                  ? '💡 Correct Syntax Invariant (How to Balance and Structure)'
                  : '💡 Invariant Logic (Why Expected Output Is Achieved)'}
              </span>
            </div>
            <div className="flow-col-body font-mono">
              {analysis.correctFlowTrace.split('\n').map((line, i) => (
                <p key={i} className="flow-step-line">
                  {line}
                </p>
              ))}
            </div>
          </div>
        </div>
      ) : (
        <div className="collapsed-flow-bar" onClick={() => setIsFlowExpanded(true)}>
          <span className="collapsed-flow-text">
            ⚡ Execution flow paths hidden ({analysis.studentFlowTrace.split('\n').length} steps)
          </span>
          <span className="collapsed-flow-action">
            <ChevronDown size={14} /> Click to expand full step-by-step trace
          </span>
        </div>
      )}

      {/* Mental Model / Takeaway */}
      <div className="post-mortem-takeaway-box">
        <div className="takeaway-header">
          <Lightbulb size={15} className="text-yellow" />
          <span>Rule of Thumb to Remember:</span>
        </div>
        <p className="takeaway-content">{analysis.keyTakeaway}</p>
      </div>

      {/* User Reflections & Save Action / Guest CTA */}
      <div className="post-mortem-save-section">
        {!isAuthenticated ? (
          /* Guest mode prompt */
          <div className="guest-note-cta-banner">
            <div className="guest-cta-text-group">
              <div className="guest-cta-icon-wrap">
                <Sparkles size={18} className="text-yellow" />
              </div>
              <div>
                <h5 className="guest-cta-title">Save this AI Note to your Personal Revision Notebook</h5>
                <p className="guest-cta-sub">
                  Create a free account or log in to store counter-examples, schedule Spaced Repetition reviews, and track your coding growth.
                </p>
              </div>
            </div>
            <div className="guest-cta-actions">
              <Link to="/login" className="btn-guest-login">
                <LogIn size={14} /> Log In to Save Note
              </Link>
              <Link to="/register" className="btn-guest-register">
                <UserPlus size={14} /> Create Free Account
              </Link>
              <Link to="/notes" className="btn-guest-notes">
                <BookOpen size={14} /> View Notes Hub
              </Link>
            </div>
          </div>
        ) : !isSaved ? (
          /* Logged-in user save form */
          <>
            <div className="user-reflection-input-group">
              <label htmlFor="reflectionInput" className="reflection-label">
                Personal mnemonic / quick reflection (optional):
              </label>
              <textarea
                id="reflectionInput"
                className="reflection-textarea"
                rows={2}
                placeholder="e.g., Note: always initialize hashmap before scanning loop..."
                value={userReflections}
                onChange={(e) => setUserReflections(e.target.value)}
              />
            </div>

            {saveError && <div className="save-error-banner">{saveError}</div>}

            <div className="post-mortem-actions">
              <button
                type="button"
                className="btn-save-note"
                onClick={handleSaveToNotes}
                disabled={isSaving}
              >
                {isSaving ? (
                  <>
                    <Loader2 size={14} className="spin-icon" /> Saving Note...
                  </>
                ) : (
                  <>
                    <Save size={14} /> 💾 Save to Revision Notes
                  </>
                )}
              </button>
            </div>
          </>
        ) : (
          <div className="saved-success-bar">
            <div className="saved-info">
              <CheckCircle2 size={18} className="text-success" />
              <span>Saved to your personal Revision Notebook!</span>
            </div>
            <Link to="/notes" className="btn-view-notes">
              <BookOpen size={14} /> Open Revision Notes <ExternalLink size={12} />
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

