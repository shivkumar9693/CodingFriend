import React, { useEffect, useState } from 'react';
import { User as UserIcon, Mail, Github, Save, CheckCircle2, Shield, Calendar, Award } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { userService } from '../services/userService';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ApiError } from '../services/apiClient';

export const ProfilePage: React.FC = () => {
  const { user, refreshProfile } = useAuth();

  const [fullName, setFullName] = useState('');
  const [bio, setBio] = useState('');
  const [githubUsername, setGithubUsername] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [validationErrors, setValidationErrors] = useState<Array<{ field: string; message: string }>>([]);

  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setBio(user.bio || '');
      setGithubUsername(user.githubUsername || '');
    }
  }, [user]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setValidationErrors([]);

    if (!fullName.trim()) {
      setErrorMessage('Full name cannot be blank.');
      return;
    }

    setIsSaving(true);
    try {
      await userService.updateProfile({
        fullName: fullName.trim(),
        bio: bio.trim() || null,
        githubUsername: githubUsername.trim() || null,
      });
      await refreshProfile();
      setSuccessMessage('Profile updated successfully!');
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setErrorMessage(apiErr.message || 'Failed to update profile.');
      if (apiErr.validationErrors) {
        setValidationErrors(apiErr.validationErrors);
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (!user) {
    return (
      <div className="page-container center-loader">
        <LoadingSpinner message="Loading profile..." />
      </div>
    );
  }

  return (
    <div className="page-container profile-page">
      <div className="page-header">
        <h1 className="page-title">Learner Profile</h1>
        <p className="page-subtitle">Manage your personal details and platform identity.</p>
      </div>

      <div className="profile-grid">
        {/* Left Side: Summary Card */}
        <div className="profile-summary-card">
          <div className="profile-avatar-wrapper">
            <div className="profile-avatar-large">
              {fullName ? fullName.charAt(0).toUpperCase() : 'U'}
            </div>
            <h2 className="profile-user-name">{fullName || 'Learner'}</h2>
            <span className="profile-user-email">{user?.email}</span>
          </div>

          <div className="profile-meta-list">
            <div className="profile-meta-item">
              <Shield size={16} className="meta-icon" />
              <span>Role:</span>
              <span className="meta-value role-badge">{user?.role || 'USER'}</span>
            </div>
            {user?.createdAt && (
              <div className="profile-meta-item">
                <Calendar size={16} className="meta-icon" />
                <span>Joined:</span>
                <span className="meta-value">
                  {new Date(user.createdAt).toLocaleDateString(undefined, {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                  })}
                </span>
              </div>
            )}
            <div className="profile-meta-item">
              <Award size={16} className="meta-icon" />
              <span>Target Language:</span>
              <span className="meta-value">Java</span>
            </div>
          </div>
        </div>

        {/* Right Side: Edit Form */}
        <div className="profile-form-card">
          <h2 className="card-section-title">Edit Profile Information</h2>

          {successMessage && (
            <div className="success-alert">
              <CheckCircle2 size={18} />
              <span>{successMessage}</span>
            </div>
          )}

          <ErrorMessage
            message={errorMessage}
            validationErrors={validationErrors}
            onDismiss={() => setErrorMessage('')}
          />

          <form onSubmit={handleSubmit} className="profile-form">
            <div className="form-group">
              <label htmlFor="fullName" className="form-label">
                Full Name / Display Handle
              </label>
              <div className="input-icon-wrapper">
                <UserIcon className="input-icon" size={18} />
                <input
                  id="fullName"
                  type="text"
                  className="form-input has-icon"
                  placeholder="Alex Mercer"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                  disabled={isSaving}
                />
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="email" className="form-label">
                Email Address (Read-Only)
              </label>
              <div className="input-icon-wrapper">
                <Mail className="input-icon" size={18} />
                <input
                  id="email"
                  type="email"
                  className="form-input has-icon input-disabled"
                  value={user?.email || ''}
                  disabled
                />
              </div>
              <small className="form-hint">Email address cannot be modified directly.</small>
            </div>

            <div className="form-group">
              <label htmlFor="githubUsername" className="form-label">
                GitHub Username
              </label>
              <div className="input-icon-wrapper">
                <Github className="input-icon" size={18} />
                <input
                  id="githubUsername"
                  type="text"
                  className="form-input has-icon"
                  placeholder="alexmercer-dev"
                  value={githubUsername}
                  onChange={(e) => setGithubUsername(e.target.value)}
                  disabled={isSaving}
                />
              </div>
              <small className="form-hint">Optional: Used to link your coding portfolio.</small>
            </div>

            <div className="form-group">
              <label htmlFor="bio" className="form-label">
                Bio / Learning Goals
              </label>
              <textarea
                id="bio"
                className="form-textarea"
                rows={4}
                maxLength={500}
                placeholder="Aspiring backend engineer focusing on Java data structures, concurrency, and algorithms..."
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                disabled={isSaving}
              />
              <div className="char-count">{bio.length} / 500 characters</div>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={isSaving}>
                {isSaving ? (
                  <LoadingSpinner size="sm" />
                ) : (
                  <>
                    <Save size={16} /> Save Changes
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
