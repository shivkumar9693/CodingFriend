import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import {
  Sparkles,
  X,
  ArrowUp,
  Loader2,
  HelpCircle,
  Copy,
  Check,
  RotateCcw,
  ChevronRight,
  BrainCircuit,
  MessageSquare,
  AlertCircle,
} from 'lucide-react';
import { mentorService } from '../services/mentorService';
import {
  HintLevel,
  SocraBuddyDiagnosisResponse,
  SocraBuddyHintResponse,
} from '../types/mentor';

export interface SocraBuddyModalProps {
  isOpen: boolean;
  onClose: () => void;
  problemId: string;
  problemTitle: string;
  sourceCode: string;
  lastErrorMessage?: string | null;
  lastVerdict?: string | null;
  initialMode?: 'hint' | 'diagnose';
}

interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  timestamp: string;
  hintData?: SocraBuddyHintResponse;
  diagnosisData?: SocraBuddyDiagnosisResponse;
  text?: string;
  level?: HintLevel;
}

const HINT_LEVELS: { level: HintLevel; label: string; desc: string }[] = [
  { level: 'LEVEL_1_NUDGE', label: '1. Nudge', desc: 'Guiding question' },
  { level: 'LEVEL_2_CONCEPT', label: '2. Concept', desc: 'Core property' },
  { level: 'LEVEL_3_ALGORITHM', label: '3. Blueprint', desc: 'Logic structure' },
  { level: 'LEVEL_4_TARGETED', label: '4. Targeted', desc: 'Specific pointer' },
];

const QUICK_PROMPTS = [
  'Why is my current approach too slow?',
  'How do I calculate the complement using HashMap?',
  'What edge cases should I consider for this problem?',
  'Give me a conceptual clue without the code',
];

