import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Swords,
  Users,
  Trophy,
  Clock,
  Zap,
  Play,
  XCircle,
  Copy,
  RotateCcw,
  Sparkles,
  LogOut,
  X,
  Terminal,
  CheckCircle2,
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  Loader2,
} from 'lucide-react';
import { Duel } from '../types/duel';
import { ProblemDetail } from '../types/problem';
import { SubmissionResponse, TestCaseEvaluationResult } from '../types/submission';
import { duelService } from '../services/duelService';
import { problemService } from '../services/problemService';
import { submissionService } from '../services/submissionService';
import { websocketService } from '../services/websocketService';
import { useAuth } from '../context/AuthContext';
import { CodeEditor } from '../components/CodeEditor';

const ACTIVE_DUEL_STORAGE_KEY = 'active_duel_room_code';

// Formatting helpers for crisp LeetCode-style problem descriptions
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
          <div key={idx} className="statement-example-card">
            <span className="example-card-title">{title}</span>
            {hasStructuredFields ? (
              <div className="example-structured-grid">
                {inputStr && (
                  <div className="example-field-row">
                    <span className="field-lbl">Input:</span>
                    <code className="field-val">{inputStr}</code>
                  </div>
                )}
                {outputStr && (
                  <div className="example-field-row">
                    <span className="field-lbl">Output:</span>
                    <code className="field-val">{outputStr}</code>
                  </div>
                )}
                {explanationStr && (
                  <div className="example-field-row">
                    <span className="field-lbl">Explanation:</span>
                    <span className="field-val-text">{explanationStr}</span>
                  </div>
                )}
              </div>
            ) : (
              <pre className="example-raw-code">{otherLines.join('\n') || block}</pre>
            )}
          </div>
        );
      })}
    </div>
  );
};

