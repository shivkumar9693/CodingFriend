import React, { useState, useEffect } from 'react';
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
  BrainCircuit,
  HelpCircle,
  Terminal,
  X,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { CodeEditor } from './CodeEditor';
import { AiMentorPanel } from './AiMentorPanel';
import { SubmissionResponse, TestCaseEvaluationResult } from '../types/submission';

export interface EditorPanelProps {
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
  isAutosaved?: boolean;
}

export const EditorPanel: React.FC<EditorPanelProps> = ({
  code,
  starterCode: _starterCode,
  onCodeChange,
  onResetCode,
  onSubmitCode: _onSubmitCode,
  onRunCode,
  onOpenMentor: _onOpenMentor,
  onDiagnoseError,
  lastEvaluation,
  onClearEvaluation,
  isRunning = false,
  isSubmitting: _isSubmitting = false,
  language = 'Java 17',
  selectedLanguage = 'JAVA',
  onLanguageChange,
  supportedLanguages = ['JAVA', 'PYTHON', 'CPP', 'JAVASCRIPT'],
  isAutosaved = true,
}) => {
  const [feedbackNotice, setFeedbackNotice] = useState<string | null>(null);
  const [showResetConfirm, setShowResetConfirm] = useState(false);
  const [selectedTestCaseTab, setSelectedTestCaseTab] = useState<number>(0);
  const [isDrawerVisible, setIsDrawerVisible] = useState<boolean>(true);
  const [isDrawerCollapsed, setIsDrawerCollapsed] = useState<boolean>(false);

  // Automatically show and expand results drawer when a new evaluation arrives
  useEffect(() => {
    if (lastEvaluation) {
      setIsDrawerVisible(true);
      setIsDrawerCollapsed(false);
      setSelectedTestCaseTab(0);
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
          language={getMonacoLang(selectedLanguage || language)}
          height={lastEvaluation && isDrawerVisible && !isDrawerCollapsed ? '280px' : '400px'}
        />
      </div>

      {/* Action Notification Banner */}
      {feedbackNotice && (
        <div className="editor-feedback-banner" data-testid="editor-feedback-banner">
          <Info size={15} />
          <span>{feedbackNotice}</span>
        </div>
      )}

      {/* When Drawer is dismissed/closed, show quick toggle bar */}
      {lastEvaluation && !isDrawerVisible && (
        <div className="drawer-collapsed-bar">
          <div className="collapsed-bar-info">
            <Terminal size={14} className="text-yellow" />
            <span className="collapsed-label">Latest Result:</span>
            <span className={getVerdictBadgeClass(lastEvaluation.verdict)}>
              {lastEvaluation.verdict || lastEvaluation.status}
            </span>
            <span className="text-xs text-muted">
              ({lastEvaluation.testsPassed}/{lastEvaluation.totalTests} passed)
            </span>
          </div>

          <button
            type="button"
            className="btn-reopen-drawer"
            onClick={() => {
              setIsDrawerVisible(true);
              setIsDrawerCollapsed(false);
            }}
            title="Open Execution Results Console"
          >
            <span>Open Console</span>
            <ChevronUp size={13} />
          </button>
        </div>
      )}

      {/* Interactive Execution Results Drawer */}
      {lastEvaluation && isDrawerVisible && (
        <div className="execution-results-drawer" data-testid="execution-results-drawer">
          {/* Drawer Header Toolbar */}
          <div className="drawer-header">
            <div className="drawer-title-row">
              <div className="drawer-heading-group">
                <span className="drawer-heading">
                  <Terminal size={15} /> Test Console
                </span>
                <span className={getVerdictBadgeClass(lastEvaluation.verdict)}>
                  {lastEvaluation.verdict === 'ACCEPTED' && <CheckCircle2 size={13} />}
                  {lastEvaluation.verdict === 'WRONG_ANSWER' && <XCircle size={13} />}
                  {lastEvaluation.verdict === 'COMPILE_ERROR' && <AlertTriangle size={13} />}
                  {lastEvaluation.verdict === 'RUNTIME_ERROR' && <AlertTriangle size={13} />}
                  {lastEvaluation.verdict === 'TIME_LIMIT_EXCEEDED' && <Clock size={13} />}
                  <span>{lastEvaluation.verdict || lastEvaluation.status}</span>
                </span>
              </div>

              <div className="drawer-actions-group">
                {onRunCode && (
                  <button
                    type="button"
                    className="btn-drawer-icon"
                    onClick={onRunCode}
                    disabled={isRunning}
                    title="Re-run code against test cases"
                  >
                    <RotateCcw size={12} />
                    <span>Re-test</span>
                  </button>
                )}

                {onClearEvaluation && (
                  <button
                    type="button"
                    className="btn-drawer-icon reset"
                    onClick={() => {
                      onClearEvaluation();
                      setIsDrawerVisible(false);
                      setSelectedTestCaseTab(0);
                    }}
                    title="Reset test case results to fresh state"
                  >
                    <RotateCcw size={12} />
                    <span>Clear</span>
                  </button>
                )}

                <button
                  type="button"
                  className="btn-drawer-icon"
                  onClick={() => setIsDrawerCollapsed(!isDrawerCollapsed)}
                  title={isDrawerCollapsed ? 'Expand Console' : 'Minimize Console'}
                >
                  {isDrawerCollapsed ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                </button>

                <button
                  type="button"
                  className="btn-drawer-icon close"
                  onClick={() => setIsDrawerVisible(false)}
                  title="Close Results Console"
                >
                  <X size={14} />
                </button>
              </div>
            </div>

            {/* Metrics Row (only visible if not collapsed) */}
            {!isDrawerCollapsed && (
              <div className="drawer-metrics-row">
                <span className="metric-chip">
                  <CheckCircle2 size={13} className="text-yellow" />
                  <span>
                    {lastEvaluation.testsPassed} / {lastEvaluation.totalTests} Test Cases Passed
                  </span>
                </span>
                <span className="metric-chip">
                  <Clock size={13} className="text-muted" />
                  <span>{lastEvaluation.executionTimeMs} ms</span>
                </span>

                {lastEvaluation.verdict !== 'ACCEPTED' && onDiagnoseError && (
                  <button
                    type="button"
                    className="btn-diagnose-chip"
                    onClick={onDiagnoseError}
                    title="Ask SocraBuddy to diagnose this error"
                  >
                    <BrainCircuit size={13} />
                    <span>Diagnose with SocraBuddy</span>
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Drawer Body (collapsible) */}
          {!isDrawerCollapsed && (
            <div className="drawer-body">
              {/* If Compilation Error */}
              {lastEvaluation.verdict === 'COMPILE_ERROR' && lastEvaluation.errorMessage && (
                <div className="error-output-box">
                  <div className="error-box-header">
                    <AlertTriangle size={14} /> Compilation Diagnostics
                  </div>
                  <pre className="error-code-trace">{lastEvaluation.errorMessage}</pre>
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
                        <span>Case #{tc.testNumber || idx + 1}</span>
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
                            <label className="field-label">Input:</label>
                            <pre className="field-pre">{testCasesList[selectedTestCaseTab].input}</pre>
                          </div>
                          <div className="case-field-col">
                            <label className="field-label">Expected Output:</label>
                            <pre className="field-pre">{testCasesList[selectedTestCaseTab].expectedOutput}</pre>
                          </div>
                          <div className="case-field-col">
                            <label className="field-label">Actual Output:</label>
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
                          <label className="field-label text-red">Error Message:</label>
                          <pre className="field-pre text-red">
                            {testCasesList[selectedTestCaseTab].errorMessage}
                          </pre>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* AI Mentor Socratic Diagnostic & Hint Card */}
              <AiMentorPanel
                submission={lastEvaluation}
                onOpenFullModal={_onOpenMentor}
              />
            </div>
          )}
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
