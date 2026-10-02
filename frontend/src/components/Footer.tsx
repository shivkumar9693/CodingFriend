import React from 'react';
import { Link } from 'react-router-dom';
import {
  Code2,
  Sparkles,
  Shield,
  Cpu,
  Terminal,
  Lock,
  Heart,
  CheckCircle2,
  Github,
} from 'lucide-react';

export const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="footer-root" data-testid="app-footer">
      <div className="footer-container">
        {/* Top Hero & Mission Section from README */}
        <div className="footer-top-brand-grid">
          <div className="footer-brand-hero">
            <div className="footer-brand-header">
              <div className="footer-logo-badge">
                <Code2 size={22} className="footer-logo-icon" />
              </div>
              <div className="footer-title-group">
                <span className="footer-brand-name">CodingFriend</span>
                <span className="footer-socrabuddy-pill">
                  <Sparkles size={11} className="mr-1" /> SocraBuddy AI
                </span>
              </div>
            </div>

            <p className="footer-mission-text">
              An online coding platform where learners solve Java coding problems and an AI mentor analyzes
              their mistakes to provide progressive, tiered Socratic hints instead of immediately revealing the solution.
            </p>

            <div className="footer-security-banner">
              <Lock size={13} className="text-yellow" />
              <span>
                <strong>Private Project:</strong> Exclusive property of <strong>Shivkumar</strong> (@shivkumar9693).
                All rights reserved.
              </span>
            </div>
          </div>

          {/* Quick Tech Badges Strip from README */}
          <div className="footer-badges-column">
            <h5 className="footer-col-heading">System Stack</h5>
            <div className="footer-stack-badges">
              <span className="stack-badge badge-java">
                <span className="badge-dot" /> Java 17 LTS
              </span>
              <span className="stack-badge badge-spring">
                <span className="badge-dot" /> Spring Boot 3.3
              </span>
              <span className="stack-badge badge-react">
                <span className="badge-dot" /> React 18 + TS
              </span>
              <span className="stack-badge badge-postgres">
                <span className="badge-dot" /> PostgreSQL 16
              </span>
              <span className="stack-badge badge-redis">
                <span className="badge-dot" /> Redis Cache
              </span>
              <span className="stack-badge badge-ollama">
                <span className="badge-dot" /> Ollama Local AI
              </span>
              <span className="stack-badge badge-docker">
                <span className="badge-dot" /> Docker Sandbox
              </span>
            </div>
          </div>
        </div>

        {/* 4 Multi-Column Information Grid */}
        <div className="footer-columns-grid">
          {/* Column 1: Platform & Features */}
          <div className="footer-nav-col">
            <h6 className="footer-col-title">
              <Terminal size={14} className="col-title-icon" /> Platform & Practice
            </h6>
            <ul className="footer-link-list">
              <li>
                <Link to="/problems">Problem Catalog (Java 17)</Link>
              </li>
              <li>
                <Link to="/contests">Weekly Coding Contests</Link>
              </li>
              <li>
                <Link to="/duels">1v1 Real-Time Duels</Link>
              </li>
              <li>
                <Link to="/leaderboard">Mastery Rankings & Heatmap</Link>
              </li>
              <li>
                <Link to="/dashboard">Learner Dashboard & Progress</Link>
              </li>
            </ul>
          </div>

          {/* Column 2: Socratic AI & Sandbox (README Specs) */}
          <div className="footer-nav-col">
            <h6 className="footer-col-title">
              <Sparkles size={14} className="col-title-icon text-yellow" /> Socratic AI & Sandbox
            </h6>
            <ul className="footer-feature-list">
              <li>
                <CheckCircle2 size={12} className="text-green" />
                <span>Tier 1–5 Progressive Socratic Hints</span>
              </li>
              <li>
                <CheckCircle2 size={12} className="text-green" />
                <span>Self-hosted Ollama (Zero Paid APIs)</span>
              </li>
              <li>
                <CheckCircle2 size={12} className="text-green" />
                <span>Isolated Docker Sandboxed Runner</span>
              </li>
              <li>
                <CheckCircle2 size={12} className="text-green" />
                <span>Shielded Hidden Grading Testcases</span>
              </li>
              <li>
                <CheckCircle2 size={12} className="text-green" />
                <span>Mistake Classification Engine</span>
              </li>
            </ul>
          </div>

          {/* Column 3: Architecture & Engineering */}
          <div className="footer-nav-col">
            <h6 className="footer-col-title">
              <Cpu size={14} className="col-title-icon" /> Architecture
            </h6>
            <ul className="footer-feature-list">
              <li>
                <span className="tech-label">Backend:</span> Spring Boot Modular Monolith
              </li>
              <li>
                <span className="tech-label">Persistence:</span> Flyway Versioned Migrations
              </li>
              <li>
                <span className="tech-label">Security:</span> Stateless JWT & BCrypt Auth
              </li>
              <li>
                <span className="tech-label">Frontend:</span> Monaco Editor & React Router
              </li>
              <li>
                <span className="tech-label">Deploy:</span> Vercel Frontend + Spring Container
              </li>
            </ul>
          </div>

          {/* Column 4: Ownership & Repository Details */}
          <div className="footer-nav-col">
            <h6 className="footer-col-title">
              <Shield size={14} className="col-title-icon" /> Repository & Author
            </h6>
            <div className="footer-author-box">
              <div className="author-row">
                <span className="author-label">Created By:</span>
                <a
                  href="https://github.com/shivkumar9693"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="author-link"
                >
                  <Github size={13} /> Shivkumar (@shivkumar9693)
                </a>
              </div>
              <div className="author-row">
                <span className="author-label">License:</span>
                <span className="text-muted">All Rights Reserved (Private)</span>
              </div>
              <div className="author-row">
                <span className="author-label">Language:</span>
                <span className="text-muted">Java First • Strict Host Isolation</span>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Legal & Credits Bar */}
        <div className="footer-bottom-bar">
          <p className="footer-copyright-text">
            &copy; {currentYear} <strong>Shivkumar</strong>. All Rights Reserved. Built with{' '}
            <Heart size={13} className="text-red inline-icon" /> for passionate Java developers.
          </p>

          <div className="footer-bottom-right-meta">
            <span className="footer-meta-pill">Java 17 Sandbox</span>
            <span className="footer-meta-pill">Ollama Socratic AI</span>
            <span className="footer-meta-pill">Modular Monolith</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
