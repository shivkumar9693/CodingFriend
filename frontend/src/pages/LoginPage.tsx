import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Lock, Mail, ArrowRight, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { GoogleLoginButton } from '../components/GoogleLoginButton';
import { BrandLogo } from '../components/BrandLogo';
import { ApiError } from '../services/apiClient';

export const LoginPage: React.FC = () => {
  const { login, loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [validationErrors, setValidationErrors] = useState<Array<{ field: string; message: string }>>([]);

  const returnPath = (location.state as { from?: { pathname: string } })?.from?.pathname;

  const handleGoogleSuccess = React.useCallback(async (idToken: string) => {
    setErrorMessage('');
    setValidationErrors([]);
    setIsLoading(true);
    try {
      const loggedInUser = await loginWithGoogle(idToken);
      if (returnPath && returnPath !== '/login' && returnPath !== '/') {
        navigate(returnPath, { replace: true });
      } else if (loggedInUser.role === 'ADMIN') {
        navigate('/admin', { replace: true });
      } else {
        navigate('/dashboard', { replace: true });
      }
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setErrorMessage(apiErr.message || 'Google authentication failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }, [loginWithGoogle, navigate, returnPath]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setValidationErrors([]);

    if (!email.trim() || !password) {
      setErrorMessage('Please enter both email and password.');
      return;
    }

    setIsLoading(true);
    try {
      const loggedInUser = await login({ email: email.trim(), password });
      if (returnPath && returnPath !== '/login' && returnPath !== '/') {
        navigate(returnPath, { replace: true });
      } else if (loggedInUser.role === 'ADMIN') {
        navigate('/admin', { replace: true });
      } else {
        navigate('/dashboard', { replace: true });
      }
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setErrorMessage(apiErr.message || 'Failed to sign in. Please check your credentials.');
      if (apiErr.validationErrors) {
        setValidationErrors(apiErr.validationErrors);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-page-container">
      <div className="auth-card">
        <div className="auth-header">
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
            <Link to="/" title="CodingFriend Home">
              <BrandLogo size={48} showText={false} />
            </Link>
          </div>
          <h1 className="auth-title">Welcome Back</h1>
          <p className="auth-subtitle">Sign in to continue your algorithmic problem-solving journey with SocraBuddy</p>
        </div>

        <ErrorMessage
          message={errorMessage}
          validationErrors={validationErrors}
          onDismiss={() => setErrorMessage('')}
        />

        <div className="google-auth-container">
          <GoogleLoginButton
            text="continue_with"
            onSuccess={handleGoogleSuccess}
            onError={(err) => setErrorMessage(err)}
            disabled={isLoading}
          />
        </div>

        <div className="auth-divider">
          <span>or continue with email</span>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="email" className="form-label">
              Email Address
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

          <div className="form-group">
            <div className="form-label-row">
              <label htmlFor="password" className="form-label">
                Password
              </label>
              <Link to="/forgot-password" className="forgot-password-link">
                Forgot password?
              </Link>
            </div>
            <div className="input-icon-wrapper">
              <Lock className="input-icon" size={16} />
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                className="form-input has-icon"
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
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

          <button
            type="submit"
            className="submit-btn"
            disabled={isLoading}
          >
            {isLoading ? (
              <LoadingSpinner size="sm" />
            ) : (
              <>
                <span>Sign In to CodingFriend</span>
                <ArrowRight size={16} />
              </>
            )}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Don't have an account?{' '}
            <Link to="/register" className="auth-link">
              Create an account
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};
