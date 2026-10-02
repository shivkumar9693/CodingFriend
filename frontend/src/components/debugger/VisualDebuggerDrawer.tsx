import React, { useState, useEffect, useRef } from 'react';
import {
  Play,
  Pause,
  SkipForward,
  SkipBack,
  RotateCcw,
  Sparkles,
  Bug,
  Lightbulb,
  X,
  Gauge,
  Layers,
  Database,
  CheckCircle2,
  HelpCircle,
} from 'lucide-react';
import { DebugSession, DebugStep } from '../../types/debugger';
import { TreeGraphVisualizer } from './TreeGraphVisualizer';
import '../../styles/debugger.css';

interface VisualDebuggerDrawerProps {
  session: DebugSession;
  onClose: () => void;
  onStepChange?: (step: DebugStep) => void;
}

export const VisualDebuggerDrawer: React.FC<VisualDebuggerDrawerProps> = ({
  session,
  onClose,
  onStepChange,
}) => {
  const [currentStepIndex, setCurrentStepIndex] = useState<number>(0);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1000); // ms per step: 2000 (0.5x), 1000 (1x), 500 (2x)
  const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
  const [answeredCorrectly, setAnsweredCorrectly] = useState<boolean | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const currentStep: DebugStep = session.steps[currentStepIndex] || session.steps[0];

  useEffect(() => {
    if (onStepChange) {
      onStepChange(currentStep);
    }
  }, [currentStepIndex, onStepChange, currentStep]);

  // Auto-play timer
  useEffect(() => {
    if (isPlaying) {
      // Pause if current step has a Socratic question not yet answered
      if (currentStep.socraticQuestion && answeredCorrectly === null) {
        setIsPlaying(false);
        return;
      }

      timerRef.current = setTimeout(() => {
        if (currentStepIndex < session.totalSteps - 1) {
          setCurrentStepIndex((prev) => prev + 1);
          setSelectedAnswer(null);
          setAnsweredCorrectly(null);
        } else {
          setIsPlaying(false);
        }
      }, playbackSpeed);
    }

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [isPlaying, currentStepIndex, session.totalSteps, playbackSpeed, currentStep, answeredCorrectly]);

  const handleNextStep = () => {
    if (currentStepIndex < session.totalSteps - 1) {
      setCurrentStepIndex((prev) => prev + 1);
      setSelectedAnswer(null);
      setAnsweredCorrectly(null);
    }
  };

  const handlePrevStep = () => {
    if (currentStepIndex > 0) {
      setCurrentStepIndex((prev) => prev - 1);
      setSelectedAnswer(null);
      setAnsweredCorrectly(null);
    }
  };

  const handleReset = () => {
    setIsPlaying(false);
    setCurrentStepIndex(0);
    setSelectedAnswer(null);
    setAnsweredCorrectly(null);
  };

  const handleAnswerClick = (idx: number, isCorrect: boolean) => {
    setSelectedAnswer(idx);
    setAnsweredCorrectly(isCorrect);
  };

  return (
    <div className="visual-debugger-drawer" data-testid="visual-debugger-drawer">
      {/* Top Header Bar */}
      <div className="debugger-header-bar">
        <div className="debugger-title-group">
          <div className="debugger-icon-badge">
            <Bug size={18} />
          </div>
          <div>
            <h4 className="debugger-title">Live Step Debugger & Visual Memory</h4>
            <span className="debugger-subtitle">
              Interactive line-by-line execution • {session.problemTitle}
            </span>
          </div>
        </div>

        {/* Top Right: Step Counter & Close */}
        <div className="debugger-header-right">
          <span className="step-indicator-pill">
            {currentStepIndex + 1} / {session.totalSteps}
          </span>

          <button
            type="button"
            className="btn-debug-control btn-close-debugger"
            onClick={onClose}
            title="Close debugger panel"
          >
            <X size={14} />
          </button>
        </div>
      </div>

      {/* Stepper Controls & Playback Toolbar */}
      <div className="debugger-actions-toolbar">
        {/* Speed Selector */}
        <div className="debugger-speed-group" title="Execution Speed">
          <Gauge size={12} className="text-muted" />
          <button
            type="button"
            className={`btn-speed-pill ${playbackSpeed === 2000 ? 'active' : ''}`}
            onClick={() => setPlaybackSpeed(2000)}
          >
            0.5x
          </button>
          <button
            type="button"
            className={`btn-speed-pill ${playbackSpeed === 1000 ? 'active' : ''}`}
            onClick={() => setPlaybackSpeed(1000)}
          >
            1x
          </button>
          <button
            type="button"
            className={`btn-speed-pill ${playbackSpeed === 500 ? 'active' : ''}`}
            onClick={() => setPlaybackSpeed(500)}
          >
            2x
          </button>
        </div>

        <button
          type="button"
          className="btn-debug-control"
          onClick={handleReset}
          title="Restart from Step 1"
        >
          <RotateCcw size={13} />
          <span>Reset</span>
        </button>

        <button
          type="button"
          className="btn-debug-control"
          onClick={handlePrevStep}
          disabled={currentStepIndex === 0}
          title="Step Back (Previous Line)"
        >
          <SkipBack size={13} />
          <span>Prev</span>
        </button>

        <button
          type="button"
          className={`btn-debug-control ${isPlaying ? '' : 'btn-debug-primary'}`}
          onClick={() => setIsPlaying(!isPlaying)}
          title={isPlaying ? 'Pause auto-step' : 'Auto-play execution'}
        >
          {isPlaying ? <Pause size={13} /> : <Play size={13} />}
          <span>{isPlaying ? 'Pause' : 'Auto Play'}</span>
        </button>

        <button
          type="button"
          className="btn-debug-control btn-debug-primary btn-next-step"
          onClick={handleNextStep}
          disabled={currentStepIndex === session.totalSteps - 1}
          title="Step Over (Next Line)"
        >
          <span>Next Step</span>
          <SkipForward size={13} />
        </button>
      </div>

      {/* Progress Timeline Dots Track */}
      <div className="debugger-timeline-track">
        {session.steps.map((step, idx) => (
          <div
            key={idx}
            className={`timeline-step-dot ${idx < currentStepIndex ? 'completed' : ''} ${idx === currentStepIndex ? 'active' : ''
              }`}
            onClick={() => {
              setIsPlaying(false);
              setCurrentStepIndex(idx);
              setSelectedAnswer(null);
              setAnsweredCorrectly(null);
            }}
            title={`Step ${idx + 1}: ${step.title}`}
          />
        ))}
      </div>

      {/* Active Code Line Snippet */}
      <div className="active-step-snippet-box">
        <div className="step-code-info">
          <span className="step-line-tag">Line {currentStep.lineNumber}</span>
          <span className="step-code-text">{currentStep.codeSnippet}</span>
        </div>
        <span className="step-action-badge">{currentStep.actionType?.replace(/_/g, ' ') || 'STEP'}</span>
      </div>

      {/* Visual Memory Canvas: Binary Tree & Graph Visualizer */}
      {(currentStep.treeState || currentStep.graphState) && (
        <TreeGraphVisualizer
          treeState={currentStep.treeState}
          graphState={currentStep.graphState}
        />
      )}

      {/* Visual Memory Canvas: Array Cells & Pointers */}
      {currentStep.arrayState && (
        <div className="debugger-visual-canvas">
          <div className="canvas-title-strip">
            <span className="canvas-title">
              <Layers size={13} /> Memory State: {currentStep.arrayState.name}
            </span>
            <span className="canvas-hint">Pointers smoothly glide to active indices</span>
          </div>

          <div className="array-visual-row">
            {currentStep.arrayState.values.map((val, idx) => {
              const pointersAtCell = currentStep.arrayState?.pointers.filter(
                (p) => p.index === idx
              );
              const isActive = currentStep.arrayState?.activeIndices?.includes(idx);
              const isMatched = currentStep.arrayState?.matchedIndices?.includes(idx);

              return (
                <div key={idx} className="array-cell-container">
                  {/* Floating Pointer Badges */}
                  {pointersAtCell && pointersAtCell.length > 0 && (
                    <div className="pointer-badge-floating">
                      {pointersAtCell.map((p, pIdx) => (
                        <span
                          key={pIdx}
                          className={`pointer-tag pointer-${p.color || 'yellow'}`}
                        >
                          {p.label || p.name}
                        </span>
                      ))}
                      <span className="pointer-arrow">▼</span>
                    </div>
                  )}

                  {/* Cell Box */}
                  <div
                    className={`array-cell-block ${isActive ? 'active' : ''} ${isMatched ? 'matched' : ''
                      }`}
                  >
                    {val}
                  </div>

                  {/* Index Label */}
                  <span className="array-cell-index">idx {idx}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* HashMap Inspector Table */}
      {currentStep.mapState && (
        <div className="hashmap-inspector-card">
          <div className="canvas-title-strip">
            <span className="canvas-title">
              <Database size={13} /> Hash Table: {currentStep.mapState.name} ({currentStep.mapState.entries.length} items)
            </span>
          </div>

          <div className="map-table-grid">
            {currentStep.mapState.entries.length === 0 ? (
              <span className="text-muted text-xs font-mono">(Empty Map: No key-value pairs stored yet)</span>
            ) : (
              currentStep.mapState.entries.map((entry, idx) => (
                <div
                  key={idx}
                  className={`map-entry-chip ${entry.isNew ? 'new-entry' : ''} ${entry.isHit ? 'hit-entry' : ''
                    }`}
                >
                  <span className="map-key-badge">Key: {entry.key}</span>
                  <span className="map-arrow">→</span>
                  <span className="map-val-badge">Val: {String(entry.value)}</span>
                  {entry.isHit && <span className="hit-indicator">MATCH!</span>}
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Scope Variables Watch Deck */}
      {currentStep.variables && currentStep.variables.length > 0 && (
        <div className="debugger-watch-deck">
          <div className="watch-deck-label">Scope Variables:</div>
          <div className="watch-pills-row">
            {currentStep.variables.map((v, idx) => (
              <div
                key={idx}
                className={`watch-var-pill ${v.isUpdated ? 'updated' : ''}`}
              >
                <span className="watch-name">{v.name} =</span>
                <span className="watch-val">{String(v.value)}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Socratic Step Explanation Callout */}
      <div className="debugger-socratic-callout">
        <Lightbulb size={16} className="text-yellow flex-shrink-0 mt-0.5" />
        <div className="socratic-content">
          <span className="socratic-badge">
            <Sparkles size={11} /> SocraBuddy Mentor Insight
          </span>
          <p className="socratic-text">{currentStep.explanation}</p>
        </div>
      </div>

      {/* Socratic Checkpoint Pause Question */}
      {currentStep.socraticQuestion && (
        <div className="socratic-checkpoint-box">
          <div className="checkpoint-header">
            <HelpCircle size={15} className="text-yellow" />
            <span>Socratic Prediction Checkpoint</span>
          </div>
          <p className="checkpoint-question">{currentStep.socraticQuestion.question}</p>

          <div className="checkpoint-options-row">
            {currentStep.socraticQuestion.options.map((opt, oIdx) => (
              <button
                key={oIdx}
                type="button"
                className={`btn-checkpoint-option ${selectedAnswer === oIdx
                    ? opt.isCorrect
                      ? 'correct'
                      : 'incorrect'
                    : ''
                  }`}
                onClick={() => handleAnswerClick(oIdx, opt.isCorrect)}
              >
                <span className="option-num">{oIdx + 1}.</span>
                <span>{opt.text}</span>
              </button>
            ))}
          </div>

          {selectedAnswer !== null && (
            <div className={`checkpoint-feedback ${answeredCorrectly ? 'feedback-success' : 'feedback-error'}`}>
              <CheckCircle2 size={15} className="flex-shrink-0" />
              <span>{currentStep.socraticQuestion.options[selectedAnswer]?.feedback}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
