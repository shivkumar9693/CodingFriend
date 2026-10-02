import React, { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import {
  ShieldCheck,
  Edit3,
  MapPin,
  Globe,
  Github,
  Linkedin,
  Phone,
  Layers,
  Trophy,
  LifeBuoy,
  Activity,
  Upload,
  Trash2,
  X,
  CheckCircle2,
  Lock,
  Cpu,
  Server,
  Database,
  Bot,
  User as UserIcon,
  Check,
  RefreshCw,
  ArrowRight,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { userService } from '../../services/userService';
import { problemService } from '../../services/problemService';
import { submissionService } from '../../services/submissionService';
import { supportService } from '../../services/supportService';
import { contestService } from '../../services/contestService';
import { ErrorMessage } from '../../components/ErrorMessage';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ApiError } from '../../services/apiClient';

export const AdminProfilePage: React.FC = () => {
  const { user, refreshProfile } = useAuth();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Edit Modal State
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [fullName, setFullName] = useState('');
  const [adminTitle, setAdminTitle] = useState('');
  const [bio, setBio] = useState('');
  const [githubUsername, setGithubUsername] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [linkedinUrl, setLinkedinUrl] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [location, setLocation] = useState('');
  const [websiteUrl, setWebsiteUrl] = useState('');

  // Platform Telemetry State
  const [isLoadingStats, setIsLoadingStats] = useState(true);
  const [problemCount, setProblemCount] = useState(0);
  const [easyCount, setEasyCount] = useState(0);
  const [mediumCount, setMediumCount] = useState(0);
  const [hardCount, setHardCount] = useState(0);
  const [contestCount, setContestCount] = useState(0);
  const [totalSubmissions, setTotalSubmissions] = useState(0);
  const [openTicketsCount, setOpenTicketsCount] = useState(0);
  const [resolvedTicketsCount, setResolvedTicketsCount] = useState(0);

  // Save / Action feedback
  const [isSaving, setIsSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [validationErrors, setValidationErrors] = useState<Array<{ field: string; message: string }>>([]);

  const loadData = async () => {
    if (!user) return;
    setFullName(user.fullName || '');
    setBio(user.bio || '');
    setGithubUsername(user.githubUsername || '');
    const savedPhone = localStorage.getItem(`phone_${user.id}`) || user.phoneNumber || '';
    setPhoneNumber(savedPhone);
    const savedLinkedin = localStorage.getItem(`linkedin_${user.id}`) || user.linkedinUrl || '';
    setLinkedinUrl(savedLinkedin);
    const savedAvatar = localStorage.getItem(`avatar_${user.id}`) || user.avatarUrl || '';
    setAvatarUrl(savedAvatar);
    const savedLoc = localStorage.getItem(`location_${user.id}`) || '';
    setLocation(savedLoc);
    const savedWeb = localStorage.getItem(`website_${user.id}`) || '';
    setWebsiteUrl(savedWeb);
    const savedTitle = localStorage.getItem(`admintitle_${user.id}`) || 'Lead System Administrator';
    setAdminTitle(savedTitle);

    setIsLoadingStats(true);
    try {
      const [problemsRes, contestsRes, subsRes, ticketsRes] = await Promise.all([
        problemService.getProblems({ page: 0, size: 100 }),
        contestService.getAllContests().catch(() => []),
        submissionService.getSubmissions({ page: 0, size: 1 }).catch(() => ({ totalElements: 0 })),
        supportService.getAllTickets().catch(() => []),
      ]);

      const pList = problemsRes.content || [];
      setProblemCount(problemsRes.totalElements || pList.length);
      setEasyCount(pList.filter((p) => p.difficulty === 'EASY').length);
      setMediumCount(pList.filter((p) => p.difficulty === 'MEDIUM').length);
      setHardCount(pList.filter((p) => p.difficulty === 'HARD').length);

      setContestCount(Array.isArray(contestsRes) ? contestsRes.length : 0);
      setTotalSubmissions(subsRes.totalElements || 0);

      const tList = Array.isArray(ticketsRes) ? ticketsRes : [];
      setOpenTicketsCount(tList.filter((t) => t.status === 'OPEN' || t.status === 'IN_PROGRESS').length);
      setResolvedTicketsCount(tList.filter((t) => t.status === 'RESOLVED' || t.status === 'CLOSED').length);
    } catch (err) {
      console.error('Failed to load admin profile telemetry:', err);
    } finally {
      setIsLoadingStats(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setErrorMessage('Please select a valid image file (PNG, JPG, WEBP, GIF).');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setErrorMessage('Image file size must be under 5MB.');
      return;
    }

    setErrorMessage('');
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setAvatarUrl(reader.result);
      }
    };
    reader.readAsDataURL(file);
  };

  const handleRemoveAvatar = () => {
    setAvatarUrl('');
    if (user?.id) {
      localStorage.removeItem(`avatar_${user.id}`);
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setValidationErrors([]);

    if (!fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }

    setIsSaving(true);
    try {
      await userService.updateProfile({
        fullName: fullName.trim(),
        bio: bio.trim() || null,
        githubUsername: githubUsername.trim() || null,
        phoneNumber: phoneNumber.trim() || null,
        avatarUrl: avatarUrl || null,
        linkedinUrl: linkedinUrl.trim() || null,
      });

      if (user?.id) {
        localStorage.setItem(`phone_${user.id}`, phoneNumber.trim());
        localStorage.setItem(`linkedin_${user.id}`, linkedinUrl.trim());
        localStorage.setItem(`location_${user.id}`, location.trim());
        localStorage.setItem(`website_${user.id}`, websiteUrl.trim());
        localStorage.setItem(`admintitle_${user.id}`, adminTitle.trim() || 'Lead System Administrator');
        if (avatarUrl) {
          localStorage.setItem(`avatar_${user.id}`, avatarUrl);
        } else {
          localStorage.removeItem(`avatar_${user.id}`);
        }
      }

      await refreshProfile();
      setSuccessMessage('Administrator profile successfully updated.');
      setTimeout(() => {
        setIsEditModalOpen(false);
        setSuccessMessage('');
      }, 700);
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setErrorMessage(apiErr.message || 'Failed to update administrator profile.');
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
        <LoadingSpinner message="Loading administrator profile..." />
      </div>
    );
  }

  const usernameHandle = user.email.split('@')[0];

  return (
    <div className="admin-profile-page">
      {/* 1. Admin Hero Header */}
      <div className="admin-profile-hero-card">
        <div className="admin-hero-banner-accent" />
        <div className="admin-hero-content">
          <div className="admin-avatar-col">
            <div className="admin-hero-avatar-wrap">
              {avatarUrl ? (
                <img src={avatarUrl} alt={fullName || 'Admin'} className="admin-hero-avatar-img" />
              ) : (
                <div className="admin-hero-avatar-placeholder">
                  {fullName ? fullName.charAt(0).toUpperCase() : user.email.charAt(0).toUpperCase()}
                </div>
              )}
              <div className="admin-avatar-shield-badge" title="Verified System Administrator">
                <ShieldCheck size={18} />
              </div>
            </div>
          </div>

          <div className="admin-hero-info-col">
            <div className="admin-hero-badge-row">
              <span className="admin-clearance-pill">
                <ShieldCheck size={14} /> ROLE_ADMIN
              </span>
              <span className="admin-status-pill status-operational">
                <span className="pulsing-green-dot" /> System Active
              </span>
              <span className="admin-env-pill">
                <Cpu size={13} /> Java 17 / Spring Boot 3.x
              </span>
            </div>

            <h1 className="admin-hero-fullname">{fullName || 'System Administrator'}</h1>
            <div className="admin-hero-handle-row">
              <span className="admin-handle-tag">@{usernameHandle}</span>
              <span className="admin-title-divider">•</span>
              <span className="admin-title-text">{adminTitle || 'Lead System Administrator'}</span>
              <span className="admin-title-divider">•</span>
              <span className="admin-email-text">{user.email}</span>
            </div>

            <p className="admin-hero-bio">
              {bio || 'Directs platform problem curation, competitive contest infrastructure, sandboxed code execution, and Socratic AI mentor prompt engineering.'}
            </p>

            {/* Social & Contact Meta Pills */}
            <div className="admin-meta-links-wrap">
              {location && (
                <span className="admin-meta-pill">
                  <MapPin size={13} /> {location}
                </span>
              )}
              {phoneNumber && (
                <span className="admin-meta-pill">
                  <Phone size={13} /> {phoneNumber}
                </span>
              )}
              {githubUsername && (
                <a
                  href={`https://github.com/${githubUsername}`}
                  target="_blank"
                  rel="noreferrer"
                  className="admin-meta-pill admin-meta-link"
                >
                  <Github size={13} /> github.com/{githubUsername}
                </a>
              )}
              {linkedinUrl && (
                <a
                  href={`https://linkedin.com/in/${linkedinUrl}`}
                  target="_blank"
                  rel="noreferrer"
                  className="admin-meta-pill admin-meta-link"
                >
                  <Linkedin size={13} /> {linkedinUrl}
                </a>
              )}
              {websiteUrl && (
                <a
                  href={websiteUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="admin-meta-pill admin-meta-link"
                >
                  <Globe size={13} /> {websiteUrl}
                </a>
              )}
            </div>
          </div>

          <div className="admin-hero-actions-col">
            <button
              type="button"
              className="btn btn-primary btn-md admin-btn-edit"
              onClick={() => setIsEditModalOpen(true)}
            >
              <Edit3 size={16} /> Edit Admin Profile
            </button>
            <Link to="/admin" className="btn btn-secondary btn-md">
              <Activity size={16} /> Admin Hub
            </Link>
          </div>
        </div>
      </div>

      {/* 2. Platform Overview & Administrative Metric Cards */}
      <div className="admin-section-header-row">
        <div>
          <h2 className="admin-section-heading">Platform Administration Telemetry</h2>
          <p className="admin-section-sub">
            Real-time catalog oversight, contest operations, and isolated execution metrics.
          </p>
        </div>
        <button
          onClick={loadData}
          disabled={isLoadingStats}
          className="btn btn-secondary btn-sm admin-refresh-btn"
          title="Refresh real-time stats"
        >
          <RefreshCw size={14} className={isLoadingStats ? 'animate-spin' : ''} />
          <span>Refresh</span>
        </button>
      </div>

      <div className="admin-metrics-grid">
        {/* Metric 1: Problem Catalog */}
        <div className="admin-stat-card card-problems">
          <div className="admin-stat-icon-wrap icon-problems">
            <Layers size={24} />
          </div>
          <div className="admin-stat-body">
            <span className="admin-stat-label">Catalog Problems</span>
            <span className="admin-stat-value">{isLoadingStats ? '...' : problemCount}</span>
            <div className="admin-stat-breakdown">
              <span className="diff-pill-easy">{easyCount} Easy</span>
              <span className="diff-pill-med">{mediumCount} Med</span>
              <span className="diff-pill-hard">{hardCount} Hard</span>
            </div>
          </div>
          <Link to="/admin/problems" className="admin-stat-card-link">
            <span>Manage Problems</span> <ArrowRight size={14} />
          </Link>
        </div>

        {/* Metric 2: Competitive Contests */}
        <div className="admin-stat-card card-contests">
          <div className="admin-stat-icon-wrap icon-contests">
            <Trophy size={24} />
          </div>
          <div className="admin-stat-body">
            <span className="admin-stat-label">Contests Managed</span>
            <span className="admin-stat-value">{isLoadingStats ? '...' : contestCount}</span>
            <p className="admin-stat-note">Scheduled rounds & arena leaderboards</p>
          </div>
          <Link to="/admin/contests" className="admin-stat-card-link">
            <span>Contest Desk</span> <ArrowRight size={14} />
          </Link>
        </div>

        {/* Metric 3: Support & Bug Reports */}
        <div className="admin-stat-card card-support">
          <div className="admin-stat-icon-wrap icon-support">
            <LifeBuoy size={24} />
          </div>
          <div className="admin-stat-body">
            <span className="admin-stat-label">Support Tickets</span>
            <span className="admin-stat-value">{isLoadingStats ? '...' : openTicketsCount + resolvedTicketsCount}</span>
            <div className="admin-stat-breakdown">
              <span className="ticket-pill-open">{openTicketsCount} Open / Active</span>
              <span className="ticket-pill-resolved">{resolvedTicketsCount} Resolved</span>
            </div>
          </div>
          <Link to="/admin/support" className="admin-stat-card-link">
            <span>Support Inbox</span> <ArrowRight size={14} />
          </Link>
        </div>

        {/* Metric 4: Total Submissions */}
        <div className="admin-stat-card card-execution">
          <div className="admin-stat-icon-wrap icon-execution">
            <Activity size={24} />
          </div>
          <div className="admin-stat-body">
            <span className="admin-stat-label">Submissions Processed</span>
            <span className="admin-stat-value">{isLoadingStats ? '...' : totalSubmissions.toLocaleString()}</span>
            <p className="admin-stat-note">Isolated Docker Worker execution pipeline</p>
          </div>
          <Link to="/admin" className="admin-stat-card-link">
            <span>Execution Stream</span> <ArrowRight size={14} />
          </Link>
        </div>
      </div>

      {/* 3. Dual Column: Security & Role Matrix + Environment Architecture */}
      <div className="admin-details-split-grid">
        {/* Left Column: Security Clearance & Permissions Matrix */}
        <div className="admin-card-panel">
          <div className="admin-panel-header">
            <div className="panel-title-with-icon">
              <ShieldCheck size={20} className="text-red-400" />
              <h3>Security Clearance & Administrative Authority</h3>
            </div>
            <span className="admin-badge-sm">TIER 1 ACCESS</span>
          </div>

          <div className="admin-permissions-list">
            <div className="admin-perm-item">
              <div className="perm-check-icon">
                <Check size={16} />
              </div>
              <div className="perm-content">
                <strong>Problem & Test Suite Authoring</strong>
                <p>Full CRUD permissions over problem statements, starter templates, and hidden grading test cases.</p>
              </div>
            </div>

            <div className="admin-perm-item">
              <div className="perm-check-icon">
                <Check size={16} />
              </div>
              <div className="perm-content">
                <strong>Contest Direction & Rating Recalibration</strong>
                <p>Schedule public and private contests, calibrate Elo rating changes, and enforce proctoring rules.</p>
              </div>
            </div>

            <div className="admin-perm-item">
              <div className="perm-check-icon">
                <Check size={16} />
              </div>
              <div className="perm-content">
                <strong>Docker Sandboxed Code Execution</strong>
                <p>Manage ephemeral container limits (256MB RAM, 1 CPU, 5s timeout, disabled network, non-root JVM).</p>
              </div>
            </div>

            <div className="admin-perm-item">
              <div className="perm-check-icon">
                <Check size={16} />
              </div>
              <div className="perm-content">
                <strong>Socratic AI Hint Guardrails</strong>
                <p>Oversee progressive hint generation (Level 1 Nudge to Level 4 Logic Pointer) via local Ollama models.</p>
              </div>
            </div>

            <div className="admin-perm-item">
              <div className="perm-check-icon">
                <Check size={16} />
              </div>
              <div className="perm-content">
                <strong>Support Inbox & Bug Triage</strong>
                <p>Inspect student stack traces, update ticket resolution status, and submit administrative fix notes.</p>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: System Infrastructure & Telemetry Specs */}
        <div className="admin-card-panel">
          <div className="admin-panel-header">
            <div className="panel-title-with-icon">
              <Server size={20} className="text-teal-400" />
              <h3>Infrastructure & Telemetry Architecture</h3>
            </div>
            <span className="admin-badge-sm badge-teal">LOCAL STACK</span>
          </div>

          <div className="admin-specs-grid">
            <div className="admin-spec-card">
              <div className="spec-icon text-blue-400">
                <Cpu size={18} />
              </div>
              <div className="spec-info">
                <span className="spec-label">Backend Host</span>
                <span className="spec-val">Spring Boot 3.x / Java 17</span>
                <span className="spec-sub">com.aicodingmentor Modular Monolith</span>
              </div>
            </div>

            <div className="admin-spec-card">
              <div className="spec-icon text-purple-400">
                <Bot size={18} />
              </div>
              <div className="spec-info">
                <span className="spec-label">AI Inference Engine</span>
                <span className="spec-val">Ollama Local LLM</span>
                <span className="spec-sub">qwen2.5-coder / deepseek-coder (Zero API keys)</span>
              </div>
            </div>

            <div className="admin-spec-card">
              <div className="spec-icon text-amber-400">
                <Database size={18} />
              </div>
              <div className="spec-info">
                <span className="spec-label">Persistence & Migrations</span>
                <span className="spec-val">PostgreSQL 16 + Flyway</span>
                <span className="spec-sub">V1 to V8 migration schemas active</span>
              </div>
            </div>

            <div className="admin-spec-card">
              <div className="spec-icon text-rose-400">
                <Lock size={18} />
              </div>
              <div className="spec-info">
                <span className="spec-label">Security & Isolation</span>
                <span className="spec-val">Stateless JWT + Sandbox</span>
                <span className="spec-sub">Network isolated execution container</span>
              </div>
            </div>
          </div>

          <div className="admin-quick-links-box">
            <h4 className="quick-links-title">Quick Administrative Shortcuts</h4>
            <div className="quick-links-row">
              <Link to="/admin/problems" className="quick-link-btn">
                <Layers size={14} /> Problems Catalog
              </Link>
              <Link to="/admin/contests" className="quick-link-btn">
                <Trophy size={14} /> Contest Wizard
              </Link>
              <Link to="/admin/support" className="quick-link-btn">
                <LifeBuoy size={14} /> Support Queue
              </Link>
              <Link to="/admin" className="quick-link-btn">
                <Activity size={14} /> System Audit Hub
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* =========================================================================
          EDIT ADMIN PROFILE MODAL
         ========================================================================= */}
      {isEditModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsEditModalOpen(false)}>
          <div className="modal-content lc-edit-modal admin-edit-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title-group">
                <ShieldCheck size={22} className="text-red-500" />
                <h3>Edit Administrator Profile</h3>
              </div>
              <button
                type="button"
                className="btn-modal-close"
                onClick={() => setIsEditModalOpen(false)}
              >
                <X size={18} />
              </button>
            </div>

            {successMessage && (
              <div className="success-alert" style={{ margin: '1rem 1.5rem 0' }}>
                <CheckCircle2 size={18} />
                <span>{successMessage}</span>
              </div>
            )}

            <ErrorMessage
              message={errorMessage}
              validationErrors={validationErrors}
              onDismiss={() => setErrorMessage('')}
            />

            <form onSubmit={handleSaveProfile} className="modal-body lc-edit-form">
              {/* Device Avatar Upload */}
              <div className="avatar-upload-card">
                <label className="form-label font-semibold">Administrator Photo (Upload from Device)</label>
                <div className="avatar-upload-row">
                  <div className="avatar-upload-preview admin-upload-preview">
                    {avatarUrl ? (
                      <img src={avatarUrl} alt="Avatar Preview" className="upload-preview-img" />
                    ) : (
                      <div className="upload-preview-fallback">
                        {fullName ? fullName.charAt(0).toUpperCase() : user.email.charAt(0).toUpperCase()}
                      </div>
                    )}
                  </div>

                  <div className="avatar-upload-controls">
                    <input
                      type="file"
                      ref={fileInputRef}
                      onChange={handleFileUpload}
                      accept="image/png, image/jpeg, image/webp, image/gif"
                      className="hidden-file-input"
                      id="admin-modal-avatar-input"
                    />
                    <div className="avatar-action-buttons">
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => fileInputRef.current?.click()}
                      >
                        <Upload size={14} /> Choose Image
                      </button>
                      {avatarUrl && (
                        <button
                          type="button"
                          className="btn btn-danger-outline btn-sm"
                          onClick={handleRemoveAvatar}
                        >
                          <Trash2 size={14} /> Remove Photo
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              <div className="form-row-grid">
                <div className="form-group">
                  <label htmlFor="adminFullName" className="form-label">
                    Full Name <span className="text-accent">*</span>
                  </label>
                  <div className="input-with-icon">
                    <UserIcon size={16} className="input-prefix-icon" />
                    <input
                      id="adminFullName"
                      type="text"
                      className="form-input has-prefix"
                      placeholder="e.g. Shiv Kumar"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      required
                      disabled={isSaving}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="adminTitleField" className="form-label">
                    Administrative Title / Role
                  </label>
                  <div className="input-with-icon">
                    <ShieldCheck size={16} className="input-prefix-icon" />
                    <input
                      id="adminTitleField"
                      type="text"
                      className="form-input has-prefix"
                      placeholder="e.g. Lead System Architect"
                      value={adminTitle}
                      onChange={(e) => setAdminTitle(e.target.value)}
                      disabled={isSaving}
                    />
                  </div>
                </div>
              </div>

              <div className="form-row-grid">
                <div className="form-group">
                  <label htmlFor="adminLocation" className="form-label">
                    Location
                  </label>
                  <div className="input-with-icon">
                    <MapPin size={16} className="input-prefix-icon" />
                    <input
                      id="adminLocation"
                      type="text"
                      className="form-input has-prefix"
                      placeholder="e.g. India"
                      value={location}
                      onChange={(e) => setLocation(e.target.value)}
                      disabled={isSaving}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="adminPhone" className="form-label">
                    Contact / Phone Number
                  </label>
                  <div className="input-with-icon">
                    <Phone size={16} className="input-prefix-icon" />
                    <input
                      id="adminPhone"
                      type="tel"
                      className="form-input has-prefix"
                      placeholder="e.g. +91 9876543210"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                      disabled={isSaving}
                    />
                  </div>
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="adminBio" className="form-label">
                  Administrative Bio / Directive
                </label>
                <textarea
                  id="adminBio"
                  rows={3}
                  className="form-textarea"
                  placeholder="Describe your role, responsibilities, and platform management mission..."
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  disabled={isSaving}
                />
              </div>

              <div className="form-row-grid">
                <div className="form-group">
                  <label htmlFor="adminGithub" className="form-label">
                    GitHub Username
                  </label>
                  <div className="input-with-icon">
                    <Github size={16} className="input-prefix-icon" />
                    <input
                      id="adminGithub"
                      type="text"
                      className="form-input has-prefix"
                      placeholder="e.g. shivkumar"
                      value={githubUsername}
                      onChange={(e) => setGithubUsername(e.target.value)}
                      disabled={isSaving}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="adminLinkedin" className="form-label">
                    LinkedIn Username / Slug
                  </label>
                  <div className="input-with-icon">
                    <Linkedin size={16} className="input-prefix-icon" />
                    <input
                      id="adminLinkedin"
                      type="text"
                      className="form-input has-prefix"
                      placeholder="e.g. shiv-kumar"
                      value={linkedinUrl}
                      onChange={(e) => setLinkedinUrl(e.target.value)}
                      disabled={isSaving}
                    />
                  </div>
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="adminWebsite" className="form-label">
                  Website / Portfolio
                </label>
                <div className="input-with-icon">
                  <Globe size={16} className="input-prefix-icon" />
                  <input
                    id="adminWebsite"
                    type="url"
                    className="form-input has-prefix"
                    placeholder="https://example.com"
                    value={websiteUrl}
                    onChange={(e) => setWebsiteUrl(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
              </div>

              <div className="modal-footer" style={{ padding: '1rem 0 0', marginTop: '1rem' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setIsEditModalOpen(false)}
                  disabled={isSaving}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isSaving}
                >
                  {isSaving ? 'Saving Changes...' : 'Save Profile'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
