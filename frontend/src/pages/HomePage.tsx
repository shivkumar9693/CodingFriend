import React from 'react';

interface ArchitectureModule {
  title: string;
  desc: string;
  badge: string;
  status: 'Ready' | 'Planned';
}

export const HomePage: React.FC = () => {
  const modules: ArchitectureModule[] = [
    {
      title: 'Progressive Socratic Mentorship',
      desc: '4-Tier progressive hint ladder (Nudge → Conceptual Insight → Algorithmic Pattern → Targeted Pointer) powered by local Ollama LLMs with strict anti-solution safeguards.',
      badge: 'Tier 1-4 Hints',
      status: 'Planned',
    },
    {
      title: 'Isolated Execution Sandbox',
      desc: 'Dockerized execution worker running untrusted Java code in ephemeral containers with disabled network access, 256MB memory cap, and 5s hard timeout.',
      badge: 'Zero Host Execution',
      status: 'Ready',
    },
    {
      title: 'Modular Monolith Backend',
      desc: 'Java 17 & Spring Boot 3 backend modular monolith partitioning Auth, Problem, Submission, Mistake, Mentor, and Learning domains with clean boundaries.',
      badge: 'Spring Boot 3.3',
      status: 'Ready',
    },
    {
      title: 'Hidden Test Privacy',
      desc: 'Evaluates learner code against comprehensive hidden test suites while ensuring evaluation inputs and internal parameters are never exposed over public APIs.',
      badge: 'Strict Anti-Cheat',
      status: 'Planned',
    },
    {
      title: 'Open-Source & Self-Hostable',
      desc: 'Runs completely on open-source infrastructure (PostgreSQL, Redis, Ollama, Docker Compose) with zero reliance on paid commercial AI APIs.',
      badge: 'Zero API Lock-in',
      status: 'Ready',
    },
    {
      title: 'Automated Failure Diagnostics',
      desc: 'Classifies submission issues into Syntax, Runtime Exception, Assertion Mismatch, and Time Limit Exceeded before dispatching to the mentor engine.',
      badge: 'Smart Diagnostics',
      status: 'Planned',
    },
  ];

  return (
    <div className="home-page">
      <section className="hero">
        <div className="badge">Day 1 Foundation &bull; Production Architecture</div>
        <h1 className="hero-title">Learn Java with Socratic AI Guidance</h1>
        <p className="hero-subtitle">
          Solve algorithmic challenges in Java with diagnostic feedback and progressive hints that guide your reasoning instead of spoiling the solution.
        </p>
        <div className="hero-cta">
          <span className="cta-status">System Architecture Initialized &bull; Day 1 Baseline Verified</span>
        </div>
      </section>

      <section className="architecture-section">
        <div className="section-header">
          <h2 className="section-title">Core System Architecture</h2>
          <p className="section-subtitle">
            Engineered with strict isolation, modular boundaries, and self-hosted AI inference.
          </p>
        </div>

        <div className="grid-cards">
          {modules.map((m, idx) => (
            <div key={idx} className="card">
              <div className="card-top">
                <h3 className="card-title">{m.title}</h3>
                <span className={`status-pill ${m.status.toLowerCase()}`}>{m.status}</span>
              </div>
              <p className="card-desc">{m.desc}</p>
              <div className="card-footer">
                <span className="card-status">{m.badge}</span>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default HomePage;
