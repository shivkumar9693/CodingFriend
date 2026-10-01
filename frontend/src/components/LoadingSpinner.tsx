import React from 'react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  message?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'md',
  message,
}) => {
  return (
    <div className={`spinner-container spinner-${size}`}>
      <div className="spinner-ring" />
      {message && <p className="spinner-message">{message}</p>}
    </div>
  );
};
