import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Lock, Eye, EyeOff, ArrowRight, CheckCircle2, ShieldCheck, KeyRound } from 'lucide-react';
import { authService } from '../services/authService';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ApiError } from '../services/apiClient';
import '../styles/auth.css';

export const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const initialToken = searchParams.get('token') || '';
  const [token, setToken] = useState(initialToken);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    if (!token.trim()) {
      setErrorMessage('Reset token is missing. Please check your reset link or request a new one.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage('Passwords do not match.');
      return;
    }

    if (newPassword.length < 8) {
      setErrorMessage('Password must be at least 8 characters long.');
      return;
    }

    setIsLoading(true);
    try {
      await authService.resetPassword({
        token: token.trim(),
        newPassword,
      });
      setIsSuccess(true);
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setErrorMessage(apiErr.message || 'Failed to reset password. The link may be invalid or expired.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-page-container">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-icon-circle">
            <ShieldCheck size={20} />
          </div>
          <h1 className="auth-title">Reset Password</h1>
          <p className="auth-subtitle">
            Enter and confirm your new secure password below.
          </p>
        </div>

        <ErrorMessage message={errorMessage} onDismiss={() => setErrorMessage('')} />

        {isSuccess ? (
          <div className="reset-success-box">
            <div className="reset-success-icon">
              <CheckCircle2 size={42} className="text-emerald-400" />
            </div>
            <h3 className="reset-success-title">Password Reset Complete!</h3>
            <p className="reset-success-desc">
              Your password has been updated securely. You can now sign in to your CodingFriend account with your new credentials.
            </p>
            <button
              type="button"
              className="submit-btn mt-4"
              onClick={() => navigate('/login', { replace: true })}
            >
              <span>Sign In with New Password</span>
              <ArrowRight size={16} />
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="auth-form">
            {!initialToken && (
              <div className="form-group">
                <label htmlFor="token" className="form-label">
                  Reset Token
                </label>
                <div className="input-icon-wrapper">
                  <KeyRound className="input-icon" size={16} />
                  <input
                    id="token"
                    type="text"
                    className="form-input has-icon"
                    placeholder="Enter or paste reset token"
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    required
                    disabled={isLoading}
                  />
                </div>
              </div>
            )}

            <div className="form-group">
              <label htmlFor="newPassword" className="form-label">
                New Password
              </label>
              <div className="input-icon-wrapper">
                <Lock className="input-icon" size={16} />
                <input
                  id="newPassword"
                  type={showPassword ? 'text' : 'password'}
                  className="form-input has-icon"
                  placeholder="Min. 8 characters"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  autoComplete="new-password"
                  required
                  disabled={isLoading}
                />
                <button
                  type="button"
                  className="password-toggle-btn"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="confirmPassword" className="form-label">
                Confirm New Password
              </label>
              <div className="input-icon-wrapper">
                <Lock className="input-icon" size={16} />
                <input
                  id="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  className="form-input has-icon"
                  placeholder="Re-enter new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  autoComplete="new-password"
                  required
                  disabled={isLoading}
                />
                <button
                  type="button"
                  className="password-toggle-btn"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                >
                  {showConfirmPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
            </div>

            <button type="submit" className="submit-btn" disabled={isLoading}>
              {isLoading ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <span>Save New Password</span>
                  <ArrowRight size={16} />
                </>
              )}
            </button>

            <div className="auth-footer">
              <p>
                Remembered your password?{' '}
                <Link to="/login" className="auth-link">
                  Sign In
                </Link>
              </p>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
