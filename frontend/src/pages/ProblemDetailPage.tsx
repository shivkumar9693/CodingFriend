import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  CheckCircle2,
  FileText,
  HelpCircle,
  Layers,
  Terminal,
  History,
  Check,
  AlertCircle,
  Play,
  Send,
  Loader2,
  Sparkles,
} from 'lucide-react';
import { problemService } from '../services/problemService';
import { submissionService } from '../services/submissionService';
import { ProblemDetail } from '../types/problem';
import { SubmissionResponse, SubmissionSummaryResponse } from '../types/submission';
import { DifficultyBadge } from '../components/DifficultyBadge';
import { StatusBadge } from '../components/StatusBadge';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EditorPanel } from '../components/EditorPanel';
import { SocraBuddyModal } from '../components/SocraBuddyModal';
import { useAuth } from '../context/AuthContext';

const DEFAULT_JAVA_STARTER = `class Solution {
    // Write your solution here
}
`;

export const ProblemDetailPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const { isAuthenticated } = useAuth();

  const [problem, setProblem] = useState<ProblemDetail | null>(null);
  const [submissions, setSubmissions] = useState<SubmissionSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Execution & Mentor State
  const [lastEvaluation, setLastEvaluation] = useState<SubmissionResponse | null>(null);
  const [isRunning, setIsRunning] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isMentorOpen, setIsMentorOpen] = useState(false);
  const [mentorInitialMode, setMentorInitialMode] = useState<'hint' | 'diagnose'>('hint');
  const [selectedLanguage, setSelectedLanguage] = useState<string>('JAVA');

  // Editor State
  const [code, setCode] = useState<string>('');
  const [isAutosaved, setIsAutosaved] = useState<boolean>(true);
  const [panelTab, setPanelTab] = useState<'statement' | 'submissions'>('statement');
  const [mobileTab, setMobileTab] = useState<'problem' | 'editor'>('problem');
  const [submissionFeedback, setSubmissionFeedback] = useState<{
    type: 'success' | 'error';
    message: string;
  } | null>(null);

  // Compute storage key per problem and per language
  const getStorageKey = useCallback((problemId?: string, problemSlug?: string, lang: string = selectedLanguage) => {
    return `acm_code_${problemId || problemSlug || 'default'}_${lang.toUpperCase()}`;
  }, [selectedLanguage]);

  const fetchSubmissionsForProblem = useCallback(async (problemId: string) => {
    if (!isAuthenticated) return;
    try {
      const res = await submissionService.getSubmissions({
        problemId,
        page: 0,
        size: 10,
      });
      setSubmissions(res.content || []);
    } catch {
      // Ignore if unauthenticated or error
    }
  }, [isAuthenticated]);

  // Fetch problem details
  useEffect(() => {
    const fetchProblem = async () => {
      if (!slug) return;
      setIsLoading(true);
      setError(null);
      try {
        const data = await problemService.getProblemByIdOrSlug(slug);
        setProblem(data);

        const initialLang = data.supportedLanguage || 'JAVA';
        setSelectedLanguage(initialLang);

        // Load autosaved code or starter template for initial language
        const storageKey = `acm_code_${data.id || data.slug || 'default'}_${initialLang.toUpperCase()}`;
        const savedCode = localStorage.getItem(storageKey);
        if (savedCode !== null && savedCode !== undefined) {
          setCode(savedCode);
        } else {
          const starter = data.starterTemplates?.[initialLang] || data.starterCode || DEFAULT_JAVA_STARTER;
          setCode(starter);
        }

        // Fetch submissions history
        fetchSubmissionsForProblem(data.id);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : 'Problem not found or failed to load.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchProblem();
  }, [slug, fetchSubmissionsForProblem]);

  // Handle switching language
  const handleLanguageChange = (newLang: string) => {
    if (!problem) return;
    setSelectedLanguage(newLang);
    const storageKey = getStorageKey(problem.id, problem.slug, newLang);
    const savedCode = localStorage.getItem(storageKey);
    if (savedCode !== null && savedCode !== undefined) {
      setCode(savedCode);
    } else {
      const starter = problem.starterTemplates?.[newLang] || problem.starterCode || DEFAULT_JAVA_STARTER;
      setCode(starter);
    }
  };

  const getCurrentStarterCode = () => {
    if (!problem) return DEFAULT_JAVA_STARTER;
    return problem.starterTemplates?.[selectedLanguage] || problem.starterCode || DEFAULT_JAVA_STARTER;
  };

  // Handle code modification with autosave
  const handleCodeChange = (newCode: string) => {
    setCode(newCode);
    setIsAutosaved(false);

    if (problem) {
      const storageKey = getStorageKey(problem.id, problem.slug, selectedLanguage);
      try {
        localStorage.setItem(storageKey, newCode);
        setIsAutosaved(true);
      } catch {
        // LocalStorage error handling
      }
    }
  };

  // Handle reset code to default starter template for active language
  const handleResetCode = () => {
    if (!problem) return;
    const starter = problem.starterTemplates?.[selectedLanguage] || problem.starterCode || DEFAULT_JAVA_STARTER;
    setCode(starter);
    const storageKey = getStorageKey(problem.id, problem.slug, selectedLanguage);
    try {
      localStorage.setItem(storageKey, starter);
      setIsAutosaved(true);
    } catch {
      // LocalStorage error handling
    }
  };

  // Handle running code against test suite
  const handleRunCode = async () => {
    if (!problem) return;
    if (!isAuthenticated) {
      setSubmissionFeedback({
        type: 'error',
        message: 'You must be logged in to execute your solution against test cases.',
      });
      return;
    }

    setIsRunning(true);
    setSubmissionFeedback(null);
    try {
      const res = await submissionService.createSubmission({
        problemId: problem.id,
        sourceCode: code,
        language: selectedLanguage,
      });

      setLastEvaluation(res);
      fetchSubmissionsForProblem(problem.id);
    } catch (err: unknown) {
      setSubmissionFeedback({
        type: 'error',
        message: err instanceof Error ? err.message : 'Execution failed. Please try again.',
      });
    } finally {
      setIsRunning(false);
    }
  };

  // Handle full submission
  const handleSubmitCode = async () => {
    if (!problem) return;
    if (!isAuthenticated) {
      setSubmissionFeedback({
        type: 'error',
        message: 'You must be logged in to submit your solution.',
      });
      return;
    }

    setIsSubmitting(true);
    setSubmissionFeedback(null);
    try {
      const res = await submissionService.createSubmission({
        problemId: problem.id,
        sourceCode: code,
        language: selectedLanguage,
      });

      setLastEvaluation(res);
      fetchSubmissionsForProblem(problem.id);
      setPanelTab('submissions');
    } catch (err: unknown) {
      setSubmissionFeedback({
        type: 'error',
        message: err instanceof Error ? err.message : 'Submission failed. Please try again.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenMentor = () => {
    setMentorInitialMode('hint');
    setIsMentorOpen(true);
  };

  const handleDiagnoseError = () => {
    setMentorInitialMode('diagnose');
    setIsMentorOpen(true);
  };

  if (isLoading) {
    return (
      <div className="page-container center-loader" data-testid="problem-loading">
        <LoadingSpinner message="Loading problem details..." />
      </div>
    );
  }

  if (error || !problem) {
    return (
      <div className="page-container" data-testid="problem-error">
        <div className="problems-error-card">
          <h2>Problem Not Found</h2>
          <p>{error || 'The requested problem could not be located.'}</p>
          <Link to="/problems" className="btn btn-primary">
            <ArrowLeft size={16} /> Back to Problems Catalog
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container problem-detail-page" data-testid="problem-detail-page">
      {/* Top Header & Breadcrumbs */}
      <div className="problem-detail-topbar">
        <Link to="/problems" className="back-link">
          <ArrowLeft size={16} /> Back to Problems
        </Link>

        {/* Top Central Quick Actions */}
        <div className="problem-top-actions">
          <button
            type="button"
            className="btn-toolbar-mentor"
            onClick={handleOpenMentor}
            title="Ask SocraBuddy for a Socratic hint"
            data-testid="top-ask-mentor-btn"
          >
            <Sparkles size={14} className="text-yellow mr-1" />
            <span>Ask SocraBuddy</span>
          </button>

          <button
            type="button"
            className="btn btn-secondary btn-sm run-code-btn"
            onClick={handleRunCode}
            disabled={isRunning || isSubmitting}
            data-testid="top-run-code-btn"
            title="Execute solution against isolated sandbox test suite"
          >
            {isRunning ? (
              <>
                <Loader2 size={13} className="spinner-icon" />
                <span>Running...</span>
              </>
            ) : (
              <>
                <Play size={13} />
                <span>Run</span>
              </>
            )}
          </button>

          <button
            type="button"
            className="btn btn-primary btn-sm submit-code-btn"
            onClick={handleSubmitCode}
            disabled={isRunning || isSubmitting}
            data-testid="top-submit-code-btn"
            title="Submit solution for full grading and SocraBuddy evaluation"
          >
            {isSubmitting ? (
              <>
                <Loader2 size={13} className="spinner-icon" />
                <span>Evaluating...</span>
              </>
            ) : (
              <>
                <Send size={13} />
                <span>Submit</span>
              </>
            )}
          </button>
        </div>

        {/* Mobile View Tab Switcher */}
        <div className="mobile-view-tabs">
          <button
            type="button"
            className={`tab-btn ${mobileTab === 'problem' ? 'active' : ''}`}
            onClick={() => setMobileTab('problem')}
          >
            <FileText size={15} />
            <span>Problem</span>
          </button>
          <button
            type="button"
            className={`tab-btn ${mobileTab === 'editor' ? 'active' : ''}`}
            onClick={() => setMobileTab('editor')}
          >
            <Terminal size={15} />
            <span>Code Editor</span>
          </button>
        </div>
      </div>

      <div className="problem-workspace-grid">
        {/* Left Column: Problem Statement & Submissions Tab */}
        <div className={`problem-statement-panel ${mobileTab !== 'problem' ? 'mobile-hidden' : ''}`}>
          <div className="problem-header-card">
            <div className="problem-header-top">
              <div className="problem-title-row">
                <h1 className="problem-title-lg">{problem.title}</h1>
                <DifficultyBadge difficulty={problem.difficulty} />
              </div>

              <div className="problem-metadata-row">
                {problem.tags?.map((tag) => (
                  <span key={tag} className="problem-tag">
                    {tag.toLowerCase().replace(/_/g, ' ')}
                  </span>
                ))}
              </div>
            </div>

            {/* Sub-tabs: Statement vs Submissions */}
            <div className="panel-subtabs">
              <button
                type="button"
                className={`panel-subtab-btn ${panelTab === 'statement' ? 'active' : ''}`}
                onClick={() => setPanelTab('statement')}
              >
                <FileText size={14} />
                <span>Statement</span>
              </button>
              <button
                type="button"
                className={`panel-subtab-btn ${panelTab === 'submissions' ? 'active' : ''}`}
                onClick={() => setPanelTab('submissions')}
              >
                <History size={14} />
                <span>Submissions</span>
                <span className="subtab-count-badge">{submissions.length}</span>
              </button>
            </div>
          </div>

          {submissionFeedback && (
            <div
              className={`feedback-toast ${
                submissionFeedback.type === 'success' ? 'success-alert' : 'error-alert'
              }`}
            >
              {submissionFeedback.type === 'success' ? (
                <Check size={16} />
              ) : (
                <AlertCircle size={16} />
              )}
              <span>{submissionFeedback.message}</span>
            </div>
          )}

          {panelTab === 'statement' ? (
            <>
              {/* Problem Statement */}
              <section className="section-card">
                <h2 className="section-title">
                  <FileText size={18} /> Problem Statement
                </h2>
                <div className="problem-description-content">
                  {problem.description.split('\n\n').map((para, i) => (
                    <p key={i}>{para}</p>
                  ))}
                </div>
              </section>

              {/* Examples */}
              {problem.examples && (
                <section className="section-card">
                  <h2 className="section-title">
                    <Layers size={18} /> Examples
                  </h2>
                  <pre className="code-block-preview">{problem.examples}</pre>
                </section>
              )}

              {/* Constraints & Formats */}
              {(problem.constraints || problem.inputFormat || problem.outputFormat) && (
                <section className="section-card">
                  <h2 className="section-title">
                    <HelpCircle size={18} /> Constraints & Formats
                  </h2>
                  {problem.constraints && (
                    <div className="format-box">
                      <strong>Constraints:</strong>
                      <pre className="format-text">{problem.constraints}</pre>
                    </div>
                  )}
                  {problem.inputFormat && (
                    <div className="format-box">
                      <strong>Input Format:</strong>
                      <p>{problem.inputFormat}</p>
                    </div>
                  )}
                  {problem.outputFormat && (
                    <div className="format-box">
                      <strong>Output Format:</strong>
                      <p>{problem.outputFormat}</p>
                    </div>
                  )}
                </section>
              )}

              {/* Public Sample Test Cases */}
              {problem.sampleTestCases && problem.sampleTestCases.length > 0 && (
                <section className="section-card">
                  <h2 className="section-title">
                    <CheckCircle2 size={18} /> Public Sample Test Cases
                  </h2>
                  <div className="sample-tests-grid">
                    {problem.sampleTestCases.map((tc, idx) => (
                      <div key={idx} className="test-case-card">
                        <div className="test-case-header">
                          <span>Sample Case #{tc.sequenceOrder || idx + 1}</span>
                        </div>
                        <div className="test-case-body">
                          <div className="test-case-field">
                            <label>Input:</label>
                            <pre>{tc.inputData}</pre>
                          </div>
                          <div className="test-case-field">
                            <label>Expected Output:</label>
                            <pre>{tc.expectedOutput}</pre>
                          </div>
                          {tc.explanation && (
                            <div className="test-case-explanation">
                              <label>Explanation:</label>
                              <p>{tc.explanation}</p>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </section>
              )}
            </>
          ) : (
            /* Submissions List for this problem */
            <section className="section-card">
              <h2 className="section-title">
                <History size={18} /> Your Submissions for {problem.title}
              </h2>
              {submissions.length === 0 ? (
                <p className="text-muted py-3">No submissions yet for this problem. Submit your solution on the right panel!</p>
              ) : (
                <div className="problem-submissions-list">
                  {submissions.map((sub) => (
                    <div key={sub.id} className="problem-submission-item">
                      <div className="sub-item-header">
                        <StatusBadge status={sub.status} size="sm" />
                        <span className="text-muted text-xs">
                          {new Date(sub.createdAt).toLocaleString()}
                        </span>
                      </div>
                      <span className="font-mono text-xs text-muted">ID: {sub.id.substring(0, 8)}...</span>
                    </div>
                  ))}
                </div>
              )}
            </section>
          )}
        </div>

        {/* Right Column: Monaco Code Editor */}
        <div className={`problem-editor-panel ${mobileTab !== 'editor' ? 'mobile-hidden' : ''}`}>
          <EditorPanel
            code={code}
            starterCode={getCurrentStarterCode()}
            onCodeChange={handleCodeChange}
            onResetCode={handleResetCode}
            onRunCode={handleRunCode}
            onSubmitCode={handleSubmitCode}
            onOpenMentor={handleOpenMentor}
            onDiagnoseError={handleDiagnoseError}
            lastEvaluation={lastEvaluation}
            onClearEvaluation={() => setLastEvaluation(null)}
            isRunning={isRunning}
            isSubmitting={isSubmitting}
            selectedLanguage={selectedLanguage}
            onLanguageChange={handleLanguageChange}
            supportedLanguages={problem.supportedLanguages || ['JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT']}
            isAutosaved={isAutosaved}
          />
        </div>
      </div>

      {/* SocraBuddy Socratic AI Mentor Modal */}
      <SocraBuddyModal
        isOpen={isMentorOpen}
        onClose={() => setIsMentorOpen(false)}
        problemId={problem.id}
        problemTitle={problem.title}
        sourceCode={code}
        lastErrorMessage={lastEvaluation?.errorMessage}
        lastVerdict={lastEvaluation?.verdict}
        initialMode={mentorInitialMode}
      />
    </div>
  );
};

export default ProblemDetailPage;
