import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Code2, User as UserIcon, LogOut, Menu, X, Sparkles, ShieldCheck, History, LayoutDashboard } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
    setMobileMenuOpen(false);
  };

  const isAdmin = user?.role === 'ADMIN';

  return (
    <header className="navbar-header">
      <div className="navbar-container">
        <Link to={isAuthenticated ? '/dashboard' : '/'} className="navbar-brand">
          <div className="brand-icon-wrapper">
            <Code2 className="brand-icon" size={24} />
          </div>
          <div className="brand-text">
            <span className="brand-title">CodingFriend</span>
            <span className="brand-badge">
              <Sparkles size={10} /> SocraBuddy
            </span>
          </div>
        </Link>

        <nav className="navbar-nav desktop-nav">
          <NavLink
            to="/problems"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            Problems
          </NavLink>
          {isAuthenticated && (
            <>
              <NavLink
                to="/dashboard"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <LayoutDashboard size={14} /> Dashboard
              </NavLink>
              <NavLink
                to="/submissions"
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <History size={14} /> Submissions
              </NavLink>
            </>
          )}
          {isAuthenticated && isAdmin && (
            <NavLink
              to="/admin"
              className={({ isActive }) => `nav-link admin-nav-link ${isActive ? 'active' : ''}`}
            >
              <ShieldCheck size={15} /> Admin Portal
            </NavLink>
          )}
        </nav>

        <div className="navbar-actions desktop-actions">
          {isAuthenticated && user ? (
            <div className="user-profile-menu">
              <Link to="/profile" className="profile-btn">
                <UserIcon size={16} />
                <span>{user.fullName || user.email}</span>
                <span className={`role-tag ${isAdmin ? 'role-admin' : ''}`}>{user.role}</span>
              </Link>
              <button
                onClick={handleLogout}
                className="logout-btn"
                title="Log out of your account"
              >
                <LogOut size={16} />
                <span>Logout</span>
              </button>
            </div>
          ) : (
            <div className="auth-buttons">
              <Link to="/login" className="btn btn-secondary">
                Login
              </Link>
              <Link to="/register" className="btn btn-primary">
                Sign Up
              </Link>
            </div>
          )}
        </div>

        <button
          className="mobile-menu-toggle"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label="Toggle navigation menu"
        >
          {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {mobileMenuOpen && (
        <div className="mobile-nav-drawer">
          <NavLink
            to="/problems"
            className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={() => setMobileMenuOpen(false)}
          >
            Problems
          </NavLink>
          {isAuthenticated && (
            <>
              <NavLink
                to="/dashboard"
                className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                <LayoutDashboard size={16} /> Dashboard
              </NavLink>
              <NavLink
                to="/submissions"
                className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                <History size={16} /> Submissions
              </NavLink>
            </>
          )}
          {isAuthenticated && isAdmin && (
            <NavLink
              to="/admin"
              className={({ isActive }) => `mobile-nav-link admin-nav-link ${isActive ? 'active' : ''}`}
              onClick={() => setMobileMenuOpen(false)}
            >
              <ShieldCheck size={16} /> Admin Portal
            </NavLink>
          )}
          {isAuthenticated && user ? (
            <>
              <NavLink
                to="/profile"
                className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
                onClick={() => setMobileMenuOpen(false)}
              >
                <UserIcon size={16} /> Profile ({user.fullName || user.email})
              </NavLink>
              <button onClick={handleLogout} className="mobile-nav-link logout-link">
                <LogOut size={16} /> Logout
              </button>
            </>
          ) : (
            <div className="mobile-auth-links">
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
      )}
    </header>
  );
};
