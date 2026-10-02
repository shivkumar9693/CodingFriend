import React, { useState, useEffect, useCallback } from 'react';
import {
  Bot,
  Sparkles,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Clock,
  ArrowRight,
  RotateCw,
  HelpCircle,
  Lightbulb,
  BookOpen,
  Tag,
  ShieldCheck,
  Zap,
} from 'lucide-react';
import { submissionService } from '../services/submissionService';
import { mentorService } from '../services/mentorService';
import { SubmissionResponse, SubmissionHintResponse, TestCaseEvaluationResult } from '../types/submission';

export interface AiMentorPanelProps {
  submission: SubmissionResponse;
  onOpenFullModal?: () => void;
}

const HINT_LEVEL_LABELS: Record<number, { title: string; subtitle: string }> = {
  1: { title: 'Level 1: Guiding Question', subtitle: 'Observational Socratic inquiry to guide your focus' },
  2: { title: 'Level 2: Conceptual Hint', subtitle: 'Theoretical foundation & algorithmic concept' },
  3: { title: 'Level 3: Targeted Example', subtitle: 'Failing edge case scenario analysis' },
  4: { title: 'Level 4: Detailed Explanation', subtitle: 'Line-by-line inspection without solution leakage' },
  5: { title: 'Level 5: Solution Guidance', subtitle: 'Architectural blueprint & final structural guidance' },
};

const MISTAKE_LABELS: Record<string, { label: string; desc: string }> = {
  OFF_BY_ONE: { label: 'Off-by-One Boundary', desc: 'Loop index or array boundary exceeds collection length' },
  WRONG_CONDITION: { label: 'Wrong Branching Condition', desc: 'Inverted boolean expression or operator mismatch' },
  EDGE_CASE: { label: 'Unhandled Edge Case', desc: 'Empty input, zero, negative value, or single element not handled' },
  INEFFICIENT_ALGORITHM: { label: 'Inefficient Time Complexity', desc: 'Nested iterations causing high time complexity' },
  INCORRECT_DATA_STRUCTURE: { label: 'Suboptimal Data Structure', desc: 'Inefficient container for lookup or mutation patterns' },
  RECURSION_ERROR: { label: 'Recursion / Stack Issue', desc: 'Missing base condition or duplicate subproblem computation' },
  LOGICAL_ERROR: { label: 'Algorithmic Logic Bug', desc: 'Incorrect state mutation or mathematical transformation' },
  COMPILE_ERROR: { label: 'Compilation Syntax Error', desc: 'Type mismatch, missing semicolon, or invalid syntax' },
  RUNTIME_ERROR: { label: 'Runtime Exception', desc: 'Null pointer, arithmetic error, or unexpected exception' },
  TIME_LIMIT_EXCEEDED: { label: 'Time Limit Exceeded', desc: 'Infinite loop or execution timeout exceeded' },
};

const mapLevelToHintEnum = (
  lvl: number
): 'LEVEL_1_NUDGE' | 'LEVEL_2_CONCEPT' | 'LEVEL_3_ALGORITHM' | 'LEVEL_4_TARGETED' => {
  switch (lvl) {
    case 1:
      return 'LEVEL_1_NUDGE';
    case 2:
      return 'LEVEL_2_CONCEPT';
    case 3:
      return 'LEVEL_3_ALGORITHM';
    case 4:
    case 5:
    default:
      return 'LEVEL_4_TARGETED';
  }
};

