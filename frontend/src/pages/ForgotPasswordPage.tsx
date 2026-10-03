import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { KeyRound, Mail, ArrowRight, ArrowLeft, CheckCircle2 } from 'lucide-react';
import { authService } from '../services/authService';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ApiError } from '../services/apiClient';
import '../styles/auth.css';

export const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    if (!email.trim()) {
      setErrorMessage('Please enter your registered email address.');
      return;
    }

    setIsLoading(true);
    try {
      const response = await authService.forgotPassword(email.trim());
      setIsSubmitted(true);
      setSuccessMessage(response.message || 'Password reset link has been sent to your email.');
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setErrorMessage(apiErr.message || 'Failed to request password reset. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-page-container">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-icon-circle">
            <KeyRound size={20} />
          </div>
          <h1 className="auth-title">Forgot Password</h1>
          <p className="auth-subtitle">
            Enter your registered email and we'll send you a secure link to reset your password.
          </p>
        </div>

        <ErrorMessage message={errorMessage} onDismiss={() => setErrorMessage('')} />

        {isSubmitted ? (
          <div className="reset-success-box">
            <div className="reset-success-icon">
              <CheckCircle2 size={36} className="text-emerald-400" />
            </div>
            <h3 className="reset-success-title">Check Your Inbox</h3>
            <p className="reset-success-desc">
              {successMessage}
            </p>

            <div className="p-3 my-3 bg-slate-900/60 rounded-lg border border-slate-800 text-xs text-slate-400 text-left">
              <p className="mb-1">💡 <strong>Quick tips:</strong></p>
              <ul className="list-disc list-inside space-y-1 text-slate-400">
                <li>Check your Spam / Junk folder if it doesn't appear within 1 minute.</li>
                <li>The reset link will automatically expire in <strong>30 minutes</strong>.</li>
              </ul>
            </div>

            <div className="flex flex-col gap-2 mt-4">
              <button
                type="button"
                onClick={() => setIsSubmitted(false)}
                className="text-xs text-blue-400 hover:text-blue-300 transition-colors"
              >
                Didn't receive the email? Try again with another address
              </button>

              <div className="auth-footer mt-2">
                <Link to="/login" className="auth-back-link">
                  <ArrowLeft size={15} />
                  <span>Back to Sign In</span>
                </Link>
              </div>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="auth-form">
            <div className="form-group">
              <label htmlFor="email" className="form-label">
                Registered Email Address
              </label>
              <div className="input-icon-wrapper">
                <Mail className="input-icon" size={16} />
                <input
                  id="email"
                  type="email"
                  className="form-input has-icon"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  autoComplete="email"
                  required
                  disabled={isLoading}
                />
              </div>
            </div>

            <button type="submit" className="submit-btn" disabled={isLoading}>
              {isLoading ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <span>Send Reset Link</span>
                  <ArrowRight size={16} />
                </>
              )}
            </button>

            <div className="auth-footer">
              <Link to="/login" className="auth-back-link">
                <ArrowLeft size={15} />
                <span>Back to Sign In</span>
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
