import React from 'react';
import { Link } from 'react-router-dom';

export const Navbar: React.FC = () => {
  return (
    <nav className="navbar" role="navigation" aria-label="Main Navigation">
      <Link to="/" className="brand" style={{ textDecoration: 'none' }}>
        <div className="brand-icon">&lt;/&gt;</div>
        <span>AI Coding Mentor</span>
      </Link>
      <div className="nav-links">
        <Link to="/" className="nav-link">Home</Link>
        <span className="nav-status">
          <span className="status-dot"></span>
          <span>Foundation Ready</span>
        </span>
      </div>
    </nav>
  );
};