export const AiMentorPanel: React.FC<AiMentorPanelProps> = ({
  submission,
  onOpenFullModal,
}) => {
  const [currentLevel, setCurrentLevel] = useState<number>(1);
  const [hint, setHint] = useState<SubmissionHintResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Parse test case results safely
  let testCasesList: TestCaseEvaluationResult[] = [];
  if (submission.evaluationDetails) {
    try {
      testCasesList = JSON.parse(submission.evaluationDetails);
    } catch {
      testCasesList = [];
    }
  }

  const failedTest = testCasesList.find((tc) => !tc.passed);

  const fetchHintForLevel = useCallback(async (level: number) => {
    setIsLoading(true);
    setError(null);
    try {
      if (submission.id) {
        try {
          const response = await submissionService.getSubmissionHint(submission.id, level);
          setHint(response);
          setCurrentLevel(response.hintLevel);
          return;
        } catch (subErr: any) {
          const isNotFound = subErr?.response?.status === 404 || subErr?.status === 404;
          const isProgressionGuard =
            subErr?.response?.status === 400 &&
            (subErr?.response?.data?.message?.includes('Progression') ||
              subErr?.response?.data?.message?.includes('progression') ||
              subErr?.message?.includes('Progression'));

          if (isProgressionGuard) {
            setError(
              'Progression Constraint: Please explore foundational hint tiers (Levels 1-3) before unlocking detailed explanations (Level 4) or solution guidance (Level 5).'
            );
            return;
          }

          if (!isNotFound) {
            throw subErr;
          }
        }
      }

      // Seamless fallback to SocraBuddy general problem hint service
      if (submission.problemId && submission.sourceCode) {
        const mentorRes = await mentorService.getHint({
          problemId: submission.problemId,
          sourceCode: submission.sourceCode,
          hintLevel: mapLevelToHintEnum(level),
          lastErrorMessage: submission.errorMessage || undefined,
        });
        const hintText = mentorRes.socraticQuestion
          ? `${mentorRes.guidance} ${mentorRes.socraticQuestion}`
          : mentorRes.guidance || 'Review your logic carefully.';
        setHint({
          mistakeType: submission.verdict || 'LOGICAL_ERROR',
          hintLevel: level,
          hint: hintText,
          concept: mentorRes.conceptKeywords?.length ? mentorRes.conceptKeywords.join(', ') : undefined,
          revealsSolution: level === 5,
        });
        setCurrentLevel(level);
      } else {
        throw new Error('Problem context is missing to generate Socratic hint.');
      }
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'AI mentor service is temporarily unavailable.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [submission]);

  // Load level 1 hint on mount if submission is not ACCEPTED
  useEffect(() => {
    if (submission.verdict && submission.verdict !== 'ACCEPTED') {
      fetchHintForLevel(1);
    }
  }, [submission.id, submission.verdict, fetchHintForLevel]);

  const handleNextHint = () => {
    if (currentLevel < 5 && !isLoading) {
      fetchHintForLevel(currentLevel + 1);
    }
  };

  const handleRetry = () => {
    fetchHintForLevel(currentLevel);
  };

  const isAccepted = submission.verdict === 'ACCEPTED';
  const mistakeInfo = MISTAKE_LABELS[hint?.mistakeType || submission.verdict || 'LOGICAL_ERROR'] || {
    label: hint?.mistakeType || 'General Mistake',
    desc: 'Algorithmic or runtime failure identified in code execution',
  };

  return (
    <div className="ai-mentor-panel-card" data-testid="ai-mentor-panel">
      {/* 1. Execution Result Section */}
      <div className="mentor-section execution-result-section">
        <div className="section-header-row">
          <div className="section-title-group">
            <span className="section-icon">
              {isAccepted ? (
                <CheckCircle2 size={18} className="text-green" />
              ) : (
                <XCircle size={18} className="text-red" />
              )}
            </span>
            <h4 className="section-title">Execution Result</h4>
          </div>

          <span
            className={`verdict-pill ${
              isAccepted ? 'verdict-pill-accepted' : 'verdict-pill-failed'
            }`}
            data-testid="execution-verdict"
          >
            {submission.verdict || submission.status}
          </span>
        </div>

        <div className="execution-metrics-grid">
          <div className="metric-box">
            <span className="metric-box-label">Tests Passed</span>
            <span className="metric-box-value" data-testid="tests-passed-count">
              {submission.testsPassed} / {submission.totalTests}
            </span>
          </div>

          <div className="metric-box">
            <span className="metric-box-label">Runtime</span>
            <span className="metric-box-value">
              <Clock size={12} className="inline-icon" /> {submission.executionTimeMs} ms
            </span>
          </div>

          {failedTest && (
            <div className="metric-box metric-box-failed" data-testid="failed-test-box">
              <span className="metric-box-label">First Failing Test</span>
              <span className="metric-box-value text-red">
                Case #{failedTest.testNumber || 'N/A'}{' '}
                {failedTest.isHidden && (
                  <span className="hidden-pill" title="Test data is protected">
                    <ShieldCheck size={11} /> Hidden
                  </span>
                )}
              </span>
            </div>
          )}
        </div>

        {/* Protected Hidden Test Case Disclaimer */}
        {failedTest?.isHidden && (
          <div className="hidden-test-notice" data-testid="hidden-test-notice">
            <HelpCircle size={14} className="text-muted" />
            <span>Hidden test case inputs/outputs are strictly shielded to protect grading integrity.</span>
          </div>
        )}
      </div>

      {/* If Accepted: Celebration Card */}
      {isAccepted && (
        <div className="mentor-accepted-banner" data-testid="mentor-accepted-card">
          <Sparkles size={20} className="text-yellow sparkle-spin" />
          <div className="accepted-banner-text">
            <h5>All Test Cases Passed!</h5>
            <p>Excellent algorithmic solution. SocraBuddy verified your logic meets all complexity bounds.</p>
          </div>
        </div>
      )}

      {/* 2. Mistake Analysis Section (for incorrect submissions) */}
      {!isAccepted && (
        <div className="mentor-section mistake-analysis-section" data-testid="mistake-analysis-section">
          <div className="section-header-row">
            <div className="section-title-group">
              <span className="section-icon text-yellow">
                <AlertTriangle size={17} />
              </span>
              <h4 className="section-title">Mistake Analysis</h4>
            </div>

            <span className="mistake-badge" data-testid="mistake-type-badge">
              {hint?.mistakeType || submission.verdict || 'DIAGNOSING'}
            </span>
          </div>

          <div className="mistake-body">
            <p className="mistake-description-lead">
              <strong>{mistakeInfo.label}:</strong> {mistakeInfo.desc}
            </p>
            {hint?.concept && (
              <div className="concept-pill-row">
                <Tag size={13} className="text-cyan" />
                <span className="concept-pill-text">{hint.concept}</span>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 3. AI Mentor Socratic Section */}
      {!isAccepted && (
        <div className="mentor-section ai-mentor-socratic-section" data-testid="ai-mentor-socratic-section">
          <div className="section-header-row">
            <div className="section-title-group">
              <span className="section-icon text-cyan">
                <Bot size={18} />
              </span>
              <h4 className="section-title">AI Mentor &bull; SocraBuddy</h4>
            </div>

            <div className="hint-level-badge" data-testid="hint-level-badge">
              <Lightbulb size={13} className="text-yellow" />
              <span>Level {currentLevel} of 5</span>
            </div>
          </div>

          <div className="level-subtitle">
            {HINT_LEVEL_LABELS[currentLevel]?.title || `Hint Level ${currentLevel}`} &mdash;{' '}
            <span className="text-muted">{HINT_LEVEL_LABELS[currentLevel]?.subtitle}</span>
          </div>

          {/* Loading State */}
          {isLoading && (
            <div className="mentor-loading-box" data-testid="mentor-loading-box">
              <div className="loading-shimmer-row shimmer-1" />
              <div className="loading-shimmer-row shimmer-2" />
              <div className="loading-shimmer-row shimmer-3" />
              <div className="loading-caption">
                <Zap size={14} className="text-cyan pulse" />
                <span>SocraBuddy is analyzing your code and constructing a Socratic nudge...</span>
              </div>
            </div>
          )}

          {/* AI Unavailable / Error State with Retry Button */}
          {error && !isLoading && (
            <div className="mentor-error-box" data-testid="mentor-error-box">
              <div className="mentor-error-top">
                <AlertTriangle size={16} className="text-red" />
                <span>{error}</span>
              </div>
              <button
                type="button"
                className="btn-mentor-retry"
                onClick={handleRetry}
                data-testid="mentor-retry-btn"
              >
                <RotateCw size={13} />
                <span>Retry AI Mentor</span>
              </button>
            </div>
          )}

          {/* Socratic Hint Display */}
          {!isLoading && !error && hint && (
            <div className="mentor-hint-bubble" data-testid="mentor-hint-bubble">
              <div className="quote-mark">&ldquo;</div>
              <p className="hint-text-content" data-testid="hint-text">
                {hint.hint}
              </p>
              <div className="quote-mark-end">&rdquo;</div>

              {hint.revealsSolution && (
                <div className="solution-warning">
                  <BookOpen size={13} />
                  <span>Targeted Solution Guidance (Level 5)</span>
                </div>
              )}
            </div>
          )}

          {/* Hint Progress Dots */}
          <div className="hint-level-dots-row">
            {[1, 2, 3, 4, 5].map((lvl) => (
              <button
                key={lvl}
                type="button"
                className={`level-dot ${lvl <= currentLevel ? 'active' : ''} ${
                  lvl === currentLevel ? 'current' : ''
                }`}
                onClick={() => fetchHintForLevel(lvl)}
                disabled={isLoading}
                title={HINT_LEVEL_LABELS[lvl]?.title}
              >
                {lvl}
              </button>
            ))}
          </div>

          {/* Actions & Next Hint Button */}
          <div className="mentor-actions-row">
            {currentLevel < 5 ? (
              <button
                type="button"
                className="btn-next-hint"
                onClick={handleNextHint}
                disabled={isLoading}
                data-testid="next-hint-btn"
              >
                <span>Give me another hint (Level {currentLevel + 1})</span>
                <ArrowRight size={14} />
              </button>
            ) : (
              <span className="max-hint-note">
                <CheckCircle2 size={14} className="text-green" /> Max hint level reached (Level 5)
              </span>
            )}

            {onOpenFullModal && (
              <button
                type="button"
                className="btn-open-interactive-mentor"
                onClick={onOpenFullModal}
                title="Open interactive dialogue with SocraBuddy"
              >
                <Sparkles size={13} />
                <span>Interactive Mentor</span>
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
