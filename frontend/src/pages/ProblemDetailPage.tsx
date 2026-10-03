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
  Play,
  Send,
  Loader2,
  Sparkles,
  X,
  Zap,
  Bot,
  Bug,
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
import { ToastNotification } from '../components/ToastNotification';
import { SubmissionResultView } from '../components/SubmissionResultView';
import { AiMentorPanel } from '../components/AiMentorPanel';
import { VisualDebuggerDrawer } from '../components/debugger/VisualDebuggerDrawer';
import { VisualDebuggerService } from '../services/visualDebuggerService';
import { DebugSession } from '../types/debugger';
import { useAuth } from '../context/AuthContext';

const DEFAULT_JAVA_STARTER = `class Solution {
    // Write your solution here
}
`;

interface ActiveToast {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title?: string;
  message: string;
  verdict?: string | null;
  duration?: number;
  action?: {
    label: string;
    onClick: () => void;
  };
}

// Helper components for clean, compact LeetCode-style problem rendering
const FormattedText: React.FC<{ text: string }> = ({ text }) => {
  if (!text) return null;
  const parts = text.split(/(`[^`]+`|\*\*[^*]+\*\*)/g);
  return (
    <>
      {parts.map((part, idx) => {
        if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
          return (
            <code key={idx} className="statement-inline-code">
              {part.slice(1, -1)}
            </code>
          );
        }
        if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
          return (
            <strong key={idx} className="statement-bold">
              {part.slice(2, -2)}
            </strong>
          );
        }
        return part;
      })}
    </>
  );
};

const FormattedExamples: React.FC<{ rawExamples?: string | null }> = ({ rawExamples }) => {
  if (!rawExamples) return null;

  const exampleBlocks = rawExamples
    .split(/(?=Example\s+\d+:?)/i)
    .map((s) => s.trim())
    .filter(Boolean);

  if (exampleBlocks.length === 0) {
    return (
      <div className="statement-example-box">
        <pre className="example-raw-code">{rawExamples}</pre>
      </div>
    );
  }

  return (
    <div className="statement-examples-list">
      {exampleBlocks.map((block, idx) => {
        const lines = block.split('\n').map((l) => l.trim()).filter(Boolean);
        const titleMatch = lines[0]?.match(/^(Example\s+\d+:?)/i);
        const title = titleMatch ? titleMatch[1].replace(/:$/, '') : `Example ${idx + 1}`;
        const contentLines = titleMatch ? lines.slice(1) : lines;

        let inputStr = '';
        let outputStr = '';
        let explanationStr = '';
        const otherLines: string[] = [];

        contentLines.forEach((line) => {
          if (/^input:/i.test(line)) {
            inputStr = line.replace(/^input:\s*/i, '');
          } else if (/^output:/i.test(line)) {
            outputStr = line.replace(/^output:\s*/i, '');
          } else if (/^explanation:/i.test(line)) {
            explanationStr = line.replace(/^explanation:\s*/i, '');
          } else {
            otherLines.push(line);
          }
        });

        const hasStructuredFields = inputStr || outputStr;

        return (
          <div key={idx} className="statement-example-box">
            <div className="example-box-header">
              <span className="example-badge">{title}</span>
            </div>
            <div className="example-box-content">
              {hasStructuredFields ? (
                <>
                  {inputStr && (
                    <div className="example-data-row">
                      <span className="example-label">Input:</span>
                      <code className="example-code-val">{inputStr}</code>
                    </div>
                  )}
                  {outputStr && (
                    <div className="example-data-row">
                      <span className="example-label">Output:</span>
                      <code className="example-code-val">{outputStr}</code>
                    </div>
                  )}
                  {explanationStr && (
                    <div className="example-data-row">
                      <span className="example-label">Explanation:</span>
                      <span className="example-text-val">{explanationStr}</span>
                    </div>
                  )}
                  {otherLines.length > 0 && (
                    <div className="example-extra-text">
                      {otherLines.map((ol, i) => (
                        <p key={i}>{ol}</p>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <pre className="example-raw-code">{contentLines.join('\n')}</pre>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};

const FormattedConstraints: React.FC<{
  constraints?: string | null;
  inputFormat?: string | null;
  outputFormat?: string | null;
}> = ({ constraints, inputFormat, outputFormat }) => {
  if (!constraints && !inputFormat && !outputFormat) return null;

  const lines = constraints ? constraints.split('\n').map((l) => l.trim()).filter(Boolean) : [];

  return (
    <div className="statement-section-divider">
      <h3 className="statement-sub-title">
        <HelpCircle size={15} className="text-yellow" />
        <span>Constraints & Rules</span>
      </h3>
      {lines.length > 0 && (
        <ul className="constraints-list">
          {lines.map((c, i) => (
            <li key={i} className="constraint-item">
              <code className="constraint-pill">{c}</code>
            </li>
          ))}
        </ul>
      )}

      {(inputFormat || outputFormat) && (
        <div className="statement-io-grid">
          {inputFormat && (
            <div className="statement-io-item">
              <span className="io-tag">Input Format</span>
              <p className="io-desc">{inputFormat}</p>
            </div>
          )}
          {outputFormat && (
            <div className="statement-io-item">
              <span className="io-tag">Output Format</span>
              <p className="io-desc">{outputFormat}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

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
  const [panelTab, setPanelTab] = useState<'statement' | 'submissions' | 'result' | 'mentor' | 'debugger'>('statement');
  const [selectedResultSubmission, setSelectedResultSubmission] = useState<SubmissionResponse | null>(null);
  const [mobileTab, setMobileTab] = useState<'problem' | 'editor'>('problem');
  const [activeToast, setActiveToast] = useState<ActiveToast | null>(null);

  // Visual Step Debugger State (Side-by-Side with Code Editor)
  const [debugSession, setDebugSession] = useState<DebugSession | null>(null);
  const [activeDebugLine, setActiveDebugLine] = useState<number | null>(null);

  const handleToggleDebugger = () => {
    if (panelTab === 'debugger') {
      setPanelTab('statement');
      setActiveDebugLine(null);
    } else {
      const session = VisualDebuggerService.generateDebugSession(
        code || problem?.starterCode || '',
        problem?.title || 'Algorithm Visualizer',
        problem?.sampleTestCases?.[0]?.inputData || 'nums = [2, 7, 11, 15], target = 9',
        selectedLanguage
      );
      setDebugSession(session);
      setPanelTab('debugger');
      setActiveDebugLine(session.steps[0]?.lineNumber || 1);
    }
  };

  const showToast = (toast: Omit<ActiveToast, 'id'>) => {
    setActiveToast({ ...toast, id: String(Date.now()) });
  };

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

  const handleInspectSubmission = async (subId: string) => {
    try {
      const fullSub = await submissionService.getSubmissionById(subId);
      setSelectedResultSubmission(fullSub);
      setPanelTab('result');
    } catch (err) {
      console.error('Failed to load submission details:', err);
    }
  };

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

    setIsRunning(true);
    try {
      const res = await submissionService.runCode({
        problemId: problem.id,
        sourceCode: code,
        language: selectedLanguage,
      });

      setLastEvaluation(res);

      if (res.verdict === 'ACCEPTED') {
        showToast({
          type: 'success',
          title: 'Sample Test Cases Passed 🎉',
          verdict: 'ACCEPTED',
          message: `All ${res.totalTests || 0} sample test cases executed cleanly in ${res.executionTimeMs || 0}ms.`,
          duration: 4500,
        });
      } else {
        showToast({
          type: 'warning',
          verdict: res.verdict,
          message: `${res.testsPassed || 0}/${res.totalTests || 0} sample tests passed (${res.executionTimeMs || 0}ms). Open the Test Console below for diagnostics.`,
          duration: 5500,
        });
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'Execution failed. Please check your syntax or try again.';
      showToast({
        type: 'error',
        title: 'Execution Failed',
        message: msg,
        duration: 6000,
      });
    } finally {
      setIsRunning(false);
    }
  };

  // Handle full submission
  const handleSubmitCode = async () => {
    if (!problem) return;
    if (!isAuthenticated) {
      showToast({
        type: 'error',
        title: 'Authentication Required',
        message: 'You must be logged in to submit your solution.',
        duration: 5000,
      });
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await submissionService.createSubmission({
        problemId: problem.id,
        sourceCode: code,
        language: selectedLanguage,
      });

      setLastEvaluation(res);
      setSelectedResultSubmission(res);
      fetchSubmissionsForProblem(problem.id);
      setPanelTab('result');

      if (res.verdict === 'ACCEPTED') {
        showToast({
          type: 'success',
          title: 'Submission Accepted 🎉',
          verdict: 'ACCEPTED',
          message: `Congratulations! Solution accepted with all ${res.totalTests || 0} tests passed (${res.executionTimeMs || 0}ms).`,
          duration: 6000,
          action: {
            label: 'View Result',
            onClick: () => setPanelTab('result'),
          },
        });
      } else {
        showToast({
          type: 'error',
          title: 'Submission Evaluated',
          verdict: res.verdict,
          message: `Verdict: ${res.verdict || res.status} (${res.testsPassed || 0}/${res.totalTests || 0} tests passed). Need help? Ask SocraBuddy!`,
          duration: 6000,
          action: {
            label: 'Ask SocraBuddy',
            onClick: () => {
              setMentorInitialMode('diagnose');
              setIsMentorOpen(true);
            },
          },
        });
      }
    } catch (err: unknown) {
      showToast({
        type: 'error',
        title: 'Submission Failed',
        message: err instanceof Error ? err.message : 'Submission failed. Please try again.',
        duration: 6000,
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
        {/* Left Column: Problem Statement, Submissions & LeetCode-style Result Tab */}
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

            {/* Sub-tabs: Statement vs Result vs Submissions */}
            <div className="panel-subtabs">
              <button
                type="button"
                className={`panel-subtab-btn ${panelTab === 'statement' ? 'active' : ''}`}
                onClick={() => setPanelTab('statement')}
              >
                <FileText size={14} />
                <span>Description</span>
              </button>

              {selectedResultSubmission && (
                <div className="tab-pill-with-close">
                  <button
                    type="button"
                    className={`panel-subtab-btn result-tab-pill ${panelTab === 'result' ? 'active' : ''} ${
                      selectedResultSubmission.verdict === 'ACCEPTED' ? 'tab-accepted' : 'tab-failed'
                    }`}
                    onClick={() => setPanelTab('result')}
                  >
                    {selectedResultSubmission.verdict === 'ACCEPTED' ? (
                      <CheckCircle2 size={13} className="text-success" />
                    ) : (
                      <Zap size={13} className="text-warning" />
                    )}
                    <span>{selectedResultSubmission.verdict === 'ACCEPTED' ? 'Accepted' : 'Result'}</span>
                  </button>
                  <button
                    type="button"
                    className="btn-tab-close"
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedResultSubmission(null);
                      if (panelTab === 'result') setPanelTab('statement');
                    }}
                    title="Close result tab"
                  >
                    <X size={12} />
                  </button>
                </div>
              )}

              <button
                type="button"
                className={`panel-subtab-btn ${panelTab === 'submissions' ? 'active' : ''}`}
                onClick={() => setPanelTab('submissions')}
              >
                <History size={14} />
                <span>Submissions</span>
                <span className="subtab-count-badge">{submissions.length}</span>
              </button>

              <button
                type="button"
                className={`panel-subtab-btn mentor-tab-btn ${panelTab === 'mentor' ? 'active' : ''}`}
                onClick={() => setPanelTab('mentor')}
                title="View Socratic progressive hints on left workspace"
              >
                <Sparkles size={14} className="text-yellow" />
                <span>AI Hints</span>
              </button>

              <button
                type="button"
                className={`panel-subtab-btn debugger-tab-btn ${panelTab === 'debugger' ? 'active' : ''}`}
                onClick={handleToggleDebugger}
                title="Live Step Debugger & Visual Memory Canvas (Side-by-side with Editor)"
                data-testid="left-tab-visualizer-btn"
              >
                <Bug size={14} className="text-yellow" />
                <span>Visualizer</span>
              </button>
            </div>
          </div>

          {panelTab === 'debugger' && debugSession ? (
            /* Dedicated Left-Panel Step Debugger & Memory Canvas */
            <section className="section-card debugger-workspace-card" data-testid="debugger-workspace-card">
              <VisualDebuggerDrawer
                session={debugSession}
                onClose={() => {
                  setPanelTab('statement');
                  setActiveDebugLine(null);
                }}
                onStepChange={(step) => setActiveDebugLine(step.lineNumber)}
              />
            </section>
          ) : panelTab === 'mentor' ? (
            /* Dedicated Left-Panel AI Socratic Guidance Tab */
            <section className="section-card mentor-workspace-card">
              <div className="section-header-top flex justify-between items-center mb-3">
                <h2 className="section-title mb-0">
                  <Sparkles size={18} className="text-yellow" /> SocraBuddy Socratic Guidance
                </h2>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handleOpenMentor}
                  title="Open ChatGPT-style dialogue modal"
                >
                  <Bot size={13} />
                  <span>Interactive Chat</span>
                </button>
              </div>

              <AiMentorPanel
                submission={
                  lastEvaluation || {
                    id: '',
                    userId: '',
                    problemId: problem.id,
                    language: selectedLanguage,
                    sourceCode: code || problem.starterCode || '',
                    status: 'COMPLETED',
                    verdict: 'WRONG_ANSWER',
                    testsPassed: 0,
                    totalTests: problem.sampleTestCases?.length || 1,
                    executionTimeMs: 0,
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString(),
                  }
                }
                onOpenFullModal={handleOpenMentor}
              />
            </section>
          ) : panelTab === 'result' && selectedResultSubmission ? (
            /* LeetCode-style Submission Result with Percentiles & Distribution Graph */
            <section className="section-card result-container-card">
              <SubmissionResultView
                submission={selectedResultSubmission}
                problemTitle={problem.title}
                problemSlug={problem.slug}
                onAskSocraBuddy={handleDiagnoseError}
                onBackToSubmissions={() => setPanelTab('submissions')}
              />
            </section>
          ) : panelTab === 'statement' ? (
            /* Unified LeetCode-style Problem Description Card */
            <section className="section-card problem-description-card">
              {/* Problem Description Body */}
              <div className="problem-description-content">
                {problem.description.split('\n\n').map((para, i) => (
                  <p key={i}>
                    <FormattedText text={para} />
                  </p>
                ))}
              </div>

              {/* Examples */}
              {problem.examples && (
                <div className="statement-section-divider">
                  <h3 className="statement-sub-title">
                    <Layers size={15} className="text-accent" />
                    <span>Examples</span>
                  </h3>
                  <FormattedExamples rawExamples={problem.examples} />
                </div>
              )}

              {/* Constraints & Formats */}
              <FormattedConstraints
                constraints={problem.constraints}
                inputFormat={problem.inputFormat}
                outputFormat={problem.outputFormat}
              />

              {/* Public Sample Test Cases */}
              {problem.sampleTestCases && problem.sampleTestCases.length > 0 && (
                <div className="statement-section-divider">
                  <div className="statement-samples-head">
                    <h3 className="statement-sub-title">
                      <CheckCircle2 size={15} className="text-success" />
                      <span>Public Sample Test Cases</span>
                    </h3>
                    <span className="sample-badge-count">{problem.sampleTestCases.length} Cases</span>
                  </div>
                  <div className="statement-samples-list">
                    {problem.sampleTestCases.map((tc, idx) => (
                      <div key={idx} className="statement-sample-row">
                        <div className="sample-row-header">
                          <span className="sample-order-pill">Case #{tc.sequenceOrder || idx + 1}</span>
                          {tc.explanation && (
                            <span className="sample-explanation-preview">{tc.explanation}</span>
                          )}
                        </div>
                        <div className="sample-io-split">
                          <div className="sample-io-box">
                            <span className="sample-io-type">Input</span>
                            <pre className="sample-io-pre">{tc.inputData}</pre>
                          </div>
                          <div className="sample-io-box">
                            <span className="sample-io-type">Expected Output</span>
                            <pre className="sample-io-pre">{tc.expectedOutput}</pre>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </section>
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
                  {submissions.map((sub) => {
                    const getVerdictBadgeClass = (verdict?: string | null) => {
                      switch (verdict) {
                        case 'ACCEPTED':
                          return 'verdict-badge verdict-accepted';
                        case 'WRONG_ANSWER':
                          return 'verdict-badge verdict-wrong';
                        case 'COMPILE_ERROR':
                          return 'verdict-badge verdict-compile';
                        case 'RUNTIME_ERROR':
                          return 'verdict-badge verdict-runtime';
                        case 'TIME_LIMIT_EXCEEDED':
                          return 'verdict-badge verdict-timeout';
                        case 'MEMORY_LIMIT_EXCEEDED':
                          return 'verdict-badge verdict-memory';
                        default:
                          return 'verdict-badge';
                      }
                    };

                    const formatVerdictLabel = (verdict?: string | null) => {
                      if (!verdict) return 'Pending';
                      return verdict
                        .split('_')
                        .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
                        .join(' ');
                    };

                    return (
                      <div
                        key={sub.id}
                        className="problem-submission-item problem-sub-interactive"
                        onClick={() => handleInspectSubmission(sub.id)}
                        title="Click to view full execution graph and code"
                      >
                        <div className="sub-item-header">
                          <div className="sub-item-verdict-row">
                            {sub.verdict ? (
                              <span className={getVerdictBadgeClass(sub.verdict)}>
                                {formatVerdictLabel(sub.verdict)}
                              </span>
                            ) : (
                              <StatusBadge status={sub.status} size="sm" />
                            )}
                            {sub.totalTests != null && sub.totalTests > 0 && (
                              <span className="text-xs text-muted">
                                ({sub.testsPassed || 0}/{sub.totalTests} passed)
                              </span>
                            )}
                            {sub.executionTimeMs != null && (
                              <span className="text-xs text-muted">
                                {sub.executionTimeMs} ms
                              </span>
                            )}
                          </div>
                          <span className="text-muted text-xs">
                            {new Date(sub.createdAt).toLocaleString()}
                          </span>
                        </div>
                        <div className="sub-item-footer">
                          <span className="lang-tag">{sub.language || 'JAVA'}</span>
                          <span className="view-sub-prompt font-mono text-xs text-accent">
                            View Result & Graph →
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          )}
        </div>

        {/* Right Column: Monaco Code Editor */}
        <div className={`problem-editor-panel ${mobileTab !== 'editor' ? 'mobile-hidden' : ''}`}>
          <EditorPanel
            problemId={problem.id}
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
            sampleTestCases={problem.sampleTestCases}
            isAutosaved={isAutosaved}
            debugActiveLine={panelTab === 'debugger' ? activeDebugLine : null}
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

      {/* Floating Animated Toast Feedback Container */}
      {activeToast && (
        <div className="toast-container-fixed">
          <ToastNotification
            key={activeToast.id}
            type={activeToast.type}
            title={activeToast.title}
            message={activeToast.message}
            verdict={activeToast.verdict}
            duration={activeToast.duration}
            action={activeToast.action}
            onClose={() => setActiveToast(null)}
          />
        </div>
      )}
    </div>
  );
};

export default ProblemDetailPage;