export const SocraBuddyModal: React.FC<SocraBuddyModalProps> = ({
  isOpen,
  onClose,
  problemId,
  problemTitle,
  sourceCode,
  lastErrorMessage,
  lastVerdict,
  initialMode = 'hint',
}) => {
  const [selectedLevel, setSelectedLevel] = useState<HintLevel>('LEVEL_1_NUDGE');
  const [userQuestion, setUserQuestion] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
      setTimeout(() => inputRef.current?.focus(), 100);
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!isOpen) {
      setMessages([]);
      setError(null);
      setUserQuestion('');
      return;
    }

    if (initialMode === 'diagnose' && (lastErrorMessage || lastVerdict)) {
      handleDiagnose();
    } else if (messages.length === 0) {
      handleRequestHint('LEVEL_1_NUDGE');
    }
  }, [isOpen, initialMode]);

  if (!isOpen) return null;

  const handleRequestHint = async (levelToFetch: HintLevel = selectedLevel, customQuestion?: string) => {
    const questionText = customQuestion !== undefined ? customQuestion : userQuestion.trim();
    setIsLoading(true);
    setError(null);

    const userMsgId = `user-${Date.now()}`;
    if (questionText) {
      setMessages((prev) => [
        ...prev,
        {
          id: userMsgId,
          sender: 'user',
          text: questionText,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
      setUserQuestion('');
    }

    const assistantMsgId = `assistant-${Date.now()}`;
    setMessages((prev) => [
      ...prev,
      {
        id: assistantMsgId,
        sender: 'assistant',
        text: '',
        level: levelToFetch,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ]);

    try {
      await mentorService.streamHint(
        {
          problemId,
          sourceCode,
          hintLevel: levelToFetch,
          userQuestion: questionText || undefined,
          lastErrorMessage: lastErrorMessage || undefined,
        },
        (token) => {
          setMessages((prev) =>
            prev.map((m) => (m.id === assistantMsgId ? { ...m, text: (m.text || '') + token } : m))
          );
        },
        (res) => {
          setSelectedLevel(levelToFetch);
          setMessages((prev) =>
            prev.map((m) => (m.id === assistantMsgId ? { ...m, hintData: res, text: undefined } : m))
          );
          setIsLoading(false);
        },
        (err) => {
          setError(err instanceof Error ? err.message : 'Mentor is temporarily unavailable. Please try again.');
          setIsLoading(false);
        }
      );
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Mentor is temporarily unavailable. Please try again.');
      setIsLoading(false);
    }
  };

  const handleDiagnose = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const res = await mentorService.diagnoseFailure({
        problemId,
        sourceCode,
        verdict: lastVerdict || undefined,
        errorMessage: lastErrorMessage || undefined,
      });

      const assistantMsgId = `diagnosis-${Date.now()}`;
      setMessages((prev) => [
        ...prev,
        {
          id: assistantMsgId,
          sender: 'assistant',
          diagnosisData: res,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to diagnose mistake. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCopyText = (id: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const getNextLevel = (current?: HintLevel): HintLevel | null => {
    if (!current) return null;
    switch (current) {
      case 'LEVEL_1_NUDGE':
        return 'LEVEL_2_CONCEPT';
      case 'LEVEL_2_CONCEPT':
        return 'LEVEL_3_ALGORITHM';
      case 'LEVEL_3_ALGORITHM':
        return 'LEVEL_4_TARGETED';
      default:
        return null;
    }
  };

  return createPortal(
    <div
      className="socrabuddy-chatgpt-backdrop"
      onClick={onClose}
      data-testid="socrabuddy-modal"
      role="dialog"
      aria-modal="true"
    >
      <div className="socrabuddy-chatgpt-window" onClick={(e) => e.stopPropagation()}>
        {/* Sleek Minimal Header */}
        <header className="socrabuddy-chat-header">
          <div className="socrabuddy-brand">
            <div className="socrabuddy-avatar-badge">
              <Sparkles size={17} className="brand-sparkle-icon" />
            </div>
            <div className="socrabuddy-header-info">
              <div className="socrabuddy-name-row">
                <span className="socrabuddy-name">SocraBuddy</span>
                <span className="socrabuddy-tag">AI Mentor</span>
                <span className="socrabuddy-problem-chip">{problemTitle}</span>
              </div>
            </div>
          </div>

          <div className="socrabuddy-header-actions">
            {/* Minimal Depth Selector */}
            <div className="socrabuddy-depth-selector">
              {HINT_LEVELS.map((item) => (
                <button
                  key={item.level}
                  type="button"
                  className={`depth-pill ${selectedLevel === item.level ? 'active' : ''}`}
                  onClick={() => handleRequestHint(item.level)}
                  disabled={isLoading}
                  title={item.desc}
                >
                  {item.label}
                </button>
              ))}
            </div>

            {messages.length > 0 && (
              <button
                type="button"
                className="btn-chat-icon"
                onClick={() => setMessages([])}
                title="Clear conversation"
              >
                <RotateCcw size={15} />
              </button>
            )}

            <button
              type="button"
              className="btn-chat-icon close-icon"
              onClick={onClose}
              title="Close (Esc)"
              data-testid="socrabuddy-close-btn"
            >
              <X size={17} />
            </button>
          </div>
        </header>

        {/* Error Notification */}
        {error && (
          <div className="socrabuddy-inline-error">
            <AlertCircle size={15} className="flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Chat Messages Stream */}
        <div className="socrabuddy-messages-container">
          {messages.length === 0 && !isLoading && (
            <div className="socrabuddy-welcome-screen">
              <div className="welcome-avatar">
                <Sparkles size={28} />
              </div>
              <h3 className="welcome-title">How can I help you with {problemTitle}?</h3>
              <p className="welcome-subtitle">
                Ask about logic, edge cases, or complexity. I guide you Socratically so you master the concept.
              </p>

              <div className="welcome-prompts-grid">
                {QUICK_PROMPTS.map((prompt, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className="welcome-prompt-card"
                    onClick={() => handleRequestHint(selectedLevel, prompt)}
                  >
                    <MessageSquare size={14} className="prompt-icon" />
                    <span>{prompt}</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg) => (
            <div
              key={msg.id}
              className={`chat-row ${msg.sender === 'user' ? 'user-row' : 'assistant-row'}`}
            >
              {msg.sender === 'assistant' && (
                <div className="message-avatar">
                  <Sparkles size={15} />
                </div>
              )}

              <div className="message-content-wrapper">
                {/* User Bubble */}
                {msg.sender === 'user' && (
                  <div className="user-bubble">
                    <p>{msg.text}</p>
                    <span className="bubble-time">{msg.timestamp}</span>
                  </div>
                )}

                {/* Assistant Socratic Hint (Streaming State) */}
                {msg.sender === 'assistant' && !msg.hintData && msg.text !== undefined && (
                  <div className="assistant-bubble streaming-bubble">
                    <div className="assistant-header-meta">
                      <span className="level-badge">
                        {msg.level ? HINT_LEVELS.find((l) => l.level === msg.level)?.label : 'Hint'}
                      </span>
                      <span className="hint-headline" style={{ opacity: 0.8, fontStyle: 'italic' }}>
                        SocraBuddy typing...
                      </span>
                    </div>
                    <p className="assistant-text">
                      {msg.text || (
                        <span className="streaming-dots">
                          <Loader2 size={13} className="animate-spin inline mr-1" />
                          Formulating Socratic nudge...
                        </span>
                      )}
                    </p>
                  </div>
                )}

                {/* Assistant Socratic Hint (Finalized) */}
                {msg.sender === 'assistant' && msg.hintData && (
                  <div className="assistant-bubble">
                    <div className="assistant-header-meta">
                      <span className="level-badge">
                        {msg.level ? HINT_LEVELS.find((l) => l.level === msg.level)?.label : 'Hint'}
                      </span>
                      <span className="hint-headline">{msg.hintData.title}</span>
                    </div>

                    <p className="assistant-text">{msg.hintData.guidance}</p>

                    {msg.hintData.socraticQuestion && (
                      <div className="socratic-quote-box">
                        <HelpCircle size={15} className="quote-icon" />
                        <div className="quote-text">
                          <span className="quote-label">Key reflection:</span>
                          <p>&ldquo;{msg.hintData.socraticQuestion}&rdquo;</p>
                        </div>
                      </div>
                    )}

                    {msg.hintData.conceptKeywords && msg.hintData.conceptKeywords.length > 0 && (
                      <div className="assistant-concepts-row">
                        {msg.hintData.conceptKeywords.map((kw, i) => (
                          <button
                            key={i}
                            type="button"
                            className="concept-chip"
                            onClick={() => handleRequestHint(selectedLevel, `Explain how ${kw} applies to this problem`)}
                            title={`Ask about ${kw}`}
                          >
                            #{kw}
                          </button>
                        ))}
                      </div>
                    )}

                    <div className="assistant-actions-bar">
                      <div className="actions-left">
                        <span className="meta-time">{msg.timestamp}</span>
                        <button
                          type="button"
                          className="btn-action-ghost"
                          onClick={() =>
                            handleCopyText(
                              msg.id,
                              `${msg.hintData?.title}\n\n${msg.hintData?.guidance}\n\n${msg.hintData?.socraticQuestion}`
                            )
                          }
                          title="Copy response"
                        >
                          {copiedId === msg.id ? (
                            <>
                              <Check size={13} className="text-green" />
                              <span>Copied</span>
                            </>
                          ) : (
                            <>
                              <Copy size={13} />
                              <span>Copy</span>
                            </>
                          )}
                        </button>
                      </div>

                      {msg.level && getNextLevel(msg.level) && (
                        <button
                          type="button"
                          className="btn-next-step"
                          onClick={() => handleRequestHint(getNextLevel(msg.level!)!)}
                          disabled={isLoading}
                        >
                          <span>{HINT_LEVELS.find((l) => l.level === getNextLevel(msg.level!))?.label || 'Next Level'}</span>
                          <ChevronRight size={13} />
                        </button>
                      )}
                    </div>
                  </div>
                )}

                {/* Assistant Diagnosis */}
                {msg.sender === 'assistant' && msg.diagnosisData && (
                  <div className="assistant-bubble diagnosis-bubble">
                    <div className="assistant-header-meta">
                      <span className="level-badge diagnosis-pill">
                        <BrainCircuit size={12} />
                        {msg.diagnosisData.mistakeClassification}
                      </span>
                      <span className="hint-headline">{msg.diagnosisData.diagnosisSummary}</span>
                    </div>

                    <p className="assistant-text">{msg.diagnosisData.socraticGuidance}</p>

                    {msg.diagnosisData.recommendedAction && (
                      <div className="diagnosis-tip-box">
                        <span className="tip-label">Suggested Direction:</span>
                        <p>{msg.diagnosisData.recommendedAction}</p>
                      </div>
                    )}

                    <div className="assistant-actions-bar">
                      <span className="meta-time">{msg.timestamp}</span>
                      <button
                        type="button"
                        className="btn-next-step"
                        onClick={() => handleRequestHint(msg.diagnosisData!.recommendedHintLevel)}
                        disabled={isLoading}
                      >
                        <span>Explore {HINT_LEVELS.find((l) => l.level === msg.diagnosisData?.recommendedHintLevel)?.label || 'Hint'}</span>
                        <ChevronRight size={13} />
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ))}

          {isLoading && (
            <div className="chat-row assistant-row">
              <div className="message-avatar">
                <Sparkles size={15} className="spinner-icon text-yellow" />
              </div>
              <div className="assistant-bubble typing-bubble">
                <Loader2 size={15} className="spinner-icon text-yellow" />
                <span>SocraBuddy is thinking...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Clean Input Bar */}
        <footer className="socrabuddy-chat-footer">
          <div className="socrabuddy-input-container">
            <input
              ref={inputRef}
              type="text"
              className="chat-text-input"
              placeholder="Ask a question about your code or approach..."
              value={userQuestion}
              onChange={(e) => setUserQuestion(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey && !isLoading) {
                  e.preventDefault();
                  if (userQuestion.trim()) {
                    handleRequestHint();
                  }
                }
              }}
              disabled={isLoading}
            />

            <div className="input-right-controls">
              {lastVerdict && lastVerdict !== 'ACCEPTED' && (
                <button
                  type="button"
                  className="btn-diagnose-pill"
                  onClick={handleDiagnose}
                  disabled={isLoading}
                  title="Diagnose submission mistake"
                >
                  <BrainCircuit size={13} />
                  <span>Diagnose</span>
                </button>
              )}

              <button
                type="button"
                className="btn-send-round"
                onClick={() => handleRequestHint()}
                disabled={isLoading || !userQuestion.trim()}
                title="Send message"
              >
                {isLoading ? <Loader2 size={15} className="spinner-icon" /> : <ArrowUp size={16} />}
              </button>
            </div>
          </div>

          <p className="socrabuddy-footer-caption">
            SocraBuddy provides Socratic guidance to build problem-solving intuition without giving away raw solutions.
          </p>
        </footer>
      </div>
    </div>,
    document.body
  );
};
