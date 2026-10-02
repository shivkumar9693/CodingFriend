import React, { useEffect, useState, useRef, useMemo } from 'react';
import {
  Github,
  Save,
  CheckCircle2,
  Phone,
  Linkedin,
  Flame,
  Upload,
  Trash2,
  Swords,
  MapPin,
  Globe,
  Tag,
  ShieldCheck,
  Edit3,
  X,
  ChevronRight,
  Eye,
  CheckSquare,
  MessageSquare,
  HelpCircle,
  Trophy,
  User as UserIcon,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { userService } from '../services/userService';
import { leaderboardService } from '../services/leaderboardService';
import { LeaderboardEntry } from '../types/leaderboard';
import { ActivityHeatmapData } from '../types/duel';
import { LearnerStatistics } from '../types/user';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ApiError } from '../services/apiClient';

export const ProfilePage: React.FC = () => {
  const { user, refreshProfile } = useAuth();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Edit form states
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [fullName, setFullName] = useState('');
  const [bio, setBio] = useState('');
  const [githubUsername, setGithubUsername] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [linkedinUrl, setLinkedinUrl] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [location, setLocation] = useState('');
  const [websiteUrl, setWebsiteUrl] = useState('');
  const [skills, setSkills] = useState<string[]>([]);
  const [skillsInput, setSkillsInput] = useState('');

  // Real Database Telemetry States
  const [userStanding, setUserStanding] = useState<LeaderboardEntry | null>(null);
  const [heatmapData, setHeatmapData] = useState<ActivityHeatmapData | null>(null);
  const [stats, setStats] = useState<LearnerStatistics | null>(null);

  // Notifications
  const [isSaving, setIsSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [validationErrors, setValidationErrors] = useState<Array<{ field: string; message: string }>>([]);

  useEffect(() => {
    if (user) {
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
      const savedSkills = localStorage.getItem(`skills_${user.id}`);
      const initialSkills = savedSkills
        ? JSON.parse(savedSkills)
        : ['Java', 'Algorithms', 'Data Structures'];
      setSkills(initialSkills);
      setSkillsInput(initialSkills.join(', '));

      // Fetch Real Leaderboard Standing
      leaderboardService
        .getLeaderboard()
        .then((entries) => {
          const found = entries.find((e) => e.email === user.email || e.userId === user.id);
          if (found) {
            setUserStanding(found);
          }
        })
        .catch(() => {});

      // Fetch Real 365-Day Activity Heatmap from DB
      userService
        .getActivityHeatmap()
        .then((data) => {
          setHeatmapData(data);
        })
        .catch(() => {});

      // Fetch Real Problem Solving Metrics & Difficulty Breakdown from DB
      userService
        .getLearnerStatistics()
        .then((s) => {
          setStats(s);
        })
        .catch(() => {});
    }
  }, [user]);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setErrorMessage('Please select a valid image file (PNG, JPG, WEBP, GIF).');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setErrorMessage('Image file size must be less than 5MB.');
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
      setErrorMessage('Full name cannot be blank.');
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

      const parsedSkills = skillsInput
        .split(',')
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
      setSkills(parsedSkills);

      if (user?.id) {
        localStorage.setItem(`phone_${user.id}`, phoneNumber.trim());
        localStorage.setItem(`linkedin_${user.id}`, linkedinUrl.trim());
        localStorage.setItem(`location_${user.id}`, location.trim());
        localStorage.setItem(`website_${user.id}`, websiteUrl.trim());
        localStorage.setItem(`skills_${user.id}`, JSON.stringify(parsedSkills));
        if (avatarUrl) {
          localStorage.setItem(`avatar_${user.id}`, avatarUrl);
        } else {
          localStorage.removeItem(`avatar_${user.id}`);
        }
      }

      await refreshProfile();
      setSuccessMessage('Profile updated successfully!');
      setTimeout(() => {
        setIsEditModalOpen(false);
        setSuccessMessage('');
      }, 700);
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

  // 52-week activity computation from real DB ledger
  const { weeks, monthLabels, totalSubmissionsCount } = useMemo(() => {
    const today = new Date();
    const map = new Map<string, number>();

    const formatLocalDate = (date: Date): string => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    if (heatmapData?.dailyActivities) {
      for (const act of heatmapData.dailyActivities) {
        // act.date can be 'YYYY-MM-DD' string
        map.set(String(act.date), act.count);
      }
    }

    const daysList: { dateStr: string; count: number; date: Date }[] = [];
    let runningTotal = 0;

    for (let i = 363; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(d.getDate() - i);
      const dateStr = formatLocalDate(d);
      const count = map.get(dateStr) || 0;
      runningTotal += count;
      daysList.push({ dateStr, count, date: d });
    }

    const groupedWeeks: { dateStr: string; count: number }[][] = [];
    let currentWeek: { dateStr: string; count: number }[] = [];

    for (let i = 0; i < daysList.length; i++) {
      currentWeek.push(daysList[i]);
      if (currentWeek.length === 7 || i === daysList.length - 1) {
        groupedWeeks.push(currentWeek);
        currentWeek = [];
      }
    }

    const months = ['Oct', 'Nov', 'Dec', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep'];

    return {
      weeks: groupedWeeks,
      monthLabels: months,
      totalSubmissionsCount: heatmapData?.totalSubmissions ?? runningTotal,
    };
  }, [heatmapData]);

  const getHeatmapColor = (count: number) => {
    if (count === 0) return '#1b2028';
    if (count <= 2) return '#0e4429';
    if (count <= 5) return '#006d32';
    if (count <= 8) return '#26a641';
    return '#39d353';
  };

  if (!user) {
    return (
      <div className="page-container center-loader">
        <LoadingSpinner message="Loading profile..." />
      </div>
    );
  }

  // Real Values Calculated from DB
  const usernameHandle = user.email.split('@')[0];
  const contestElo = user.contestRating || 1200;
  const duelElo = user.duelRating || 1200;
  const totalSolved = stats?.problemsSolved ?? 0;
  const totalAttempted = stats?.problemsAttempted ?? 0;
  const totalProblems = stats?.totalProblems ?? Math.max(totalSolved, 1);
  const easySolved = stats?.easySolved ?? 0;
  const totalEasy = stats?.totalEasy ?? Math.max(easySolved, 1);
  const mediumSolved = stats?.mediumSolved ?? 0;
  const totalMedium = stats?.totalMedium ?? Math.max(mediumSolved, 1);
  const hardSolved = stats?.hardSolved ?? 0;
  const totalHard = stats?.totalHard ?? Math.max(hardSolved, 1);
  const attendedCount = stats?.contestsAttended ?? 0;

  // Real SVG Circumference Calculations
  const easyPercent = totalEasy > 0 ? (easySolved / totalEasy) * 100 : 0;
  const medPercent = totalMedium > 0 ? (mediumSolved / totalMedium) * 100 : 0;
  const hardPercent = totalHard > 0 ? (hardSolved / totalHard) * 100 : 0;
  const solvePercent = totalProblems > 0 ? Math.min(100, Math.round((totalSolved / totalProblems) * 100)) : 0;

  return (
    <div className="leetcode-profile-container">
      <div className="leetcode-profile-layout">
        {/* =========================================================================
            LEFT COLUMN: User Identity, Bio, Social Links, Skills, Community Stats
           ========================================================================= */}
        <aside className="leetcode-left-sidebar">
          <div className="lc-card lc-user-summary-card">
            {/* Top User Row */}
            <div className="lc-user-top-row">
              <div className="lc-avatar-box">
                {avatarUrl ? (
                  <img src={avatarUrl} alt={fullName || 'Avatar'} className="lc-avatar-img" />
                ) : (
                  <div className="lc-avatar-placeholder">
                    {fullName ? fullName.charAt(0).toUpperCase() : user.email.charAt(0).toUpperCase()}
                  </div>
                )}
              </div>

              <div className="lc-user-name-box">
                <div className="lc-name-with-badge">
                  <h2 className="lc-user-fullname">{fullName || user.email.split('@')[0]}</h2>
                  <ShieldCheck size={16} className="lc-verified-icon" />
                </div>
                <div className="lc-username-handle">@{usernameHandle}</div>
                <div className="lc-global-rank-pill">
                  {userStanding?.rank ? `Rank #${userStanding.rank}` : 'Unranked'}
                </div>
              </div>
            </div>

            {/* Bio / Tagline */}
            <div className="lc-bio-quote">
              {bio || 'Learner preparing algorithms & competitive coding in Java.'}
            </div>

            {/* Followers / Following */}
            <div className="lc-followers-row">
              <span><strong>0</strong> Following</span>
              <span><strong>0</strong> Followers</span>
            </div>

            {/* Edit Profile Button (Triggers Modal) */}
            <button
              type="button"
              className="lc-btn-edit-profile"
              onClick={() => setIsEditModalOpen(true)}
            >
              <Edit3 size={15} /> Edit Profile
            </button>

            {/* User Details & Social Chips */}
            <div className="lc-details-list">
              {location ? (
                <div className="lc-detail-item">
                  <MapPin size={15} className="lc-detail-icon" />
                  <span>{location}</span>
                </div>
              ) : null}

              {websiteUrl ? (
                <div className="lc-detail-item">
                  <Globe size={15} className="lc-detail-icon" />
                  <a href={websiteUrl} target="_blank" rel="noreferrer" className="lc-link">
                    {websiteUrl}
                  </a>
                </div>
              ) : null}

              {githubUsername ? (
                <div className="lc-detail-item">
                  <Github size={15} className="lc-detail-icon" />
                  <a
                    href={`https://github.com/${githubUsername}`}
                    target="_blank"
                    rel="noreferrer"
                    className="lc-link"
                  >
                    {githubUsername}
                  </a>
                </div>
              ) : null}

              {linkedinUrl ? (
                <div className="lc-detail-item">
                  <Linkedin size={15} className="lc-detail-icon" />
                  <a
                    href={`https://linkedin.com/in/${linkedinUrl}`}
                    target="_blank"
                    rel="noreferrer"
                    className="lc-link"
                  >
                    {linkedinUrl}
                  </a>
                </div>
              ) : null}

              {phoneNumber ? (
                <div className="lc-detail-item">
                  <Phone size={15} className="lc-detail-icon" />
                  <span>{phoneNumber}</span>
                </div>
              ) : null}
            </div>

            {/* Skill Tags */}
            {skills.length > 0 && (
              <div className="lc-skills-section">
                <div className="lc-skills-tags-wrap">
                  <Tag size={13} className="lc-tag-icon" />
                  {skills.map((tag, i) => (
                    <span key={i} className="lc-skill-pill">
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Community Stats */}
            <div className="lc-community-stats">
              <h4 className="lc-section-header-sm">Community Stats</h4>
              <div className="lc-comm-row">
                <div className="lc-comm-item">
                  <div className="lc-comm-label">
                    <Eye size={14} className="text-blue-400" /> Views
                  </div>
                  <div className="lc-comm-val">{stats?.totalSubmissions || 0}</div>
                  <div className="lc-comm-sub">Submissions</div>
                </div>
                <div className="lc-comm-item">
                  <div className="lc-comm-label">
                    <CheckSquare size={14} className="text-teal-400" /> Solved
                  </div>
                  <div className="lc-comm-val">{totalSolved}</div>
                  <div className="lc-comm-sub">Accuracy {stats?.accuracyPercentage || 0}%</div>
                </div>
                <div className="lc-comm-item">
                  <div className="lc-comm-label">
                    <MessageSquare size={14} className="text-emerald-400" /> Contests
                  </div>
                  <div className="lc-comm-val">{attendedCount}</div>
                  <div className="lc-comm-sub">Attended</div>
                </div>
              </div>
            </div>
          </div>
        </aside>

        {/* =========================================================================
            RIGHT MAIN: Contest Rating, Solved Gauge, Badges, & Submissions Heatmap
           ========================================================================= */}
        <main className="leetcode-right-main">
          {/* 1. Contest Rating Banner Card */}
          <div className="lc-card lc-contest-banner-card">
            <div className="lc-contest-meta-row">
              <div className="lc-contest-stat-col">
                <span className="lc-meta-label">Contest Rating</span>
                <span className="lc-meta-big-number">{contestElo.toLocaleString()}</span>
              </div>

              <div className="lc-contest-level-col">
                <div className="lc-knight-shield">
                  <Trophy size={20} className="lc-shield-icon" />
                </div>
                <div>
                  <div className="lc-meta-label">Tier</div>
                  <div className="lc-level-name">{userStanding?.tier ? `${userStanding.tier}` : 'Bronze'}</div>
                </div>
              </div>

              <div className="lc-contest-stat-col">
                <span className="lc-meta-label">Global Standing</span>
                <span className="lc-meta-medium-number">
                  {userStanding?.rank ? `#${userStanding.rank}` : 'Unranked'}
                </span>
              </div>

              <div className="lc-contest-stat-col">
                <span className="lc-meta-label">Contests Attended</span>
                <span className="lc-meta-medium-number">{attendedCount}</span>
              </div>

              <div className="lc-contest-stat-col right-align">
                <span className="lc-meta-label">1v1 Duel Rating</span>
                <span className="lc-meta-big-number text-amber">{duelElo} ELO</span>
              </div>
            </div>

            {/* Rating Progression Line Chart (SVG) */}
            <div className="lc-contest-chart-container">
              <div className="lc-chart-svg-wrap">
                <svg viewBox="0 0 700 120" className="lc-sparkline-svg" preserveAspectRatio="none">
                  <defs>
                    <linearGradient id="ratingGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#f59e0b" stopOpacity="0.35" />
                      <stop offset="100%" stopColor="#f59e0b" stopOpacity="0.0" />
                    </linearGradient>
                  </defs>
                  <path
                    d="M 0,110 L 0,95 Q 120,85 240,60 T 480,42 T 650,48 L 700,50 L 700,120 L 0,120 Z"
                    fill="url(#ratingGrad)"
                  />
                  <path
                    d="M 0,95 Q 120,85 240,60 T 480,42 T 650,48 L 700,50"
                    fill="none"
                    stroke="#f59e0b"
                    strokeWidth="2.5"
                  />
                  <circle cx="480" cy="42" r="4.5" fill="#f59e0b" />
                  <g transform="translate(450, 16)">
                    <rect width="60" height="20" rx="4" fill="#1e222d" stroke="#f59e0b" strokeWidth="1" />
                    <text x="30" y="14" fill="#ffffff" fontSize="11" fontWeight="bold" textAnchor="middle">
                      {contestElo}
                    </text>
                  </g>
                </svg>

                {/* Rating Distribution Histogram Bars */}
                <div className="lc-histogram-bars">
                  {[10, 16, 28, 42, 60, 80, 100, 75, 48, 32, 22, 16, 12, 8, 6, 4, 3, 2, 1, 1].map(
                    (val, i) => (
                      <div
                        key={i}
                        className={`lc-hist-bar ${i === 6 ? 'lc-hist-bar-active' : ''}`}
                        style={{ height: `${val}%` }}
                      />
                    )
                  )}
                </div>
              </div>

              <div className="lc-chart-timeline-labels">
                <span>Recent Progress</span>
                <span>Active Season</span>
              </div>
            </div>
          </div>

          {/* 2. Middle Row: Real Solved Problems Gauge & Real Badges Showcase */}
          <div className="lc-middle-row-grid">
            {/* Solved Problems Circular Gauge */}
            <div className="lc-card lc-solved-card">
              <div className="lc-solved-inner">
                <div className="lc-donut-container">
                  <svg className="lc-donut-svg" viewBox="0 0 160 160">
                    <circle cx="80" cy="80" r="66" fill="transparent" stroke="#262c38" strokeWidth="10" className="lc-donut-track" />
                    <circle
                      cx="80"
                      cy="80"
                      r="66"
                      fill="transparent"
                      stroke="#00b8a3"
                      strokeWidth="10"
                      strokeDasharray="415"
                      strokeDashoffset={415 - (415 * Math.min(100, solvePercent)) / 100}
                      strokeLinecap="round"
                    />
                  </svg>
                  <div className="lc-donut-content">
                    <div className="lc-donut-solved-num">{totalSolved}</div>
                    <div className="lc-donut-total">/ {totalProblems}</div>
                    <div className="lc-donut-status">
                      <CheckCircle2 size={12} className="text-teal" /> Solved
                    </div>
                    <div className="lc-donut-attempting">{totalAttempted} Attempted</div>
                  </div>
                </div>

                <div className="lc-diff-breakdown-list">
                  <div className="lc-diff-box lc-diff-easy">
                    <div className="lc-diff-title-row">
                      <span className="lc-diff-name">Easy</span>
                      <span className="lc-diff-ratio">{easySolved}/{totalEasy}</span>
                    </div>
                    <div className="lc-diff-progress-bg">
                      <div className="lc-diff-progress-bar easy-bar" style={{ width: `${Math.min(100, easyPercent)}%` }} />
                    </div>
                  </div>

                  <div className="lc-diff-box lc-diff-med">
                    <div className="lc-diff-title-row">
                      <span className="lc-diff-name">Med.</span>
                      <span className="lc-diff-ratio">{mediumSolved}/{totalMedium}</span>
                    </div>
                    <div className="lc-diff-progress-bg">
                      <div className="lc-diff-progress-bar med-bar" style={{ width: `${Math.min(100, medPercent)}%` }} />
                    </div>
                  </div>

                  <div className="lc-diff-box lc-diff-hard">
                    <div className="lc-diff-title-row">
                      <span className="lc-diff-name">Hard</span>
                      <span className="lc-diff-ratio">{hardSolved}/{totalHard}</span>
                    </div>
                    <div className="lc-diff-progress-bg">
                      <div className="lc-diff-progress-bar hard-bar" style={{ width: `${Math.min(100, hardPercent)}%` }} />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Badges Showcase Card */}
            <div className="lc-card lc-badges-card">
              <div className="lc-badges-header">
                <div>
                  <span className="lc-badges-title">Badges & Streaks</span>
                  <span className="lc-badges-count">{user.maxStreak && user.maxStreak > 0 ? 'Active' : 'Unearned'}</span>
                </div>
                <ChevronRight size={18} className="lc-text-muted" />
              </div>

              <div className="lc-badges-showcase-row">
                <div className="lc-badge-item" title="1v1 Duel Arena Competitor">
                  <div className="lc-badge-hexagon badge-knight">
                    <Swords size={26} className="text-teal-400" />
                  </div>
                </div>

                <div className="lc-badge-item" title="Consistency Streak">
                  <div className="lc-badge-hexagon badge-gold">
                    <span className="badge-text">{user.currentStreak || 0}D</span>
                  </div>
                </div>

                <div className="lc-badge-item" title="Max Daily Record">
                  <div className="lc-badge-hexagon badge-purple">
                    <span className="badge-text">{user.maxStreak || 0}D</span>
                  </div>
                </div>
              </div>

              <div className="lc-recent-badge-footer">
                <span className="lc-recent-badge-lbl">Current Daily Streak</span>
                <span className="lc-recent-badge-name">{user.currentStreak || 0} Days Active</span>
              </div>
            </div>
          </div>

          {/* 3. Bottom Panel: Real Submissions Activity Heatmap */}
          <div className="lc-card lc-heatmap-card">
            <div className="lc-heatmap-header-row">
              <div className="lc-heatmap-left-info">
                <strong>{totalSubmissionsCount}</strong> submissions in the past one year{' '}
                <HelpCircle size={14} className="lc-info-icon" />
              </div>

              <div className="lc-heatmap-right-telemetry">
                <span>
                  Total active days: <strong>{heatmapData?.totalActiveDays || 0}</strong>
                </span>
                <span>
                  Max streak: <strong>{user.maxStreak || heatmapData?.maxStreak || 0}</strong>
                </span>
                <div className="lc-current-streak-chip">
                  <Flame size={13} className="text-orange" />
                  <span>Current {user.currentStreak || 0}</span>
                </div>
              </div>
            </div>

            {/* 52-Week Grid */}
            <div className="lc-heatmap-grid-scroll">
              <div className="lc-heatmap-grid-inner">
                {weeks.map((week, wIdx) => (
                  <div key={wIdx} className="lc-heatmap-col">
                    {week.map((day, dIdx) => (
                      <div
                        key={dIdx}
                        className="lc-heatmap-cell"
                        data-count={day.count}
                        style={{ backgroundColor: getHeatmapColor(day.count) }}
                        title={`${day.count} submissions on ${day.dateStr}`}
                      />
                    ))}
                  </div>
                ))}
              </div>

              {/* Month Footers */}
              <div className="lc-heatmap-months-row">
                {monthLabels.map((m, idx) => (
                  <span key={idx} className="lc-month-lbl">
                    {m}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </main>
      </div>

      {/* =========================================================================
          EDIT PROFILE MODAL (Opens when clicking "Edit Profile")
         ========================================================================= */}
      {isEditModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsEditModalOpen(false)}>
          <div className="modal-content lc-edit-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title-group">
                <Edit3 size={20} className="text-yellow" />
                <h3>Edit Learner Profile</h3>
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
                <label className="form-label font-semibold">Profile Photo (Upload from Device)</label>
                <div className="avatar-upload-row">
                  <div className="avatar-upload-preview">
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
                      id="edit-modal-avatar-input"
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
                  <label htmlFor="modalFullName" className="form-label">
                    Full Name <span className="text-accent">*</span>
                  </label>
                  <div className="input-with-icon">
                    <UserIcon size={16} className="input-prefix-icon" />
                    <input
                      id="modalFullName"
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
                  <label htmlFor="modalLocation" className="form-label">
                    Location
                  </label>
                  <div className="input-with-icon">
                    <MapPin size={16} className="input-prefix-icon" />
                    <input
                      id="modalLocation"
                      type="text"
                      className="form-input has-prefix"
                      placeholder="e.g. India"
                      value={location}
                      onChange={(e) => setLocation(e.target.value)}
                      disabled={isSaving}
                    />
                  </div>
                </div>
              </div>

              <div className="form-row-grid">
                <div className="form-group">
                  <label htmlFor="modalWebsite" className="form-label">
                    Website / Portfolio URL
                  </label>
                  <div className="input-with-icon">
                    <Globe size={16} className="input-prefix-icon" />
                    <input
                      id="modalWebsite"
                      type="text"
                      className="form-input has-prefix"
                      placeholder="e.g. https://chefservice.in"
                      value={websiteUrl}
                      onChange={(e) => setWebsiteUrl(e.target.value)}
                      disabled={isSaving}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="modalGithub" className="form-label">
                    GitHub Username
                  </label>
                  <div className="input-with-icon">
                    <Github size={16} className="input-prefix-icon" />
                    <input
                      id="modalGithub"
                      type="text"
                      className="form-input has-prefix"
                      placeholder="e.g. shivkumar9693"
                      value={githubUsername}
                      onChange={(e) => setGithubUsername(e.target.value)}
                      disabled={isSaving}
                    />
                  </div>
                </div>
              </div>

              <div className="form-row-grid">
                <div className="form-group">
                  <label htmlFor="modalLinkedin" className="form-label">
                    LinkedIn Handle
                  </label>
                  <div className="input-with-icon">
                    <Linkedin size={16} className="input-prefix-icon" />
                    <input
                      id="modalLinkedin"
                      type="text"
                      className="form-input has-prefix"
                      placeholder="e.g. shiv-kumar-thakur-200284272"
                      value={linkedinUrl}
                      onChange={(e) => setLinkedinUrl(e.target.value)}
                      disabled={isSaving}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="modalPhone" className="form-label">
                    Phone Number
                  </label>
                  <div className="input-with-icon">
                    <Phone size={16} className="input-prefix-icon" />
                    <input
                      id="modalPhone"
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
                <label htmlFor="modalSkills" className="form-label">
                  Skills & Technologies (comma separated)
                </label>
                <div className="input-with-icon">
                  <Tag size={16} className="input-prefix-icon" />
                  <input
                    id="modalSkills"
                    type="text"
                    className="form-input has-prefix"
                    placeholder="Java, Spring Boot, PostgreSQL, Docker"
                    value={skillsInput}
                    onChange={(e) => setSkillsInput(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
              </div>

              <div className="form-group">
                <div className="label-row-between">
                  <label htmlFor="modalBio" className="form-label">
                    Bio / Tagline
                  </label>
                  <span className="char-indicator">{bio.length}/500</span>
                </div>
                <textarea
                  id="modalBio"
                  className="form-textarea"
                  rows={3}
                  maxLength={500}
                  placeholder="Tell others about your learning goals and tech stack..."
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  disabled={isSaving}
                />
              </div>

              <div className="modal-footer" style={{ marginTop: '1.25rem' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setIsEditModalOpen(false)}
                  disabled={isSaving}
                >
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSaving}>
                  {isSaving ? <LoadingSpinner size="sm" /> : <><Save size={16} /> Save Changes</>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
