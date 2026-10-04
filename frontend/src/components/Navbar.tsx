import React, { useState, useRef, useEffect } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import {
  Code2,
  User as UserIcon,
  LogOut,
  Menu,
  X,
  Sparkles,
  ShieldCheck,
  Layers,
  Trophy,
  LifeBuoy,
  Sun,
  Moon,
  Swords,
  BookOpen,
  ChevronDown,
  Search,
  Command,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { SupportModal } from './SupportModal';
import { CommandPalette } from './CommandPalette';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [supportModalOpen, setSupportModalOpen] = useState(false);
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);
  const [openDropdown, setOpenDropdown] = useState<'practice' | 'compete' | 'admin' | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const location = useLocation();
  const navigate = useNavigate();

  // Global shortcut listener for Cmd+K / Ctrl+K
  useEffect(() => {
    const handleGlobalKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {
        e.preventDefault();
        setCommandPaletteOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, []);

  // Close dropdown on outside click or route change
  useEffect(() => {
    const handleOutsideClick = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpenDropdown(null);
      }
    };
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  useEffect(() => {
    setOpenDropdown(null);
    setMobileMenuOpen(false);
  }, [location.pathname]);

  // Close drawer on Escape key and lock body scroll when mobile menu is open
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setMobileMenuOpen(false);
        setOpenDropdown(null);
      }
    };
    if (mobileMenuOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [mobileMenuOpen]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
    setMobileMenuOpen(false);
  };

  const isAdmin = user?.role === 'ADMIN';
  const brandDestination = !isAuthenticated ? '/' : isAdmin ? '/admin' : '/dashboard';
  const userAvatar = user?.id ? localStorage.getItem(`avatar_${user.id}`) : null;

  // Active state checkers for parent dropdown triggers
  const isPracticeActive =
    location.pathname.startsWith('/problems') ||
    location.pathname.startsWith('/notes');

  const isCompeteActive =
    location.pathname.startsWith('/contests') ||
    location.pathname.startsWith('/duels') ||
    location.pathname.startsWith('/leaderboard');

  const isAdminManageActive =
    location.pathname.startsWith('/admin/problems') ||
    location.pathname.startsWith('/admin/contests') ||
    location.pathname.startsWith('/admin/support');

  return (
    <>
      <header className="navbar-header">
        <div className="navbar-container" ref={dropdownRef}>
          {/* Brand Logo - Navigates to /dashboard when authenticated */}
          <Link
            to={brandDestination}
            className="navbar-brand"
            title="CodingFriend Home / Dashboard"
            onClick={() => setMobileMenuOpen(false)}
          >
            <div className="brand-icon-wrapper">
              <Code2 className="brand-icon" size={20} />
            </div>
            <div className="brand-text">
              <span className="brand-title">CodingFriend</span>
              <span className="brand-badge">
                <Sparkles size={11} className="text-yellow" /> SocraBuddy
              </span>
            </div>
          </Link>

          {/* Desktop Navigation Links with Dropdowns */}
          <nav className="navbar-nav desktop-nav">
            {isAuthenticated && isAdmin ? (
              <>
                <NavLink
                  to="/admin"
                  end
                  className={({ isActive }) => `nav-link admin-nav-link ${isActive ? 'active' : ''}`}
                >
                  <ShieldCheck size={15} />
                  <span>Admin Hub</span>
                </NavLink>

                {/* Admin Management Dropdown */}
                <div className="nav-dropdown-wrapper">
                  <button
                    type="button"
                    className={`nav-link nav-dropdown-trigger ${isAdminManageActive ? 'active' : ''}`}
                    onClick={() => setOpenDropdown(openDropdown === 'admin' ? null : 'admin')}
                    aria-expanded={openDropdown === 'admin'}
                  >
                    <Layers size={15} />
                    <span>Manage</span>
                    <ChevronDown size={13} className={`dropdown-chevron ${openDropdown === 'admin' ? 'rotate-180' : ''}`} />
                  </button>

                  {openDropdown === 'admin' && (
                    <div className="nav-dropdown-menu">
                      <Link to="/admin/problems" className="nav-dropdown-item" onClick={() => setOpenDropdown(null)}>
                        <div className="dropdown-item-icon-wrap bg-blue-soft">
                          <Layers size={16} />
                        </div>
                        <div className="dropdown-item-info">
                          <span className="dropdown-item-title">Problems</span>
                          <span className="dropdown-item-desc">Create & curate challenges</span>
                        </div>
                      </Link>
                      <Link to="/admin/contests" className="nav-dropdown-item" onClick={() => setOpenDropdown(null)}>
                        <div className="dropdown-item-icon-wrap bg-yellow-soft">
                          <Trophy size={16} />
                        </div>
                        <div className="dropdown-item-info">
                          <span className="dropdown-item-title">Contests</span>
                          <span className="dropdown-item-desc">Schedule rated rounds</span>
                        </div>
                      </Link>
                      <Link to="/admin/support" className="nav-dropdown-item" onClick={() => setOpenDropdown(null)}>
                        <div className="dropdown-item-icon-wrap bg-purple-soft">
                          <LifeBuoy size={16} />
                        </div>
                        <div className="dropdown-item-info">
                          <span className="dropdown-item-title">Support & Bugs</span>
                          <span className="dropdown-item-desc">Triage learner reports</span>
                        </div>
                      </Link>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <>
                {/* 1. Practice & Learn Dropdown (Problems, Revision Notes) */}
                <div className="nav-dropdown-wrapper">
                  <button
                    type="button"
                    className={`nav-link nav-dropdown-trigger ${isPracticeActive ? 'active' : ''}`}
                    onClick={() => setOpenDropdown(openDropdown === 'practice' ? null : 'practice')}
                    aria-expanded={openDropdown === 'practice'}
                  >
                    <Layers size={15} />
                    <span>Practice & Notes</span>
                    <ChevronDown size={13} className={`dropdown-chevron ${openDropdown === 'practice' ? 'rotate-180' : ''}`} />
                  </button>

                  {openDropdown === 'practice' && (
                    <div className="nav-dropdown-menu">
                      <Link to="/problems" className="nav-dropdown-item" onClick={() => setOpenDropdown(null)}>
                        <div className="dropdown-item-icon-wrap bg-blue-soft">
                          <Layers size={16} />
                        </div>
                        <div className="dropdown-item-info">
                          <span className="dropdown-item-title">Problems</span>
                          <span className="dropdown-item-desc">Java coding challenges & test runner</span>
                        </div>
                      </Link>

                      <Link to="/notes" className="nav-dropdown-item highlight-item" onClick={() => setOpenDropdown(null)}>
                        <div className="dropdown-item-icon-wrap bg-yellow-soft">
                          <BookOpen size={16} />
                        </div>
                        <div className="dropdown-item-info">
                          <div className="item-title-cluster">
                            <span className="dropdown-item-title">Revision Notes</span>
                            <span className="dropdown-item-tag">AI Mnemonic</span>
                          </div>
                          <span className="dropdown-item-desc">Counter-examples & spaced repetition</span>
                        </div>
                      </Link>
                    </div>
                  )}
                </div>

                {/* 2. Compete & Arena Dropdown (Contests, 1v1 Duels, Rankings) */}
                <div className="nav-dropdown-wrapper">
                  <button
                    type="button"
                    className={`nav-link nav-dropdown-trigger ${isCompeteActive ? 'active' : ''}`}
                    onClick={() => setOpenDropdown(openDropdown === 'compete' ? null : 'compete')}
                    aria-expanded={openDropdown === 'compete'}
                  >
                    <Trophy size={15} />
                    <span>Compete</span>
                    <ChevronDown size={13} className={`dropdown-chevron ${openDropdown === 'compete' ? 'rotate-180' : ''}`} />
                  </button>

                  {openDropdown === 'compete' && (
                    <div className="nav-dropdown-menu">
                      <Link to="/contests" className="nav-dropdown-item" onClick={() => setOpenDropdown(null)}>
                        <div className="dropdown-item-icon-wrap bg-yellow-soft">
                          <Trophy size={16} />
                        </div>
                        <div className="dropdown-item-info">
                          <span className="dropdown-item-title">Contests</span>
                          <span className="dropdown-item-desc">Timed rounds & live scoreboards</span>
                        </div>
                      </Link>

                      <Link to="/duels" className="nav-dropdown-item" onClick={() => setOpenDropdown(null)}>
                        <div className="dropdown-item-icon-wrap bg-red-soft">
                          <Swords size={16} />
                        </div>
                        <div className="dropdown-item-info">
                          <div className="item-title-cluster">
                            <span className="dropdown-item-title">1v1 Duels</span>
                            <span className="dropdown-item-tag tag-live">Live</span>
                          </div>
                          <span className="dropdown-item-desc">Head-to-head real-time coding battles</span>
                        </div>
                      </Link>

                      <Link to="/leaderboard" className="nav-dropdown-item" onClick={() => setOpenDropdown(null)}>
                        <div className="dropdown-item-icon-wrap bg-purple-soft">
                          <Trophy size={16} />
                        </div>
                        <div className="dropdown-item-info">
                          <span className="dropdown-item-title">Rankings</span>
                          <span className="dropdown-item-desc">Global standings & tier percentiles</span>
                        </div>
                      </Link>
                    </div>
                  )}
                </div>
              </>
            )}
          </nav>

          {/* Desktop Utility / Actions Cluster */}
          <div className="navbar-actions desktop-actions">
            {/* Global Spotlight Search Trigger */}
            <button
              type="button"
              className="btn-search-trigger"
              onClick={() => setCommandPaletteOpen(true)}
              title="Search problems, topics and actions (Ctrl + K / Cmd + K)"
              data-testid="navbar-spotlight-btn"
            >
              <Search size={14} className="search-icon" />
              <span className="search-placeholder">Quick search...</span>
              <kbd className="search-kbd">
                <Command size={10} />K
              </kbd>
            </button>

            {/* Theme Toggle Button */}
            <button
              onClick={toggleTheme}
              className="theme-toggle-btn"
              title={`Switch to ${theme === 'dark' ? 'Light' : 'Dark'} Mode`}
              aria-label="Toggle theme mode"
            >
              {theme === 'dark' ? (
                <Sun size={17} className="theme-icon sun-icon" />
              ) : (
                <Moon size={17} className="theme-icon moon-icon" />
              )}
            </button>

            {/* Quick Student Help / Bug Report Trigger */}
            {isAuthenticated && !isAdmin && (
              <button
                onClick={() => setSupportModalOpen(true)}
                className="btn-help-trigger"
                title="Get Help or Report a Bug"
              >
                <LifeBuoy size={14} className="help-icon" />
                <span>Help & Bugs</span>
              </button>
            )}

            {/* User Profile Pill & Logout */}
            {isAuthenticated && user ? (
              <div className="user-profile-menu">
                <Link
                  to={isAdmin ? '/admin/profile' : '/profile'}
                  className="profile-btn"
                  title={isAdmin ? 'View Administrator Profile' : 'View & Edit Profile'}
                >
                  <div className="navbar-avatar-wrapper">
                    {userAvatar ? (
                      <img src={userAvatar} alt="Profile" className="navbar-avatar-img" />
                    ) : (
                      <div className="navbar-avatar-initials">
                        {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
                      </div>
                    )}
                  </div>
                  <span className="navbar-user-name">{user.fullName || user.email.split('@')[0]}</span>
                  {isAdmin && <span className="role-tag role-admin">Admin</span>}
                </Link>

                <button
                  onClick={handleLogout}
                  className="logout-btn"
                  title="Log out of your account"
                >
                  <LogOut size={15} />
                  <span>Logout</span>
                </button>
              </div>
            ) : (
              <div className="auth-buttons">
                <Link to="/login" className="btn btn-secondary btn-sm">
                  Login
                </Link>
                <Link to="/register" className="btn btn-primary btn-sm">
                  Sign Up
                </Link>
              </div>
            )}
          </div>

          {/* Mobile & Mid-Screen Menu Toggle Button */}
          <div className="mobile-controls-cluster">
            {/* Quick Spotlight Search on Mobile */}
            <button
              onClick={() => setCommandPaletteOpen(true)}
              className="theme-toggle-btn mobile-header-theme-btn"
              title="Quick search"
              aria-label="Search"
            >
              <Search size={16} />
            </button>

            {/* Quick theme toggle also visible on mobile navbar */}
            <button
              onClick={toggleTheme}
              className="theme-toggle-btn mobile-header-theme-btn"
              title={`Switch to ${theme === 'dark' ? 'Light' : 'Dark'} Mode`}
              aria-label="Toggle theme mode"
            >
              {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
            </button>

            <button
              className={`mobile-menu-toggle ${mobileMenuOpen ? 'is-open' : ''}`}
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-label={mobileMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
              aria-expanded={mobileMenuOpen}
            >
              {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>
      </header>

      {/* Backdrop for Mobile/Mid Screen Drawer */}
      {mobileMenuOpen && (
        <div
          className="mobile-nav-backdrop"
          onClick={() => setMobileMenuOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Mobile & Mid-Screen Navigation Drawer */}
      {mobileMenuOpen && (
        <div className="mobile-nav-drawer" role="dialog" aria-modal="true" aria-label="Mobile Navigation">
          {isAuthenticated && user && (
            <div className="mobile-user-card">
              <div className="navbar-avatar-wrapper avatar-large">
                {userAvatar ? (
                  <img src={userAvatar} alt="Profile" className="navbar-avatar-img" />
                ) : (
                  <div className="navbar-avatar-initials">
                    {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
                  </div>
                )}
              </div>
              <div className="mobile-user-info">
                <div className="mobile-user-name">{user.fullName || 'Learner'}</div>
                <div className="mobile-user-email">{user.email}</div>
                {isAdmin && <span className="role-tag role-admin">Administrator</span>}
              </div>
            </div>
          )}

          <div className="mobile-nav-sections">
            {isAuthenticated && isAdmin ? (
              <>
                <div className="mobile-section-label">Admin Control</div>
                <NavLink
                  to="/admin"
                  end
                  className={({ isActive }) => `mobile-nav-link admin-nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <ShieldCheck size={18} />
                  <span>Admin Hub</span>
                </NavLink>
                <NavLink
                  to="/admin/problems"
                  className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <Layers size={18} />
                  <span>Problems Management</span>
                </NavLink>
                <NavLink
                  to="/admin/contests"
                  className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <Trophy size={18} />
                  <span>Contests Management</span>
                </NavLink>
                <NavLink
                  to="/admin/support"
                  className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <LifeBuoy size={18} />
                  <span>Support & Bugs</span>
                </NavLink>
              </>
            ) : (
              <>
                <div className="mobile-section-label">Learn & Practice</div>
                <NavLink
                  to="/problems"
                  className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <Layers size={18} />
                  <span>Practice Problems</span>
                </NavLink>
                {isAuthenticated && (
                  <NavLink
                    to="/notes"
                    className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    <BookOpen size={18} />
                    <div className="mobile-link-text-wrap">
                      <span>Revision Notes</span>
                      <span className="dropdown-item-tag">AI Mnemonic</span>
                    </div>
                  </NavLink>
                )}

                <div className="mobile-section-label">Arena & Standings</div>
                <NavLink
                  to="/contests"
                  className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <Trophy size={18} />
                  <span>Contests</span>
                </NavLink>
                <NavLink
                  to="/duels"
                  className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <Swords size={18} />
                  <div className="mobile-link-text-wrap">
                    <span>1v1 Duels</span>
                    <span className="dropdown-item-tag tag-live">Live</span>
                  </div>
                </NavLink>
                <NavLink
                  to="/leaderboard"
                  className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <Trophy size={18} />
                  <span>Rankings</span>
                </NavLink>

                {isAuthenticated && (
                  <>
                    <div className="mobile-section-label">Assistance</div>
                    <button
                      type="button"
                      onClick={() => {
                        setMobileMenuOpen(false);
                        setSupportModalOpen(true);
                      }}
                      className="mobile-nav-link mobile-help-btn"
                    >
                      <LifeBuoy size={18} />
                      <span>Get Help / Report Bug</span>
                    </button>
                  </>
                )}
              </>
            )}
          </div>

          <div className="mobile-drawer-footer">
            {isAuthenticated && user ? (
              <>
                <NavLink
                  to={isAdmin ? '/admin/profile' : '/profile'}
                  className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <UserIcon size={18} />
                  <span>My Profile</span>
                </NavLink>
                <button onClick={handleLogout} className="mobile-nav-link mobile-logout-btn">
                  <LogOut size={18} />
                  <span>Sign Out</span>
                </button>
              </>
            ) : (
              <div className="mobile-auth-grid">
                <Link
                  to="/login"
                  className="btn btn-secondary w-full"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Login
                </Link>
                <Link
                  to="/register"
                  className="btn btn-primary w-full"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Sign Up
                </Link>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Global Spotlight Command Palette */}
      <CommandPalette
        isOpen={commandPaletteOpen}
        onClose={() => setCommandPaletteOpen(false)}
        onOpenSupport={() => setSupportModalOpen(true)}
      />

      {/* Student Support & Bug Report Modal */}
      <SupportModal
        isOpen={supportModalOpen}
        onClose={() => setSupportModalOpen(false)}
      />
    </>
  );
};