export const DuelsPage: React.FC = () => {
  const { roomCode: paramRoomCode } = useParams<{ roomCode?: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Mode: lobby or arena
  const [activeDuel, setActiveDuel] = useState<Duel | null>(null);
  const [savedRoomCode, setSavedRoomCode] = useState<string | null>(null);
  const [selectedDifficulty, setSelectedDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD'>('EASY');
  const [joinCodeInput, setJoinCodeInput] = useState<string>('');
  const [isQueueing, setIsQueueing] = useState<boolean>(false);
  const [errorNotice, setErrorNotice] = useState<string | null>(null);
  const [copiedCodeNotice, setCopiedCodeNotice] = useState<boolean>(false);
  const [isRefreshingRoom, setIsRefreshingRoom] = useState<boolean>(false);

  // Arena state
  const [problemDetail, setProblemDetail] = useState<ProblemDetail | null>(null);
  const [code, setCode] = useState<string>('');
  const [duelTimeLeft, setDuelTimeLeft] = useState<number>(900); // 15 mins default
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [myProgress, setMyProgress] = useState<number>(0);
  const [showVictoryModal, setShowVictoryModal] = useState<boolean>(false);
  const [lastEvaluation, setLastEvaluation] = useState<SubmissionResponse | null>(null);
  const [selectedTestCaseTab, setSelectedTestCaseTab] = useState<number>(0);
  const [isDrawerCollapsed, setIsDrawerCollapsed] = useState<boolean>(false);

  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const loadedProblemSlugRef = useRef<string | null>(null);

  // Check for stored active session on mount
  useEffect(() => {
    const saved = localStorage.getItem(ACTIVE_DUEL_STORAGE_KEY);
    if (saved && !paramRoomCode) {
      setSavedRoomCode(saved);
    }
  }, [paramRoomCode]);

  // Poll room state if in active duel
  const pollDuelState = async (codeStr: string, isManual = false) => {
    if (isManual) setIsRefreshingRoom(true);
    try {
      const d = await duelService.getDuelRoom(codeStr);
      setActiveDuel(d);
      localStorage.setItem(ACTIVE_DUEL_STORAGE_KEY, codeStr);

      if (d.status === 'COMPLETED') {
        setShowVictoryModal(true);
        localStorage.removeItem(ACTIVE_DUEL_STORAGE_KEY);
        localStorage.removeItem(`duel_code_${codeStr}`);
        setSavedRoomCode(null);
        if (pollingRef.current) clearInterval(pollingRef.current);
      }

      // Problem is loaded ONLY ONCE when BOTH users have joined and status transitions to IN_PROGRESS
      if (d.status === 'IN_PROGRESS' && d.problemSlug && loadedProblemSlugRef.current !== d.problemSlug) {
        loadedProblemSlugRef.current = d.problemSlug;
        const p = await problemService.getProblemByIdOrSlug(d.problemSlug);
        setProblemDetail(p);

        // Retain any ongoing code typed in this session, otherwise use starter template
        const savedCode = localStorage.getItem(`duel_code_${codeStr}`);
        if (savedCode) {
          setCode(savedCode);
        } else {
          setCode(p.starterCode || 'class Solution {\n    // Code your 1v1 duel solution here\n}');
        }
      }
    } catch {
      // If room no longer exists or error, clear storage
      localStorage.removeItem(ACTIVE_DUEL_STORAGE_KEY);
      localStorage.removeItem(`duel_code_${codeStr}`);
      setSavedRoomCode(null);
    } finally {
      if (isManual) setIsRefreshingRoom(false);
    }
  };

  useEffect(() => {
    if (paramRoomCode) {
      pollDuelState(paramRoomCode, false);
      // Fallback polling every 5s if WebSocket is offline
      pollingRef.current = setInterval(() => pollDuelState(paramRoomCode, false), 5000);

      // Instant Zero-Cost STOMP WebSocket Subscription for 0ms latency opponent updates
      const unsubscribe = websocketService.subscribeToDuel(paramRoomCode, (updatedDuel) => {
        if (!updatedDuel) return;
        setActiveDuel(updatedDuel);
        localStorage.setItem(ACTIVE_DUEL_STORAGE_KEY, updatedDuel.roomCode);

        if (updatedDuel.status === 'COMPLETED') {
          setShowVictoryModal(true);
          localStorage.removeItem(ACTIVE_DUEL_STORAGE_KEY);
          localStorage.removeItem(`duel_code_${updatedDuel.roomCode}`);
          setSavedRoomCode(null);
          if (pollingRef.current) clearInterval(pollingRef.current);
        }

        if (
          updatedDuel.status === 'IN_PROGRESS' &&
          updatedDuel.problemSlug &&
          loadedProblemSlugRef.current !== updatedDuel.problemSlug
        ) {
          loadedProblemSlugRef.current = updatedDuel.problemSlug;
          problemService.getProblemByIdOrSlug(updatedDuel.problemSlug).then((p) => {
            setProblemDetail(p);
            const savedCode = localStorage.getItem(`duel_code_${updatedDuel.roomCode}`);
            if (savedCode) {
              setCode(savedCode);
            } else {
              setCode(p.starterCode || 'class Solution {\n    // Code your 1v1 duel solution here\n}');
            }
          }).catch(() => {});
        }
      });

      return () => {
        if (pollingRef.current) clearInterval(pollingRef.current);
        unsubscribe();
      };
    } else {
      loadedProblemSlugRef.current = null;
      setActiveDuel(null);
      setProblemDetail(null);
    }
  }, [paramRoomCode]);

  // Countdown clock
  useEffect(() => {
    if (!activeDuel || activeDuel.status !== 'IN_PROGRESS') return;
    const timer = setInterval(() => {
      setDuelTimeLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [activeDuel]);

  const handleStartQueue = async () => {
    setIsQueueing(true);
    setErrorNotice(null);
    try {
      const duel = await duelService.queueOrStartDuel({ difficulty: selectedDifficulty });
      setActiveDuel(duel);
      localStorage.setItem(ACTIVE_DUEL_STORAGE_KEY, duel.roomCode);
      navigate(`/duels/${duel.roomCode}`);
    } catch (err: any) {
      setErrorNotice(err.message || 'Failed to enter duel queue');
    } finally {
      setIsQueueing(false);
    }
  };

  const handleJoinWithCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinCodeInput.trim()) return;
    try {
      const duel = await duelService.joinDuelRoom(joinCodeInput.trim());
      setActiveDuel(duel);
      localStorage.setItem(ACTIVE_DUEL_STORAGE_KEY, duel.roomCode);
      navigate(`/duels/${duel.roomCode}`);
    } catch (err: any) {
      setErrorNotice(err.message || 'Failed to join duel room');
    }
  };

  const handleLeaveRoom = async () => {
    if (activeDuel) {
      try {
        await duelService.leaveDuelRoom(activeDuel.roomCode);
      } catch {
        // ignore
      }
    }
    if (pollingRef.current) clearInterval(pollingRef.current);
    localStorage.removeItem(ACTIVE_DUEL_STORAGE_KEY);
    setSavedRoomCode(null);
    setActiveDuel(null);
    setProblemDetail(null);
    navigate('/duels');
  };

  const handleExitArena = () => {
    const confirmExit = window.confirm(
      'Are you sure you want to leave the active duel arena? You can rejoin anytime from the Duels hub before the timer expires.'
    );
    if (confirmExit) {
      if (pollingRef.current) clearInterval(pollingRef.current);
      navigate('/duels');
    }
  };

  const handleRejoinSession = () => {
    if (savedRoomCode) {
      navigate(`/duels/${savedRoomCode}`);
    }
  };

  const handleDismissSavedSession = () => {
    localStorage.removeItem(ACTIVE_DUEL_STORAGE_KEY);
    setSavedRoomCode(null);
  };

  const handleCodeChange = (newVal: string) => {
    setCode(newVal);
    const roomKey = activeDuel?.roomCode || paramRoomCode;
    if (roomKey) {
      localStorage.setItem(`duel_code_${roomKey}`, newVal);
    }
  };

  const handleResetCode = () => {
    if (!problemDetail) return;
    const confirmReset = window.confirm(
      'Reset code editor back to default Java starter template? Any unsaved edits will be lost.'
    );
    if (confirmReset) {
      const starter = problemDetail.starterCode || 'class Solution {\n    // Code your 1v1 duel solution here\n}';
      setCode(starter);
      const roomKey = activeDuel?.roomCode || paramRoomCode;
      if (roomKey) {
        localStorage.removeItem(`duel_code_${roomKey}`);
      }
      setLastEvaluation(null);
    }
  };

  const handleRunSamples = async () => {
    if (!problemDetail || !activeDuel) return;
    setIsRunning(true);
    setErrorNotice(null);
    try {
      const res = await submissionService.runCode({
        problemId: problemDetail.id,
        sourceCode: code,
        language: 'JAVA',
      });
      setLastEvaluation(res);
      setIsDrawerCollapsed(false);
      const percent = res.totalTests > 0 ? Math.round((res.testsPassed / res.totalTests) * 50) : 25;
      setMyProgress(percent);
      await duelService.updateProgress(activeDuel.roomCode, percent);
    } catch (err: any) {
      setErrorNotice(err.message || 'Execution error');
    } finally {
      setIsRunning(false);
    }
  };

  // Fast transient gaming evaluation: verify test cases without persisting code records, awarding ELO points to first solver
  const handleSubmitSolution = async () => {
    if (!problemDetail || !activeDuel) return;
    setIsSubmitting(true);
    setErrorNotice(null);
    try {
      // Ephemeral evaluation against full test suite
      const res = await submissionService.runCode({
        problemId: problemDetail.id,
        sourceCode: code,
        language: 'JAVA',
      });
      setLastEvaluation(res);
      setIsDrawerCollapsed(false);

      const isAllPassed = res.totalTests > 0 && res.testsPassed === res.totalTests;

      if (isAllPassed) {
        setMyProgress(100);
        await duelService.updateProgress(activeDuel.roomCode, 100);
        const finished = await duelService.declareComplete(activeDuel.roomCode);
        setActiveDuel(finished);
        localStorage.removeItem(ACTIVE_DUEL_STORAGE_KEY);
        localStorage.removeItem(`duel_code_${activeDuel.roomCode}`);
        setSavedRoomCode(null);
        setShowVictoryModal(true);
      } else {
        const percent = res.totalTests > 0 ? Math.round((res.testsPassed / res.totalTests) * 90) : 40;
        setMyProgress(percent);
        await duelService.updateProgress(activeDuel.roomCode, percent);
      }
    } catch (err: any) {
      setErrorNotice(err.message || 'Submission error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getVerdictBadgeClass = (verdict?: string | null) => {
    switch (verdict) {
      case 'ACCEPTED':
        return 'badge-accepted';
      case 'WRONG_ANSWER':
        return 'badge-wrong';
      case 'COMPILE_ERROR':
      case 'RUNTIME_ERROR':
        return 'badge-error';
      case 'TIME_LIMIT_EXCEEDED':
        return 'badge-tle';
      default:
        return 'badge-neutral';
    }
  };

  const formatTime = (secs: number) => {
    const mins = Math.floor(secs / 60);
    const s = secs % 60;
    return `${mins.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const copyRoomCode = () => {
    if (!activeDuel) return;
    navigator.clipboard.writeText(activeDuel.roomCode);
    setCopiedCodeNotice(true);
    setTimeout(() => setCopiedCodeNotice(false), 2000);
  };

  // 1. LOBBY VIEW (Shown if no duel, or only 1 player waiting, or problem not loaded)
  if (!activeDuel || activeDuel.status === 'WAITING' || !problemDetail) {
    return (
      <div className="duels-page-container">
        {/* Rejoin Active Duel Session Alert */}
        {savedRoomCode && !activeDuel && (
          <div className="rejoin-duel-banner">
            <div className="rejoin-banner-left">
              <div className="rejoin-pulse-dot" />
              <div className="rejoin-banner-text">
                <strong>Active Duel in Progress: Room {savedRoomCode}</strong>
                <p>You have an ongoing 1v1 duel session. Jump back in before the clock runs out!</p>
              </div>
            </div>
            <div className="rejoin-banner-actions">
              <button type="button" className="btn-rejoin-duel" onClick={handleRejoinSession}>
                <Swords size={15} />
                <span>Rejoin Duel</span>
              </button>
              <button
                type="button"
                className="btn-dismiss-rejoin"
                onClick={handleDismissSavedSession}
                title="Dismiss active session"
              >
                <X size={15} />
              </button>
            </div>
          </div>
        )}

        <div className="duels-hero">
          <div className="badge-pill-gold">
            <Swords size={14} /> 1v1 Real-Time Code Duels
          </div>
          <h1>Student vs Student Coding Arena</h1>
          <p>
            Challenge a friend or join matchmaking to race on a live algorithm problem. First to achieve 100% testcase pass wins ELO rating points.
          </p>

          <div className="user-rating-pill">
            <Trophy size={16} className="text-yellow" />
            <span>Your Duel ELO: <strong>{user?.duelRating || 1200}</strong></span>
          </div>
        </div>

        {errorNotice && (
          <div className="contest-toast error">
            <span>{errorNotice}</span>
          </div>
        )}

        {/* Duel Waiting State (if in waiting room) */}
        {activeDuel && activeDuel.status === 'WAITING' ? (
          <div className="duel-waiting-card">
            <div className="waiting-spinner" />
            <h2>Waiting for Opponent to Join...</h2>
            <p>Share this 6-character room code with a friend or wait for matchmaking:</p>

            <div className="room-code-share-box">
              <span className="code-text">{activeDuel.roomCode}</span>
              <button type="button" className="btn-copy-code" onClick={copyRoomCode}>
                <Copy size={14} />
                <span>{copiedCodeNotice ? 'Copied!' : 'Copy Code'}</span>
              </button>
            </div>

            <div className="waiting-players-row">
              <div className="player-badge ready">
                <Users size={14} />
                <span>Player 1: {activeDuel.player1Name} (Ready)</span>
              </div>
              <div className="player-badge waiting">
                <Users size={14} />
                <span>Player 2: Waiting...</span>
              </div>
            </div>

            {/* Exit / Cancel and Refresh Controls */}
            <div className="waiting-room-actions">
              <button
                type="button"
                className="btn-refresh-room"
                onClick={() => pollDuelState(activeDuel.roomCode, true)}
                disabled={isRefreshingRoom}
                title="Refresh Room Status"
              >
                <RotateCcw size={14} className={isRefreshingRoom ? 'animate-spin' : ''} />
                <span>{isRefreshingRoom ? 'Refreshing...' : 'Refresh Status'}</span>
              </button>

              <button
                type="button"
                className="btn-cancel-queue"
                onClick={handleLeaveRoom}
                title="Leave Waiting Room"
              >
                <LogOut size={14} />
                <span>Cancel & Leave Room</span>
              </button>
            </div>
          </div>
        ) : (
          /* Matchmaking Selection Cards */
          <div className="duel-modes-grid">
            <div className="duel-mode-card">
              <div className="mode-icon-circle bg-yellow">
                <Zap size={24} />
              </div>
              <h2>Quick Match (Ranked Queue)</h2>
              <p>Pick a difficulty and pair up with an online student of similar ELO rating.</p>

              <div className="difficulty-pills">
                {(['EASY', 'MEDIUM', 'HARD'] as const).map((diff) => (
                  <button
                    key={diff}
                    type="button"
                    className={`diff-pill ${selectedDifficulty === diff ? 'active' : ''}`}
                    onClick={() => setSelectedDifficulty(diff)}
                  >
                    {diff}
                  </button>
                ))}
              </div>

              <button
                type="button"
                className="btn-find-opponent"
                onClick={handleStartQueue}
                disabled={isQueueing}
              >
                <Swords size={16} />
                <span>{isQueueing ? 'Searching...' : 'Find Opponent Now'}</span>
              </button>
            </div>

            <div className="duel-mode-card">
              <div className="mode-icon-circle bg-red">
                <Users size={24} />
              </div>
              <h2>Challenge with Room Code</h2>
              <p>Enter a 6-character private invite code from a friend to start a duel.</p>

              <form onSubmit={handleJoinWithCode} className="join-room-form">
                <input
                  type="text"
                  placeholder="e.g. DUEL-9X2K"
                  value={joinCodeInput}
                  onChange={(e) => setJoinCodeInput(e.target.value.toUpperCase())}
                  required
                />
                <button type="submit" className="btn-join-room">
                  <span>Join Room</span>
                </button>
              </form>
            </div>
          </div>
        )}
      </div>
    );
  }

  // 2. ACTIVE ARENA VIEW
  const isPlayer1 = user?.id === activeDuel.player1Id;
  const opponentName = isPlayer1 ? activeDuel.player2Name || 'Opponent' : activeDuel.player1Name;
  const opponentProgress = isPlayer1 ? activeDuel.player2Progress : activeDuel.player1Progress;

  return (
    <div className="duel-arena-container">
      {/* Top Match Bar with Exit Button */}
      <div className="duel-arena-header">
        <div className="duel-header-left">
          <div className="duel-badge-pulse">
            <Swords size={15} />
            <span>1v1 Code Duel • {activeDuel.difficulty}</span>
          </div>
          <h2 className="duel-problem-title">{problemDetail?.title || 'Loading problem...'}</h2>
        </div>

        {/* 15-Minute Countdown */}
        <div className="duel-timer-box">
          <Clock size={16} />
          <span>{formatTime(duelTimeLeft)}</span>
        </div>

        <div className="duel-header-right">
          <span className="room-label">Room: <strong>{activeDuel.roomCode}</strong></span>
          <button
            type="button"
            className="btn-exit-arena"
            onClick={handleExitArena}
            title="Exit Duel to Lobby (You can rejoin anytime)"
          >
            <LogOut size={14} />
            <span>Exit Arena</span>
          </button>
        </div>
      </div>

      {/* Opponent Progress Radar Bar */}
      <div className="duel-progress-radar">
        <div className="player-progress-col">
          <div className="player-progress-info">
            <strong>You ({user?.fullName || 'You'})</strong>
            <span>{myProgress}% testcases</span>
          </div>
          <div className="progress-bar-bg">
            <div className="progress-bar-fill me" style={{ width: `${myProgress}%` }} />
          </div>
        </div>

        <div className="vs-badge">VS</div>

        <div className="player-progress-col">
          <div className="player-progress-info">
            <strong>{opponentName}</strong>
            <span>{opponentProgress}% testcases</span>
          </div>
          <div className="progress-bar-bg">
            <div className="progress-bar-fill opponent" style={{ width: `${opponentProgress}%` }} />
          </div>
        </div>
      </div>

      {/* Split Workspace */}
      <div className="duel-workspace-grid">
        <div className="duel-problem-pane">
          <h2>Problem Statement</h2>
          <div className="statement-body-text">
            <FormattedText text={problemDetail?.description || ''} />
          </div>

          {problemDetail?.examples && (
            <div className="examples-section">
              <h3>Examples</h3>
              <FormattedExamples rawExamples={problemDetail.examples} />
            </div>
          )}

          {problemDetail?.constraints && (
            <div className="constraints-section">
              <h3>Constraints</h3>
              <div className="constraints-list">
                <FormattedText text={problemDetail.constraints} />
              </div>
            </div>
          )}
        </div>

        <div className="duel-editor-pane">
          <div className="duel-editor-toolbar">
            <span className="lang-tag">Java 17 Sandboxed Execution</span>
            <div className="actions">
              <button
                type="button"
                className="btn-reset-code"
                onClick={handleResetCode}
                title="Reset code editor back to starter template"
              >
                <RotateCcw size={13} />
                <span>Reset Code</span>
              </button>

              <button
                type="button"
                className="btn-run"
                onClick={handleRunSamples}
                disabled={isRunning || isSubmitting}
              >
                <Play size={13} />
                <span>{isRunning ? 'Running...' : 'Run Samples'}</span>
              </button>

              <button
                type="button"
                className="btn-submit"
                onClick={handleSubmitSolution}
                disabled={isRunning || isSubmitting}
              >
                <Zap size={13} />
                <span>{isSubmitting ? 'Submitting...' : 'Submit Duel Solution'}</span>
              </button>
            </div>
          </div>

          {/* Server / Execution Error Banner */}
          {errorNotice && (
            <div className="duel-error-alert">
              <AlertTriangle size={15} className="text-red" />
              <span>{errorNotice}</span>
              <button
                type="button"
                onClick={() => setErrorNotice(null)}
                className="btn-close-alert"
                title="Dismiss Alert"
              >
                <X size={13} />
              </button>
            </div>
          )}

          <div className="duel-editor-wrap">
            <CodeEditor
              code={code}
              onChange={handleCodeChange}
              language="java"
              height="340px"
            />
          </div>

          {/* Collapsed Console Bar */}
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
                    <span className="text-xs text-muted">Ready to test assertions</span>
                  </>
                )}
              </div>

              <button
                type="button"
                className="btn-reopen-drawer"
                onClick={() => setIsDrawerCollapsed(false)}
                title="Expand Test Cases Console"
              >
                <span>Open Console</span>
                <ChevronUp size={13} />
              </button>
            </div>
          )}

          {/* Always Present Test Cases & Results Console Drawer */}
          {!isDrawerCollapsed && (
            <div className="execution-results-drawer" data-testid="execution-results-drawer">
              <div className="drawer-header">
                <div className="drawer-title-row">
                  <div className="drawer-console-tabs-group">
                    <div className="drawer-console-tab-btn active">
                      <Terminal size={14} />
                      <span>{lastEvaluation ? 'Execution Result' : 'Testcase Console'}</span>
                    </div>
                  </div>

                  <div className="drawer-actions-group">
                    <button
                      type="button"
                      className="btn-drawer-icon"
                      onClick={() => setLastEvaluation(null)}
                      title="Reset test case evaluation"
                    >
                      <RotateCcw size={13} />
                      <span className="btn-drawer-text">Reset</span>
                    </button>

                    <button
                      type="button"
                      className="btn-drawer-icon"
                      onClick={() => setIsDrawerCollapsed(true)}
                      title="Minimize Console"
                    >
                      <ChevronDown size={14} />
                    </button>
                  </div>
                </div>
              </div>

              <div className="drawer-body">
                {isRunning || isSubmitting ? (
                  <div className="console-running-indicator">
                    <Loader2 size={18} className="spinner-icon text-yellow" />
                    <div className="running-text-group">
                      <strong>Executing code in Java 17 isolated sandbox...</strong>
                      <span>Running assertions against test suite</span>
                    </div>
                  </div>
                ) : lastEvaluation ? (
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

                    {/* Compile Error Monospace Terminal Box */}
                    {lastEvaluation.verdict === 'COMPILE_ERROR' && lastEvaluation.errorMessage && (
                      <div className="console-terminal-block">
                        <div className="terminal-header text-red font-medium mb-1">
                          <AlertTriangle size={14} className="inline mr-1" />
                          <span>Java Compiler Output (javac):</span>
                        </div>
                        <pre className="console-code-pre">{lastEvaluation.errorMessage}</pre>
                      </div>
                    )}

                    {/* Runtime Error Box */}
                    {lastEvaluation.verdict === 'RUNTIME_ERROR' && lastEvaluation.errorMessage && (
                      <div className="console-terminal-block">
                        <div className="terminal-header text-red font-medium mb-1">
                          <AlertTriangle size={14} className="inline mr-1" />
                          <span>Runtime Exception (JVM):</span>
                        </div>
                        <pre className="console-code-pre">{lastEvaluation.errorMessage}</pre>
                      </div>
                    )}

                    {/* Test Cases Results List */}
                    {(() => {
                      let testCasesList: TestCaseEvaluationResult[] = [];
                      if (lastEvaluation.evaluationDetails) {
                        try {
                          testCasesList = JSON.parse(lastEvaluation.evaluationDetails);
                        } catch {
                          testCasesList = [];
                        }
                      }

                      if (testCasesList.length === 0) return null;

                      const activeTc = testCasesList[selectedTestCaseTab] || testCasesList[0];

                      return (
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
                                <span className={`tab-indicator-dot ${tc.passed ? 'passed' : 'failed'}`} />
                                <span>Case {tc.testNumber || idx + 1}</span>
                                {tc.isHidden && <span className="tab-hidden-badge">Hidden</span>}
                              </button>
                            ))}
                          </div>

                          {activeTc && (
                            <div className="test-case-detail">
                              {activeTc.isHidden ? (
                                <div className="hidden-test-shield text-muted p-2">
                                  <span>Hidden grading test case (inputs protected).</span>
                                </div>
                              ) : (
                                <>
                                  <div className="tc-field-group">
                                    <label className="tc-label">Input:</label>
                                    <pre className="tc-value-box">
                                      {activeTc.input || '(empty)'}
                                    </pre>
                                  </div>

                                  <div className="tc-field-group">
                                    <label className="tc-label">Your Output:</label>
                                    <pre
                                      className={`tc-value-box ${
                                        activeTc.passed ? 'tc-passed' : 'tc-failed'
                                      }`}
                                    >
                                      {activeTc.actualOutput || activeTc.errorMessage || '(no output)'}
                                    </pre>
                                  </div>

                                  <div className="tc-field-group">
                                    <label className="tc-label">Expected Output:</label>
                                    <pre className="tc-value-box tc-expected">
                                      {activeTc.expectedOutput || '(empty)'}
                                    </pre>
                                  </div>
                                </>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })()}
                  </>
                ) : (
                  /* Sample Test Cases Initial State */
                  <div className="test-cases-viewer">
                    {problemDetail?.sampleTestCases && problemDetail.sampleTestCases.length > 0 ? (
                      <>
                        <div className="test-case-tabs">
                          {problemDetail.sampleTestCases.map((_, idx) => (
                            <button
                              key={idx}
                              type="button"
                              className={`test-tab-btn ${selectedTestCaseTab === idx ? 'active' : ''}`}
                              onClick={() => setSelectedTestCaseTab(idx)}
                            >
                              <span className="tab-indicator-dot" />
                              <span>Case {idx + 1}</span>
                            </button>
                          ))}
                        </div>

                        {problemDetail.sampleTestCases[selectedTestCaseTab] && (
                          <div className="test-case-detail">
                            <div className="tc-field-group">
                              <label className="tc-label">Input:</label>
                              <pre className="tc-value-box">
                                {problemDetail.sampleTestCases[selectedTestCaseTab].inputData}
                              </pre>
                            </div>

                            <div className="tc-field-group">
                              <label className="tc-label">Expected Output:</label>
                              <pre className="tc-value-box tc-expected">
                                {problemDetail.sampleTestCases[selectedTestCaseTab].expectedOutput}
                              </pre>
                            </div>
                          </div>
                        )}
                      </>
                    ) : (
                      <div className="console-empty-prompt">
                        <span>Click <strong>"Run Samples"</strong> to execute assertions in the Java sandbox.</span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Victory / Defeat Modal */}
      {showVictoryModal && (
        <div className="contest-modal-overlay">
          <div className="duel-victory-modal">
            {activeDuel.winnerId === user?.id ? (
              <div className="victory-content">
                <div className="victory-icon-circle">
                  <Trophy size={48} className="text-yellow" />
                </div>
                <h2>VICTORY! 🎉</h2>
                <p>You solved the problem first and won the 1v1 duel!</p>

                <div className="elo-delta-pill win">
                  <Sparkles size={16} />
                  <span>+25 Duel ELO Rating</span>
                </div>
              </div>
            ) : (
              <div className="defeat-content">
                <div className="defeat-icon-circle">
                  <XCircle size={48} className="text-red" />
                </div>
                <h2>DUEL FINISHED</h2>
                <p>{activeDuel.winnerName || 'Your opponent'} solved the problem first.</p>

                <div className="elo-delta-pill lose">
                  <span>-20 Duel ELO Rating</span>
                </div>
              </div>
            )}

            <div className="modal-footer">
              <button
                type="button"
                className="btn-duel-rematch"
                onClick={() => navigate('/duels')}
              >
                <RotateCcw size={14} />
                <span>Back to Duels Hub</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
