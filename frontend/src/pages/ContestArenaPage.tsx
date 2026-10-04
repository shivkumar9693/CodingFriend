import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Clock,
  ShieldAlert,
  Trophy,
  ChevronLeft,
  Play,
  Send,
  Lock,
} from 'lucide-react';
import { Contest, ContestProblem, ContestLeaderboard } from '../types/contest';
import { ProblemDetail } from '../types/problem';
import { SubmissionResponse } from '../types/submission';
import { contestService } from '../services/contestService';
import { problemService } from '../services/problemService';
import { submissionService } from '../services/submissionService';
import { CodeEditor } from '../components/CodeEditor';
import { EditorPanel } from '../components/EditorPanel';

export const ContestArenaPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const [contest, setContest] = useState<Contest | null>(null);
  const [activeProblemIdx, setActiveProblemIdx] = useState<number>(0);
  const [currentProblemDetail, setCurrentProblemDetail] = useState<ProblemDetail | null>(null);
  const [code, setCode] = useState<string>('');
  const [timeRemainingSeconds, setTimeRemainingSeconds] = useState<number>(0);
  const [_tabSwitchAlerts, setTabSwitchAlerts] = useState<number>(0);
  const [warningNotice, setWarningNotice] = useState<string | null>(null);

  // Execution state
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [lastEvaluation, setLastEvaluation] = useState<SubmissionResponse | null>(null);

  // Standings Drawer
  const [showStandings, setShowStandings] = useState<boolean>(false);
  const [standingsData, setStandingsData] = useState<ContestLeaderboard | null>(null);

  // Copy-paste origin validator ref
  const internalCopiedTextRef = useRef<string>('');

  // 1. Fetch contest and active problem
  useEffect(() => {
    if (!slug) return;
    contestService.getContestBySlug(slug).then((c) => {
      setContest(c);
      const remaining = Math.max(0, Math.floor((new Date(c.endTime).getTime() - Date.now()) / 1000));
      setTimeRemainingSeconds(remaining);

      if (c.problems && c.problems.length > 0) {
        loadProblem(c.problems[0]);
      }
    }).catch(() => {
      navigate('/contests');
    });
  }, [slug, navigate]);

  const loadProblem = async (cp: ContestProblem) => {
    try {
      const p = await problemService.getProblemByIdOrSlug(cp.slug || cp.problemId);
      setCurrentProblemDetail(p);
      const savedCode = slug ? localStorage.getItem(`contest_${slug}_${p.id}_code`) : null;
      setCode(savedCode || p.starterCode || 'class Solution {\n    // Contest proctored solution\n}');
      setLastEvaluation(null);
    } catch {
      // ignore
    }
  };

  const handleCodeChange = (newCode: string) => {
    setCode(newCode);
    if (slug && currentProblemDetail?.id) {
      localStorage.setItem(`contest_${slug}_${currentProblemDetail.id}_code`, newCode);
    }
  };

  // 2. Countdown Timer
  useEffect(() => {
    if (timeRemainingSeconds <= 0) return;
    const timer = setInterval(() => {
      setTimeRemainingSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [timeRemainingSeconds]);

  // 3. Anti-Cheat: Tab Switch & Window Blur Telemetry
  const handleVisibilityChange = useCallback(() => {
    if (document.hidden && slug) {
      setTabSwitchAlerts((prev) => {
        const next = prev + 1;
        setWarningNotice(`⚠️ Warning #${next}: Leaving the contest tab is monitored.`);
        return next;
      });
      contestService.reportTabSwitch(slug).catch(() => {});
    }
  }, [slug]);

  useEffect(() => {
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [handleVisibilityChange]);

  // Keyboard Shortcuts (Ctrl+Enter to Run, Ctrl+Alt+Enter to Submit)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey && e.altKey && e.key === 'Enter') || (e.metaKey && e.shiftKey && e.key === 'Enter')) {
        e.preventDefault();
        if (!isRunning && !isSubmitting) {
          handleSubmitCode();
        }
      } else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        if (!isRunning && !isSubmitting) {
          handleRunCode();
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isRunning, isSubmitting, currentProblemDetail, code, contest]);

  // 4. Anti-Cheat: Intercept External Clipboard Paste
  const handlePasteCheck = (e: React.ClipboardEvent) => {
    const pastedText = e.clipboardData.getData('text');
    if (pastedText && pastedText !== internalCopiedTextRef.current && pastedText.length > 30) {
      // If text didn't originate from inside the arena buffer
      setWarningNotice('🔒 Anti-Cheat Guard: External paste is restricted in proctored contests.');
    }
  };

  const formatTimer = (seconds: number) => {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleRunCode = async () => {
    if (!currentProblemDetail) return;
    setIsRunning(true);
    try {
      const res = await submissionService.runCode({
        problemId: currentProblemDetail.id,
        sourceCode: code,
        language: 'JAVA',
      });
      setLastEvaluation(res);
    } catch (err: any) {
      setWarningNotice(err.message || 'Run code failed');
    } finally {
      setIsRunning(false);
    }
  };

  const handleSubmitCode = async () => {
    if (!currentProblemDetail || !contest) return;
    setIsSubmitting(true);
    try {
      const res = await submissionService.createSubmission({
        problemId: currentProblemDetail.id,
        sourceCode: code,
        language: 'JAVA',
      });
      setLastEvaluation(res);
    } catch (err: any) {
      setWarningNotice(err.message || 'Submission failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  const toggleStandings = async () => {
    if (!showStandings && slug) {
      try {
        const data = await contestService.getLeaderboard(slug);
        setStandingsData(data);
      } catch {
        // ignore
      }
    }
    setShowStandings(!showStandings);
  };

  if (!contest || !currentProblemDetail) {
    return (
      <div className="contest-arena-loading">
        <div className="skeleton" style={{ height: '60px', marginBottom: '1rem' }} />
        <div className="skeleton" style={{ height: '70vh' }} />
      </div>
    );
  }

  return (
    <div className="contest-arena-container" onPaste={handlePasteCheck}>
      {/* Warning Notification Banner */}
      {warningNotice && (
        <div className="proctoring-warning-banner">
          <ShieldAlert size={16} />
          <span>{warningNotice}</span>
          <button type="button" onClick={() => setWarningNotice(null)}>×</button>
        </div>
      )}

      {/* Top Contest Header Toolbar */}
      <div className="contest-arena-navbar">
        <div className="arena-nav-left">
          <button
            type="button"
            className="btn-arena-back"
            onClick={() => navigate('/contests')}
            title="Leave Arena"
          >
            <ChevronLeft size={16} />
            <span>Contests</span>
          </button>
          <div className="arena-contest-title">
            <h2>{contest.title}</h2>
          </div>
        </div>

        {/* Problem Selector Pills */}
        <div className="arena-problem-tabs">
          {contest.problems.map((cp, idx) => (
            <button
              key={cp.problemId}
              type="button"
              className={`arena-tab-pill ${activeProblemIdx === idx ? 'active' : ''}`}
              onClick={() => {
                setActiveProblemIdx(idx);
                loadProblem(cp);
              }}
            >
              <span>Q{idx + 1}</span>
              <span className="pill-pts">({cp.points} pts)</span>
            </button>
          ))}
        </div>

        {/* Timer & Telemetry Controls */}
        <div className="arena-nav-right">
          <div className={`arena-timer-box ${timeRemainingSeconds < 300 ? 'timer-urgent' : ''}`}>
            <Clock size={15} />
            <span className="timer-digits">{formatTimer(timeRemainingSeconds)}</span>
          </div>

          <div className="proctor-shield-pill" title="AI hints are disabled for authentic contest grading">
            <Lock size={13} className="text-yellow" />
            <span>Proctored • Zero AI</span>
          </div>

          <button
            type="button"
            className="btn-arena-standings"
            onClick={toggleStandings}
            title="View Live Standings"
          >
            <Trophy size={15} />
            <span>Ranklist</span>
          </button>
        </div>
      </div>

      {/* Main Split-View Workspace */}
      <div className="contest-workspace-grid">
        {/* Left Column: Problem Statement */}
        <div className="contest-problem-pane">
          <div className="problem-pane-header">
            <span className={`difficulty-badge diff-${currentProblemDetail.difficulty.toLowerCase()}`}>
              {currentProblemDetail.difficulty}
            </span>
            <h1 className="problem-title">{currentProblemDetail.title}</h1>
          </div>

          <div className="problem-pane-content">
            <div className="problem-description-section">
              <p>{currentProblemDetail.description}</p>
            </div>

            {currentProblemDetail.examples && (
              <div className="problem-examples-section">
                <h3>Examples & Constraints</h3>
                <pre className="example-block">{currentProblemDetail.examples}</pre>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Code Editor & Execution Panel */}
        <div className="contest-editor-pane">
          <div className="contest-editor-toolbar">
            <span className="lang-tag">Java 17 Sandboxed Execution</span>
            <div className="editor-actions">
              <button
                type="button"
                className="btn-run"
                onClick={handleRunCode}
                disabled={isRunning || isSubmitting}
              >
                <Play size={13} />
                <span>{isRunning ? 'Running...' : 'Run Samples'}</span>
              </button>

              <button
                type="button"
                className="btn-submit"
                onClick={handleSubmitCode}
                disabled={isRunning || isSubmitting}
              >
                <Send size={13} />
                <span>{isSubmitting ? 'Submitting...' : 'Submit Contest Solution'}</span>
              </button>
            </div>
          </div>

          <div className="contest-editor-wrapper">
            <CodeEditor
              code={code}
              onChange={handleCodeChange}
              language="java"
            />
          </div>

          {/* Results Console */}
          {lastEvaluation && (
            <EditorPanel
              problemId={currentProblemDetail.id}
              code={code}
              starterCode={currentProblemDetail.starterCode || ''}
              onCodeChange={handleCodeChange}
              onResetCode={() => handleCodeChange(currentProblemDetail.starterCode || '')}
              lastEvaluation={lastEvaluation}
              selectedLanguage="JAVA"
            />
          )}
        </div>
      </div>

      {/* Standings Slide-over Drawer */}
      {showStandings && standingsData && (
        <div className="standings-drawer-overlay" onClick={() => setShowStandings(false)}>
          <div className="standings-drawer" onClick={(e) => e.stopPropagation()}>
            <div className="drawer-head">
              <div className="head-title">
                <Trophy size={18} className="text-yellow" />
                <h3>Live Contest Standings</h3>
              </div>
              <button type="button" onClick={() => setShowStandings(false)}>×</button>
            </div>

            <div className="drawer-table-wrap">
              <table className="standings-mini-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Learner</th>
                    <th>Score</th>
                    <th>Penalty</th>
                  </tr>
                </thead>
                <tbody>
                  {standingsData.standings.map((p, idx) => (
                    <tr key={p.id}>
                      <td>#{idx + 1}</td>
                      <td>{p.fullName}</td>
                      <td className="text-green">{p.totalScore}</td>
                      <td>{p.totalPenaltyMinutes}m</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
