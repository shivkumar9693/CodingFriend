import React from 'react';
import './App.css';

export const App: React.FC = () => {
  const modules = [
    {
      title: 'Progressive Socratic Mentorship',
      desc: '4-Tier hint ladder (Nudge → Concept → Algorithm → Pointer) powered by local Ollama LLMs with strict anti-solution leakage.',
      badge: 'Tier 1-4 Hints',
    },
    {
      title: 'Isolated Execution Sandbox',
      desc: 'Secure ephemeral runner isolating untrusted Java code with zero network access and memory caps.',
      badge: 'Zero Host Execution',
    },
    {
      title: 'Modular Monolith Backend',
      desc: 'Spring Boot 3 + Java 17 modular monolith cleanly partitioning Auth, Problem, Submission, and Learning domains.',
      badge: 'Spring Boot 3.3',
    },
  ];

  return (
    <div className="app-container">
      <nav className="navbar">
        <div className="brand">
          <div className="brand-icon">&lt;/&gt;</div>
          <span>AI Coding Mentor</span>
        </div>
        <div className="nav-status">
          <span className="status-dot"></span>
          <span>Foundation Ready</span>
        </div>
      </nav>

      <main className="main-content">
        <section className="hero">
          <div className="badge">Production Resume Project</div>
          <h1 className="hero-title">Learn Java with Socratic AI Guidance</h1>
          <p className="hero-subtitle">
            Solve algorithmic problems in Java with automated diagnostics and progressive hints that guide your thinking rather than spoiling the solution.
          </p>
        </section>

        <section className="grid-cards">
          {modules.map((m, idx) => (
            <div key={idx} className="card">
              <h2 className="card-title">{m.title}</h2>
              <p className="card-desc">{m.desc}</p>
              <span className="card-status">{m.badge}</span>
            </div>
          ))}
        </section>
      </main>

      <footer className="footer">
        AI Coding Mentor &bull; Self-Hostable Socratic Learning Platform &bull; 2026
      </footer>
    </div>
  );
};

export default App;
