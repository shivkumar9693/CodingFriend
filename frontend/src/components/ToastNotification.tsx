import React, { useEffect, useState, useRef } from 'react';
import {
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Info,
  X,
  ExternalLink,
} from 'lucide-react';

export interface ToastNotificationProps {
  type?: 'success' | 'error' | 'info' | 'warning';
  title?: string;
  message: string;
  verdict?: string | null;
  duration?: number; // duration in ms, defaults to 5000ms. Set to 0 to disable auto-close.
  onClose: () => void;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export const ToastNotification: React.FC<ToastNotificationProps> = ({
  type = 'info',
  title,
  message,
  verdict,
  duration = 5000,
  onClose,
  action,
}) => {
  const [isExiting, setIsExiting] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const remainingTimeRef = useRef<number>(duration);
  const startTimeRef = useRef<number>(Date.now());

  const handleDismiss = () => {
    setIsExiting(true);
    setTimeout(() => {
      onClose();
    }, 280); // Wait for exit animation
  };

  useEffect(() => {
    if (duration <= 0) return;

    if (!isPaused) {
      startTimeRef.current = Date.now();
      timerRef.current = setTimeout(() => {
        handleDismiss();
      }, remainingTimeRef.current);
    } else {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        const elapsed = Date.now() - startTimeRef.current;
        remainingTimeRef.current = Math.max(0, remainingTimeRef.current - elapsed);
      }
    }

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [isPaused, duration]);

  const getIcon = () => {
    if (verdict) {
      switch (verdict) {
        case 'ACCEPTED':
          return <CheckCircle2 size={18} className="toast-icon text-success" />;
        case 'WRONG_ANSWER':
          return <XCircle size={18} className="toast-icon text-error" />;
        case 'COMPILE_ERROR':
        case 'RUNTIME_ERROR':
        case 'TIME_LIMIT_EXCEEDED':
          return <AlertTriangle size={18} className="toast-icon text-warning" />;
        default:
          return <Info size={18} className="toast-icon text-info" />;
      }
    }

    switch (type) {
      case 'success':
        return <CheckCircle2 size={18} className="toast-icon text-success" />;
      case 'error':
        return <XCircle size={18} className="toast-icon text-error" />;
      case 'warning':
        return <AlertTriangle size={18} className="toast-icon text-warning" />;
      case 'info':
      default:
        return <Info size={18} className="toast-icon text-info" />;
    }
  };

  const getVerdictLabel = (v?: string | null) => {
    if (!v) return null;
    return v
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  };

  return (
    <div
      className={`floating-toast-card toast-${type} ${isExiting ? 'toast-exit' : 'toast-enter'}`}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      role="alert"
      data-testid="toast-notification"
    >
      <div className="toast-content-wrapper">
        <div className="toast-icon-wrapper">{getIcon()}</div>

        <div className="toast-text-area">
          <div className="toast-header-row">
            {title ? (
              <strong className="toast-title">{title}</strong>
            ) : verdict ? (
              <strong className="toast-title">{getVerdictLabel(verdict)}</strong>
            ) : (
              <strong className="toast-title">
                {type === 'error' ? 'Submission Alert' : type === 'success' ? 'Success' : 'Notice'}
              </strong>
            )}

            {verdict && (
              <span className={`toast-verdict-tag verdict-${verdict.toLowerCase().replace(/_/g, '-')}`}>
                {verdict}
              </span>
            )}
          </div>

          <p className="toast-message-body">{message}</p>

          {action && (
            <button
              type="button"
              className="toast-action-btn"
              onClick={() => {
                action.onClick();
                handleDismiss();
              }}
            >
              <span>{action.label}</span>
              <ExternalLink size={12} />
            </button>
          )}
        </div>

        <button
          type="button"
          className="toast-close-btn"
          onClick={handleDismiss}
          title="Dismiss notification"
          aria-label="Close"
          data-testid="toast-close-btn"
        >
          <X size={15} />
        </button>
      </div>

      {duration > 0 && (
        <div
          className={`toast-progress-bar toast-progress-${type} ${isPaused ? 'paused' : ''}`}
          style={{ animationDuration: `${duration}ms` }}
        />
      )}
    </div>
  );
};
