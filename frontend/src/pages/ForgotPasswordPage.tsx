import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { KeyRound, Mail, ArrowRight, ArrowLeft, CheckCircle2, Sparkles } from 'lucide-react';
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
  const [directResetToken, setDirectResetToken] = useState<string | null>(null);

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
      if (response.resetToken) {
        setDirectResetToken(response.resetToken);
      }
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
            Enter your registered email and we'll generate a secure link to reset your password.
          </p>
        </div>

        <ErrorMessage message={errorMessage} onDismiss={() => setErrorMessage('')} />

        {isSubmitted ? (
          <div className="reset-success-box">
            <div className="reset-success-icon">
              <CheckCircle2 size={36} className="text-emerald-400" />
            </div>
            <h3 className="reset-success-title">Check Your Email</h3>
            <p className="reset-success-desc">
              {successMessage}
            </p>

            {directResetToken && (
              <div className="reset-direct-action">
                <div className="reset-badge">
                  <Sparkles size={14} />
                  <span>Dev Shortcut</span>
                </div>
                <p className="text-xs text-slate-400 mb-2">Direct password reset link:</p>
                <Link
                  to={`/reset-password?token=${directResetToken}`}
                  className="submit-btn direct-reset-btn"
                >
                  <span>Proceed to Reset Password</span>
                  <ArrowRight size={15} />
                </Link>
              </div>
            )}

            <div className="auth-footer mt-4">
              <Link to="/login" className="auth-back-link">
                <ArrowLeft size={15} />
                <span>Back to Sign In</span>
              </Link>
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
