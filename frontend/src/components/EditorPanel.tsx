import React, { useState, useEffect, useMemo } from 'react';
import {
  Code,
  RotateCcw,
  Check,
  Info,
  Sparkles,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Clock,
  HelpCircle,
  Terminal,
  ChevronDown,
  ChevronUp,
  Lightbulb,
  Bot,
  Loader2,
  Play,
} from 'lucide-react';
import { CodeEditor } from './CodeEditor';
import { AiMentorPanel } from './AiMentorPanel';
import { SubmissionResponse, TestCaseEvaluationResult } from '../types/submission';
import { mentorService } from '../services/mentorService';
import { InlineAssistResponse } from '../types/mentor';
import { ComplexityRadarService } from '../services/complexityRadarService';
import { ComplexityRadarHud } from './ComplexityRadarHud';

export interface EditorPanelProps {
  problemId?: string;
  code: string;
  starterCode: string;
  onCodeChange: (newCode: string) => void;
  onResetCode: () => void;
  onSubmitCode?: () => Promise<void>;
  onRunCode?: () => Promise<void>;
  onOpenMentor?: () => void;
  onDiagnoseError?: () => void;
  lastEvaluation?: SubmissionResponse | null;
  onClearEvaluation?: () => void;
  isRunning?: boolean;
  isSubmitting?: boolean;
  language?: string;
  selectedLanguage?: string;
  onLanguageChange?: (language: string) => void;
  supportedLanguages?: string[];
  sampleTestCases?: Array<{
    inputData: string;
    expectedOutput: string;
    sequenceOrder?: number;
    explanation?: string | null;
  }>;
  isAutosaved?: boolean;
  debugActiveLine?: number | null;
}

