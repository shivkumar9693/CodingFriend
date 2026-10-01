import React from 'react';
import { AlertCircle, X } from 'lucide-react';

interface ErrorMessageProps {
  message: string;
  validationErrors?: Array<{ field: string; message: string }>;
  onDismiss?: () => void;
}

export const ErrorMessage: React.FC<ErrorMessageProps> = ({
  message,
  validationErrors,
  onDismiss,
}) => {
  if (!message && (!validationErrors || validationErrors.length === 0)) {
    return null;
  }

  return (
    <div className="error-alert">
      <div className="error-alert-header">
        <AlertCircle className="error-alert-icon" size={20} />
        <div className="error-alert-text">
          <span className="error-alert-title">{message}</span>
          {validationErrors && validationErrors.length > 0 && (
            <ul className="error-alert-list">
              {validationErrors.map((err, idx) => (
                <li key={idx}>
                  <strong>{err.field}:</strong> {err.message}
                </li>
              ))}
            </ul>
          )}
        </div>
        {onDismiss && (
          <button className="error-alert-close" onClick={onDismiss} aria-label="Dismiss error">
            <X size={16} />
          </button>
        )}
      </div>
    </div>
  );
};