export const EditorPanel: React.FC<EditorPanelProps> = ({
  problemId,
  code,
  starterCode: _starterCode,
  onCodeChange,
  onResetCode,
  onSubmitCode: _onSubmitCode,
  onRunCode: _onRunCode,
  onOpenMentor,
  onDiagnoseError: _onDiagnoseError,
  lastEvaluation,
  onClearEvaluation,
  isRunning = false,
  isSubmitting = false,
  language = 'Java 17',
  selectedLanguage = 'JAVA',
  onLanguageChange,
  supportedLanguages = ['JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT'],
  sampleTestCases = [],
  isAutosaved = true,
  debugActiveLine = null,
}) => {
  const [feedbackNotice, setFeedbackNotice] = useState<string | null>(null);
  const [showResetConfirm, setShowResetConfirm] = useState(false);
  const [selectedTestCaseTab, setSelectedTestCaseTab] = useState<number>(0);
  const [isDrawerCollapsed, setIsDrawerCollapsed] = useState<boolean>(false);
  const [consoleTab, setConsoleTab] = useState<'cases' | 'hints'>('cases');

  // Live Socratic Inline Mentor State
  const [isLiveMentorEnabled, setIsLiveMentorEnabled] = useState<boolean>(() => {
    try {
      const saved = localStorage.getItem('codingfriend_live_mentor_enabled');
      return saved !== null ? saved === 'true' : true;
    } catch {
      return true;
    }
  });
  const [isLiveMentorLoading, setIsLiveMentorLoading] = useState<boolean>(false);
  const [inlineNudge, setInlineNudge] = useState<InlineAssistResponse | null>(null);
  const [cursorLine, setCursorLine] = useState<number>(1);
  const debounceTimerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  // Real-Time Complexity & Cognitive Detour Radar Analysis
  const radarAnalysis = useMemo(() => {
    return ComplexityRadarService.analyzeCode(code, selectedLanguage || language);
  }, [code, selectedLanguage, language]);


  // Trigger Socratic Micro-Nudge
  const triggerInlineNudge = React.useCallback(
    async (targetLine: number, currentCode: string) => {
      if (!problemId || !currentCode.trim()) return;

      setIsLiveMentorLoading(true);
      try {
        const res = await mentorService.getInlineAssist({
          problemId,
          sourceCode: currentCode,
          cursorLine: targetLine,
          language: selectedLanguage,
        });
        if (res && res.inlineMessage) {
          setInlineNudge(res);
        }
      } catch {
        // Silently fail non-critical live hints
      } finally {
        setIsLiveMentorLoading(false);
      }
    },
    [problemId, selectedLanguage]
  );

  // Debounce typing when live mentor is active
  useEffect(() => {
    if (!isLiveMentorEnabled || !problemId || !code.trim()) {
      return;
    }

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    debounceTimerRef.current = setTimeout(() => {
      triggerInlineNudge(cursorLine, code);
    }, 1800);

    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, [code, cursorLine, isLiveMentorEnabled, problemId, triggerInlineNudge]);

  // Keyboard shortcut listener: Alt + / or Escape
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setInlineNudge(null);
      } else if (e.altKey && (e.key === '/' || e.code === 'Slash')) {
        e.preventDefault();
        triggerInlineNudge(cursorLine, code);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [cursorLine, code, triggerInlineNudge]);

  // Automatically expand results drawer and set active tab when a new evaluation arrives
  useEffect(() => {
    if (lastEvaluation) {
      setIsDrawerCollapsed(false);
      setSelectedTestCaseTab(0);
      setConsoleTab('cases');
    }
  }, [lastEvaluation]);

  const getMonacoLang = (lang: string) => {
    switch (lang.toUpperCase()) {
      case 'PYTHON':
      case 'PY':
        return 'python';
      case 'CPP':
      case 'C++':
        return 'cpp';
      case 'JAVASCRIPT':
      case 'JS':
        return 'javascript';
      case 'JAVA':
      default:
        return 'java';
    }
  };

  let testCasesList: TestCaseEvaluationResult[] = [];
  if (lastEvaluation?.evaluationDetails) {
    try {
      testCasesList = JSON.parse(lastEvaluation.evaluationDetails);
    } catch {
      testCasesList = [];
    }
  }

  const confirmReset = () => {
    onResetCode();
    setShowResetConfirm(false);
    setFeedbackNotice('Code reset to starter template.');
    setTimeout(() => setFeedbackNotice(null), 3000);
  };

  // Reset / Refresh Test Cases to initial state
  const handleResetTestCases = () => {
    if (onClearEvaluation) {
      onClearEvaluation();
    }
    setSelectedTestCaseTab(0);
    setConsoleTab('cases');
    setFeedbackNotice('Test cases reset to initial ready state.');
    setTimeout(() => setFeedbackNotice(null), 3000);
  };

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

  const getLanguageDisplayName = (lang: string) => {
    switch (lang.toUpperCase()) {
      case 'JAVA':
        return 'Java 17';
      case 'PYTHON':
        return 'Python 3';
      case 'CPP':
        return 'C++ 20';
      case 'JAVASCRIPT':
        return 'JavaScript';
      default:
        return lang;
    }
  };

  return (
    <div className="editor-panel-container" data-testid="editor-panel">
      {/* Editor Header Toolbar */}
      <div className="editor-panel-header">
        <div className="editor-panel-left">
          {onLanguageChange ? (
            <div className="lang-selector-wrapper">
              <Code size={14} className="lang-icon-prefix" />
              <select
                className="language-selector-dropdown"
                value={selectedLanguage}
                onChange={(e) => onLanguageChange(e.target.value)}
                data-testid="language-selector-dropdown"
                title="Select programming language"
              >
                {supportedLanguages.map((lang) => (
                  <option key={lang} value={lang} className="lang-option">
                    {getLanguageDisplayName(lang)}
                  </option>
                ))}
              </select>
            </div>
          ) : (
            <span className="editor-lang-tag">
              <Code size={14} /> {language}
            </span>
          )}

          <span className="autosave-status" title="Code changes are automatically preserved locally">
            {isAutosaved ? (
              <>
                <Check size={13} className="autosave-icon saved" />
                <span>Saved</span>
              </>
            ) : (
              <>
                <span className="autosave-dot saving" />
                <span>Saving...</span>
              </>
            )}
          </span>
        </div>

        <div className="editor-panel-actions">
          {/* Cognitive Detour & Complexity Radar Chip */}
          <ComplexityRadarHud
            analysis={radarAnalysis}
            onAskMentorAdvice={(_msg) => onOpenMentor && onOpenMentor()}
          />

          {/* Live Socratic AI Mentor Toggle */}
          <button
            type="button"
            className={`btn-toolbar-live-mentor ${isLiveMentorEnabled ? 'active' : ''}`}
            onClick={() => {
              const next = !isLiveMentorEnabled;
              setIsLiveMentorEnabled(next);
              try {
                localStorage.setItem('codingfriend_live_mentor_enabled', String(next));
              } catch {
                // Ignore storage error
              }
              if (!next) {
                setInlineNudge(null);
              } else {
                triggerInlineNudge(cursorLine, code);
              }
            }}
            title={
              isLiveMentorEnabled
                ? 'Live AI Mentor active. Click to disable (Alt + /)'
                : 'Click to enable real-time Socratic hints as you type (Alt + /)'
            }
            data-testid="live-mentor-toggle-btn"
          >
            <Sparkles size={13} className={isLiveMentorEnabled ? 'sparkle-icon active' : 'sparkle-icon'} />
            <span className="live-mentor-text">Live Mentor</span>
            <span className={`live-mentor-pill ${isLiveMentorEnabled ? 'pill-on' : 'pill-off'}`}>
              {isLiveMentorEnabled ? 'ON' : 'OFF'}
            </span>
          </button>

          {showResetConfirm ? (
            <div className="reset-confirm-group">
              <span className="reset-confirm-text">Reset code?</span>
              <button
                type="button"
                className="btn-link-danger"
                onClick={confirmReset}
                data-testid="confirm-reset-btn"
              >
                Yes, Reset
              </button>
              <button
                type="button"
                className="btn-link-cancel"
                onClick={() => setShowResetConfirm(false)}
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="btn-toolbar-reset"
              onClick={() => setShowResetConfirm(true)}
              title="Reset code to original starter template"
              data-testid="reset-code-btn"
            >
              <RotateCcw size={13} />
              <span>Reset</span>
            </button>
          )}
        </div>
      </div>

      {/* Editor Body */}
      <div className="editor-panel-body">
        <CodeEditor
          code={code}
          onChange={onCodeChange}
          onCursorChange={(line) => setCursorLine(line)}
          language={getMonacoLang(selectedLanguage || language)}
          height={!isDrawerCollapsed ? '280px' : '480px'}
          inlineNudge={isLiveMentorEnabled ? inlineNudge : null}
          onDismissNudge={() => setInlineNudge(null)}
          onAskMoreMentor={onOpenMentor}
          debugActiveLine={debugActiveLine}
        />
      </div>


      {/* Real-time Live Mentor Thinking Status Indicator */}
      {isLiveMentorEnabled && isLiveMentorLoading && !inlineNudge && (
        <div className="editor-nudge-loading-indicator">
          <Sparkles size={12} className="spin-slow text-yellow" />
          <span>SocraBuddy is reviewing line {cursorLine}...</span>
        </div>
      )}

      {/* Action Notification Banner */}
      {feedbackNotice && (
        <div className="editor-feedback-banner" data-testid="editor-feedback-banner">
          <Info size={15} />
          <span>{feedbackNotice}</span>
        </div>
      )}

      {/* Collapsed Console Bar (When minimized) */}
      {isDrawerCollapsed && (
        <div className="drawer-collapsed-bar" data-testid="drawer-collapsed-bar">
          <div className="collapsed-bar-info">
            <Terminal size={14} className="text-yellow" />
            {lastEvaluation ? (
              <>
                <span className="collapsed-label">Latest Result:</span>
                <span className={getVerdictBadgeClass(lastEvaluation.verdict)}>
                  {lastEvaluation.verdict || lastEvaluation.status}
                </span>
                <span className="text-xs text-muted">
                  ({lastEvaluation.testsPassed}/{lastEvaluation.totalTests} passed)
                </span>
              </>
            ) : (
              <>
                <span className="collapsed-label">Test Cases Console:</span>
                <span className="text-xs text-muted">
                  {sampleTestCases.length > 0 ? `${sampleTestCases.length} sample cases ready` : 'Ready to run'}
                </span>
              </>
            )}
          </div>

          <button
            type="button"
            className="btn-reopen-drawer"
            onClick={() => setIsDrawerCollapsed(false)}
            title="Expand Test Cases Console"
            data-testid="expand-console-btn"
          >
            <span>Open Console</span>
            <ChevronUp size={13} />
          </button>
        </div>
      )}

      {/* Always Present Test Cases & Results Console Drawer */}
      {!isDrawerCollapsed && (
        <div className="execution-results-drawer" data-testid="execution-results-drawer">
          {/* Drawer Header Toolbar */}
          <div className="drawer-header">
            <div className="drawer-title-row">
              <div className="drawer-console-tabs-group">
                <button
                  type="button"
                  className={`drawer-console-tab-btn ${consoleTab === 'cases' ? 'active' : ''}`}
                  onClick={() => setConsoleTab('cases')}
                  data-testid="console-tab-cases"
                >
                  <Terminal size={14} />
                  <span>{lastEvaluation ? 'Test Result' : 'Testcase'}</span>
                </button>

                <button
                  type="button"
                  className={`drawer-console-tab-btn mentor-tab ${consoleTab === 'hints' ? 'active' : ''}`}
                  onClick={() => setConsoleTab('hints')}
                  data-testid="console-tab-hints"
                >
                  <Sparkles size={14} className="text-yellow" />
                  <span>Socratic Hints</span>
                  {lastEvaluation && lastEvaluation.verdict !== 'ACCEPTED' && (
                    <span className="console-tab-hint-pill">Tiers 1-5</span>
                  )}
                </button>
              </div>

              <div className="drawer-actions-group">
                {/* Reset / Refresh Test Cases Button */}
                <button
                  type="button"
                  className="btn-drawer-icon"
                  onClick={handleResetTestCases}
                  title="Reset test cases and clear evaluation state"
                  data-testid="reset-testcases-btn"
                >
                  <RotateCcw size={13} />
                  <span className="btn-drawer-text">Reset Cases</span>
                </button>

                {/* Minimize Console */}
                <button
                  type="button"
                  className="btn-drawer-icon"
                  onClick={() => setIsDrawerCollapsed(true)}
                  title="Minimize Console"
                  data-testid="minimize-console-btn"
                >
                  <ChevronDown size={14} />
                </button>
              </div>
            </div>
          </div>

          {/* Drawer Body */}
          <div className="drawer-body">
            {isRunning || isSubmitting ? (
              /* Execution in Progress Loading State */
              <div className="console-running-indicator" data-testid="console-running-indicator">
                <Loader2 size={18} className="spinner-icon text-yellow" />
                <div className="running-text-group">
                  <strong>Executing code in Java 17 isolated sandbox...</strong>
                  <span>Testing assertions against sample test cases</span>
                </div>
              </div>
            ) : consoleTab === 'cases' ? (
              lastEvaluation ? (
                /* Evaluated Test Results State */
                <>
                  {/* Verdict & Meta Bar */}
                  <div className="console-verdict-bar">
                    <div className="verdict-bar-left">
                      <span className={`console-verdict-title ${getVerdictBadgeClass(lastEvaluation.verdict)}`}>
                        {lastEvaluation.verdict === 'ACCEPTED' && <CheckCircle2 size={16} />}
                        {lastEvaluation.verdict === 'WRONG_ANSWER' && <XCircle size={16} />}
                        {lastEvaluation.verdict === 'COMPILE_ERROR' && <AlertTriangle size={16} />}
                        {lastEvaluation.verdict === 'RUNTIME_ERROR' && <AlertTriangle size={16} />}
                        {lastEvaluation.verdict === 'TIME_LIMIT_EXCEEDED' && <Clock size={16} />}
                        <span>
                          {lastEvaluation.verdict === 'COMPILE_ERROR'
                            ? 'Compile Error'
                            : lastEvaluation.verdict === 'WRONG_ANSWER'
                            ? 'Wrong Answer'
                            : lastEvaluation.verdict === 'ACCEPTED'
                            ? 'Accepted'
                            : lastEvaluation.verdict === 'TIME_LIMIT_EXCEEDED'
                            ? 'Time Limit Exceeded'
                            : lastEvaluation.verdict === 'RUNTIME_ERROR'
                            ? 'Runtime Error'
                            : lastEvaluation.verdict || lastEvaluation.status}
                        </span>
                      </span>

                      <div className="console-verdict-meta">
                        {lastEvaluation.totalTests > 0 && (
                          <span className="meta-item">
                            {lastEvaluation.testsPassed} / {lastEvaluation.totalTests} testcases passed
                          </span>
                        )}
                        {lastEvaluation.executionTimeMs !== undefined && (
                          <span className="meta-item">
                            Runtime: <strong>{lastEvaluation.executionTimeMs} ms</strong>
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* If Compilation Error */}
                  {lastEvaluation.verdict === 'COMPILE_ERROR' && lastEvaluation.errorMessage && (
                    <div className="console-terminal-block">
                      <pre className="console-code-pre">{lastEvaluation.errorMessage}</pre>
                    </div>
                  )}

                  {/* If Test Cases evaluated */}
                  {testCasesList.length > 0 && (
                    <div className="test-cases-viewer">
                      <div className="test-case-tabs">
                        {testCasesList.map((tc, idx) => (
                          <button
                            key={idx}
                            type="button"
                            className={`test-tab-btn ${selectedTestCaseTab === idx ? 'active' : ''} ${
                              tc.passed ? 'passed' : 'failed'
                            }`}
                            onClick={() => setSelectedTestCaseTab(idx)}
                          >
                            <span className="tab-indicator">{tc.passed ? '✓' : '✗'}</span>
                            <span>Case {tc.testNumber || idx + 1}</span>
                            {tc.isHidden && <span className="tab-hidden-badge">Hidden</span>}
                          </button>
                        ))}
                      </div>

                      {testCasesList[selectedTestCaseTab] && (
                        <div className="selected-case-details">
                          {testCasesList[selectedTestCaseTab].isHidden ? (
                            <div className="hidden-test-shield">
                              <HelpCircle size={16} className="text-muted" />
                              <span>
                                Hidden grading test case. Inputs and assertions are protected from client inspection.
                              </span>
                            </div>
                          ) : (
                            <div className="public-case-grid">
                              <div className="case-field-col">
                                <label className="field-label">Input</label>
                                <pre className="field-pre">{testCasesList[selectedTestCaseTab].input}</pre>
                              </div>
                              <div className="case-field-col">
                                <label className="field-label">Expected Output</label>
                                <pre className="field-pre">{testCasesList[selectedTestCaseTab].expectedOutput}</pre>
                              </div>
                              <div className="case-field-col">
                                <label className="field-label">Actual Output</label>
                                <pre
                                  className={`field-pre ${
                                    testCasesList[selectedTestCaseTab].passed ? 'text-green' : 'text-red'
                                  }`}
                                >
                                  {testCasesList[selectedTestCaseTab].actualOutput || '(No output)'}
                                </pre>
                              </div>
                            </div>
                          )}

                          {!testCasesList[selectedTestCaseTab].isHidden && testCasesList[selectedTestCaseTab].errorMessage && (
                            <div className="case-error-row">
                              <label className="field-label text-red">Error Message</label>
                              <pre className="field-pre text-red">
                                {testCasesList[selectedTestCaseTab].errorMessage}
                              </pre>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Minimal Inline Socratic Assist Card */}
                  {lastEvaluation.verdict !== 'ACCEPTED' && (
                    <div className="console-inline-help-card">
                      <div className="inline-help-info">
                        <Sparkles size={14} className="text-yellow" />
                        <span>
                          {lastEvaluation.verdict === 'COMPILE_ERROR'
                            ? 'Stuck on this compiler error? SocraBuddy can guide you.'
                            : 'Want guidance without spoiling the solution?'}
                        </span>
                      </div>
                      <div className="inline-help-actions">
                        <button
                          type="button"
                          className="btn-inline-help"
                          onClick={() => setConsoleTab('hints')}
                        >
                          <Lightbulb size={13} />
                          <span>View Progressive Hints</span>
                        </button>
                        {onOpenMentor && (
                          <button
                            type="button"
                            className="btn-inline-help-chat"
                            onClick={onOpenMentor}
                          >
                            <Bot size={13} />
                            <span>Interactive Chat</span>
                          </button>
                        )}
                      </div>
                    </div>
                  )}
                </>
              ) : (
                /* Initial Ready State / Fresh Sample Test Cases */
                <div className="console-initial-cases-view">
                  <div className="initial-cases-banner">
                    <div className="cases-banner-info">
                      <Terminal size={15} className="text-accent" />
                      <span className="cases-banner-title">Sample Test Cases</span>
                      <span className="cases-banner-badge">Ready to Run</span>
                    </div>
                    <span className="cases-banner-sub">
                      Click <strong>Run</strong> above to execute against these sample cases.
                    </span>
                  </div>

                  {sampleTestCases.length > 0 ? (
                    <div className="test-cases-viewer">
                      <div className="test-case-tabs">
                        {sampleTestCases.map((tc, idx) => (
                          <button
                            key={idx}
                            type="button"
                            className={`test-tab-btn ${selectedTestCaseTab === idx ? 'active' : ''}`}
                            onClick={() => setSelectedTestCaseTab(idx)}
                          >
                            <span className="tab-indicator font-mono">#{idx + 1}</span>
                            <span>Case {tc.sequenceOrder || idx + 1}</span>
                          </button>
                        ))}
                      </div>

                      {sampleTestCases[selectedTestCaseTab] && (
                        <div className="selected-case-details">
                          <div className="public-case-grid">
                            <div className="case-field-col">
                              <label className="field-label">Input</label>
                              <pre className="field-pre">{sampleTestCases[selectedTestCaseTab].inputData}</pre>
                            </div>
                            <div className="case-field-col">
                              <label className="field-label">Expected Output</label>
                              <pre className="field-pre">{sampleTestCases[selectedTestCaseTab].expectedOutput}</pre>
                            </div>
                            <div className="case-field-col">
                              <label className="field-label">Actual Output</label>
                              <pre className="field-pre text-placeholder">
                                (Click &lsquo;Run&rsquo; to evaluate)
                              </pre>
                            </div>
                          </div>

                          {sampleTestCases[selectedTestCaseTab].explanation && (
                            <div className="case-explanation-row">
                              <label className="field-label">Explanation</label>
                              <p className="case-explanation-text">
                                {sampleTestCases[selectedTestCaseTab].explanation}
                              </p>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="empty-cases-placeholder">
                      <Play size={16} className="text-muted" />
                      <span>Click <strong>Run</strong> to execute your code against sandbox test cases.</span>
                    </div>
                  )}
                </div>
              )
            ) : (
              /* Focused AI Mentor Tab */
              <AiMentorPanel
                submission={
                  lastEvaluation || {
                    id: '',
                    userId: '',
                    problemId: problemId || '',
                    language: selectedLanguage,
                    sourceCode: code,
                    status: 'COMPLETED',
                    verdict: 'WRONG_ANSWER',
                    testsPassed: 0,
                    totalTests: sampleTestCases.length || 1,
                    executionTimeMs: 0,
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString(),
                  }
                }
                onOpenFullModal={onOpenMentor}
              />
            )}
          </div>
        </div>
      )}

      {/* Editor Footer Status Bar */}
      <div className="editor-panel-footer">
        <div className="editor-footer-hint">
          <Sparkles size={14} className="sparkle-icon text-yellow" />
          <span>{getLanguageDisplayName(selectedLanguage)} Sandboxed Execution &bull; SocraBuddy Socratic Guidance</span>
        </div>
        <div className="editor-footer-hint text-xs text-muted">
          <span>Ctrl+Enter to Submit</span>
        </div>
      </div>
    </div>
  );
};
