import React, { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import {
  Sparkles,
  Shield,
  ArrowRight,
  Layers,
  Code2,
  Terminal,
  Zap,
  ChevronRight,
  Check,
  Flame,
  BrainCircuit,
  Lock,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const HomePage: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [activeHintLevel, setActiveHintLevel] = useState<number>(1);
  const [activeFaq, setActiveFaq] = useState<number | null>(0);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const hintDemos = [
    {
      level: 1,
      title: 'Level 1: Gentle Nudge',
      tag: 'OBSERVATIONAL',
      aiResponse: 'Take a close look at line 7 in your hash map lookup. What happens when the complement target - nums[i] is identical to nums[i] at the same index?',
      benefit: 'Encourages self-discovery by pointing your attention without giving away the logic.',
    },
    {
      level: 2,
      title: 'Level 2: Conceptual Insight',
      tag: 'EDGE CASE',
      aiResponse: 'Remember that each element can only be used once. If your map stores indices as you iterate, ensure you do not pair an index with itself before saving.',
      benefit: 'Highlights the underlying algorithmic invariant or constraint violation.',
    },
    {
      level: 3,
      title: 'Level 3: Algorithmic Pattern',
      tag: 'DATA STRUCTURE',
      aiResponse: 'Consider a One-Pass Hash Table pattern: as you visit nums[i], check if target - nums[i] is already in the map. If yes, return both indices; otherwise insert (nums[i], i).',
      benefit: 'Provides structural design patterns without revealing verbatim solution code.',
    },
    {
      level: 4,
      title: 'Level 4: Targeted Pointer',
      tag: 'SYNTAX & LOGIC',
      aiResponse: 'Refactor your loop to check map.containsKey(complement) and verify map.get(complement) != i before creating your return array.',
      benefit: 'Pinpoints specific construct corrections while keeping your hands on the keyboard.',
    },
  ];

  const problemCategories = [
    {
      title: 'Arrays & Two Pointers',
      count: '4 Challenges',
      desc: 'Master two-pointer traversals, index mapping, and sliding windows.',
      tags: ['ARRAY', 'TWO_POINTERS', 'HASH_TABLE'],
      highlight: 'Two Sum, Valid Palindrome',
      slug: 'two-sum',
    },
    {
      title: 'Dynamic Programming',
      count: '2 Challenges',
      desc: "Develop Kadane's algorithm, state transitions, and optimal subproblems.",
      tags: ['DYNAMIC_PROGRAMMING', 'DIVIDE_AND_CONQUER'],
      highlight: 'Maximum Subarray, Stock Buying',
      slug: 'maximum-subarray',
    },
    {
      title: 'Hash Tables & Sets',
      count: '3 Challenges',
      desc: 'O(1) lookups, frequency hashing, and duplicate detection.',
      tags: ['HASH_SET', 'SORTING', 'STRING'],
      highlight: 'Contains Duplicate, Valid Anagram',
      slug: 'contains-duplicate',
    },
    {
      title: 'Strings & Simulation',
      count: '2 Challenges',
      desc: 'In-place character manipulation, palindrome verification, and simulation.',
      tags: ['STRING', 'SIMULATION', 'MATH'],
      highlight: 'Reverse String, Fizz Buzz',
      slug: 'reverse-string',
    },
  ];

  const faqs = [
    {
      q: 'How does SocraBuddy Socratic Hinting differ from normal AI code generation?',
      a: 'Standard AI tools give away full solution code immediately, which harms retention and interview readiness. Our SocraBuddy Socratic Mentor never generates copy-paste solutions. Instead, it diagnoses your exact mistake and gives progressive nudges that train your problem-solving intuition.',
    },
    {
      q: 'Is untrusted Java code executed safely?',
      a: 'Yes. Learner code is strictly isolated inside dedicated containerized workers with hard memory caps (256MB), CPU quotas, wall-clock timeouts, and completely disabled network access (--network none). Code never runs in the host application JVM.',
    },
    {
      q: 'Can learners scrape or inspect hidden test cases?',
      a: 'No. The backend enforces strict separation between public sample test cases (for debugging) and hidden grading test cases. Hidden test inputs and assertions are never transmitted through student-facing APIs.',
    },
    {
      q: 'Which programming language is supported?',
      a: 'Java 17 (and Java 21 LTS) is the primary first-class citizen. Starter templates, compiler diagnostic pipelines, and test harnesses are specifically optimized for standard Java object-oriented design.',
    },
  ];

  return (
    <div className="home-page-root">
      {/* 1. Hero Section with Live IDE Simulation */}
      <section className="hero-section">
        <div className="hero-badge">
          <Flame size={14} className="text-red" />
          <span>Next-Gen Java Algorithm Mastery</span>
          <span className="badge-sep">•</span>
          <span className="text-yellow">SocraBuddy AI Mentor</span>
        </div>

        <h1 className="hero-heading">
          Stop Copy-Pasting Solutions. <br />
          <span className="text-gradient">Learn How To Think In Java.</span>
        </h1>

        <p className="hero-lead">
          Solve algorithmic challenges with SocraBuddy, an AI mentor that pinpoints your logical,
          syntactic, and runtime mistakes through tiered Socratic nudges — without spoiling the answer.
        </p>

        <div className="hero-action-buttons">
          <Link to="/problems" className="btn btn-primary btn-lg">
            <span>Explore Challenges</span>
            <ArrowRight size={18} />
          </Link>
          {!isAuthenticated ? (
            <Link to="/register" className="btn btn-yellow btn-lg">
              <span>Start Free Account</span>
              <Sparkles size={18} />
            </Link>
          ) : (
            <Link to="/submissions" className="btn btn-secondary btn-lg">
              <span>View Your Progress</span>
              <Terminal size={18} />
            </Link>
          )}
        </div>

        {/* Live Interactive Hero IDE / Socratic Simulator */}
        <div className="hero-ide-preview">
          <div className="ide-window-header">
            <div className="window-dots">
              <span className="dot dot-red"></span>
              <span className="dot dot-yellow"></span>
              <span className="dot dot-green"></span>
            </div>
            <div className="window-filename">
              <Code2 size={14} className="text-yellow" />
              <span>Solution.java — TwoSum.java (Java 17 OpenJDK)</span>
            </div>
            <div className="window-pill">
              <Zap size={12} className="text-red" />
              <span>Diagnostic Active</span>
            </div>
          </div>

          <div className="ide-window-body">
            <div className="ide-code-panel font-mono">
              <div className="code-line">
                <span className="ln">1</span>
                <span className="kw">class</span> <span className="cls">Solution</span> &#123;
              </div>
              <div className="code-line">
                <span className="ln">2</span>
                <span className="pl-4">
                  <span className="kw">public int</span>[] <span className="fn">twoSum</span>(<span className="kw">int</span>[] nums, <span className="kw">int</span> target) &#123;
                </span>
              </div>
              <div className="code-line">
                <span className="ln">3</span>
                <span className="pl-8">
                  Map&lt;Integer, Integer&gt; map = <span className="kw">new</span> HashMap&lt;&gt;();
                </span>
              </div>
              <div className="code-line">
                <span className="ln">4</span>
                <span className="pl-8">
                  <span className="kw">for</span> (<span className="kw">int</span> i = 0; i &lt; nums.length; i++) &#123;
                </span>
              </div>
              <div className="code-line code-line-highlight">
                <span className="ln">5</span>
                <span className="pl-12">
                  <span className="kw">int</span> complement = target - nums[i];
                </span>
              </div>
              <div className="code-line code-line-error">
                <span className="ln">6</span>
                <span className="pl-12">
                  <span className="kw">if</span> (map.containsKey(complement)) &#123;
                </span>
                <span className="error-squiggle">▲ Found pair!</span>
              </div>
              <div className="code-line">
                <span className="ln">7</span>
                <span className="pl-16">
                  <span className="kw">return new int</span>[] &#123; map.get(complement), i &#125;;
                </span>
              </div>
              <div className="code-line">
                <span className="ln">8</span>
                <span className="pl-12">&#125;</span>
              </div>
              <div className="code-line">
                <span className="ln">9</span>
                <span className="pl-12">map.put(nums[i], i);</span>
              </div>
              <div className="code-line">
                <span className="ln">10</span>
                <span className="pl-8">&#125;</span>
              </div>
              <div className="code-line">
                <span className="ln">11</span>
                <span className="pl-8">
                  <span className="kw">return new int</span>[]&#123;&#125;;
                </span>
              </div>
              <div className="code-line">
                <span className="ln">12</span>
                <span className="pl-4">&#125;</span>
              </div>
              <div className="code-line">
                <span className="ln">13</span>
                &#125;
              </div>
            </div>

            {/* Socratic Mentor Diagnostic Box */}
            <div className="ide-mentor-panel">
              <div className="mentor-panel-header">
                <div className="mentor-avatar">
                  <BrainCircuit size={18} />
                </div>
                <div>
                  <h4 className="mentor-name">SocraBuddy AI Mentor</h4>
                  <span className="mentor-status">● Diagnosing solution state</span>
                </div>
              </div>

              {/* Hint Level Selector */}
              <div className="hint-tier-selector">
                {[1, 2, 3, 4].map((lvl) => (
                  <button
                    key={lvl}
                    type="button"
                    className={`hint-tier-btn ${activeHintLevel === lvl ? 'active' : ''}`}
                    onClick={() => setActiveHintLevel(lvl)}
                  >
                    Level {lvl}
                  </button>
                ))}
              </div>

              <div className="mentor-dialogue-card">
                <div className="mentor-badge-row">
                  <span className="mentor-tier-pill">
                    {hintDemos[activeHintLevel - 1].title}
                  </span>
                  <span className="mentor-tag-pill">
                    {hintDemos[activeHintLevel - 1].tag}
                  </span>
                </div>
                <p className="mentor-dialogue-text">
                  "{hintDemos[activeHintLevel - 1].aiResponse}"
                </p>
                <div className="mentor-benefit-row">
                  <Sparkles size={13} className="text-yellow" />
                  <span>{hintDemos[activeHintLevel - 1].benefit}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Platform Telemetry Metrics Strip */}
      <section className="telemetry-strip">
        <div className="telemetry-item">
          <span className="telemetry-number text-yellow">4-Tier</span>
          <span className="telemetry-label">Progressive SocraBuddy Nudges</span>
        </div>
        <div className="telemetry-divider"></div>
        <div className="telemetry-item">
          <span className="telemetry-number text-red">Java 17/21</span>
          <span className="telemetry-label">First-Class Execution Target</span>
        </div>
        <div className="telemetry-divider"></div>
        <div className="telemetry-item">
          <span className="telemetry-number text-yellow">0% Leakage</span>
          <span className="telemetry-label">Hidden Test Case Privacy</span>
        </div>
        <div className="telemetry-divider"></div>
        <div className="telemetry-item">
          <span className="telemetry-number text-red">100% Isolated</span>
          <span className="telemetry-label">Container Sandbox Security</span>
        </div>
      </section>

      {/* 3. The 4-Step Socratic Mastery Workflow */}
      <section className="workflow-section">
        <div className="section-heading-wrapper">
          <h2 className="features-heading">The SocraBuddy Socratic Engine</h2>
          <p className="features-subheading">
            How our AI guides you from syntax flaws to optimal interview-ready algorithms.
          </p>
        </div>

        <div className="workflow-steps-grid">
          <div className="workflow-step-card">
            <div className="step-number-box">01</div>
            <div className="step-icon-circle bg-red">
              <Terminal size={22} />
            </div>
            <h3>Write in Monaco IDE</h3>
            <p>
              Develop Java solutions directly in an interactive Monaco editor with local autosave,
              instant resets, and full syntax highlighting.
            </p>
          </div>

          <div className="workflow-step-card">
            <div className="step-number-box">02</div>
            <div className="step-icon-circle bg-yellow">
              <Shield size={22} />
            </div>
            <h3>Isolated Grading Suite</h3>
            <p>
              Your code is verified against comprehensive hidden test cases inside an isolated
              execution sandbox with memory caps and zero network access.
            </p>
          </div>

          <div className="workflow-step-card">
            <div className="step-number-box">03</div>
            <div className="step-icon-circle bg-red">
              <BrainCircuit size={22} />
            </div>
            <h3>Mistake Classification</h3>
            <p>
              The engine automatically classifies syntactic, runtime, timeout, or edge-case
              logical mistakes before triggering guidance.
            </p>
          </div>

          <div className="workflow-step-card">
            <div className="step-number-box">04</div>
            <div className="step-icon-circle bg-yellow">
              <Sparkles size={22} />
            </div>
            <h3>Progressive Nudges</h3>
            <p>
              Receive observational questions and conceptual hints from SocraBuddy that nudge you toward the
              solution without writing the code for you.
            </p>
          </div>
        </div>
      </section>

      {/* 4. Curated Challenge Tracks */}
      <section className="tracks-section">
        <div className="section-heading-wrapper">
          <div className="badge-outline">Curated Algorithm Tracks</div>
          <h2 className="features-heading">Targeted Java Interview Prep</h2>
          <p className="features-subheading">
            Filter by pattern or difficulty and jump straight into solving.
          </p>
        </div>

        <div className="tracks-grid">
          {problemCategories.map((cat, idx) => (
            <Link to={`/problems/${cat.slug}`} key={idx} className="track-card">
              <div className="track-card-header">
                <div className="track-icon-box">
                  <Layers size={20} />
                </div>
                {isAuthenticated ? (
                  <span className="track-count flex-align-gap text-yellow">
                    <Zap size={13} /> {cat.count}
                  </span>
                ) : (
                  <span className="track-count flex-align-gap text-muted">
                    <Lock size={12} /> Login required
                  </span>
                )}
              </div>
              <h3 className="track-title">{cat.title}</h3>
              <p className="track-desc">{cat.desc}</p>
              <div className="track-tags-row">
                {cat.tags.map((t) => (
                  <span key={t} className="track-tag-chip">
                    {t}
                  </span>
                ))}
              </div>
              <div className="track-footer">
                <span className="track-featured">Featured: {cat.highlight}</span>
                <ChevronRight size={16} className="track-arrow" />
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* 5. Comparison: CodingFriend vs Traditional Platforms */}
      <section className="comparison-section">
        <div className="section-heading-wrapper">
          <h2 className="features-heading">Why SocraBuddy Socratic Mentorship Wins</h2>
          <p className="features-subheading">
            Built for long-term algorithmic intuition, not short-term memorization.
          </p>
        </div>

        <div className="comparison-table-wrapper">
          <table className="comparison-table">
            <thead>
              <tr>
                <th>Learning Dimension</th>
                <th className="th-competitor">Traditional Platforms</th>
                <th className="th-us">CodingFriend (SocraBuddy)</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="font-bold">When You Get Stuck</td>
                <td className="text-muted">Directly reveals the solution or full code discussion</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Progressive 4-tier SocraBuddy nudges (Nudge $\to$ Concept $\to$ Pattern)
                </td>
              </tr>
              <tr>
                <td className="font-bold">Code Execution Security</td>
                <td className="text-muted">Uncertain sandbox isolation or shared host execution</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Hard container boundaries with 0 network access and 256MB memory cap
                </td>
              </tr>
              <tr>
                <td className="font-bold">Hidden Test Integrity</td>
                <td className="text-muted">Frequently leaked in public payloads</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Strict separation: hidden assertions never leave the server
                </td>
              </tr>
              <tr>
                <td className="font-bold">Language Focus</td>
                <td className="text-muted">Generic polyglot without idiomatic compiler diagnostics</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Java 17/21 first: idiomatic data structures & OpenJDK diagnostics
                </td>
              </tr>
              <tr>
                <td className="font-bold">AI Cost & Privacy</td>
                <td className="text-muted">Locked behind expensive monthly API subscriptions</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  100% self-hostable with local open-weight Ollama models
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      {/* 6. Interactive FAQ Accordion */}
      <section className="faq-section">
        <div className="section-heading-wrapper">
          <h2 className="features-heading">Frequently Asked Questions</h2>
          <p className="features-subheading">
            Everything you need to know about the platform, security, and mentorship.
          </p>
        </div>

        <div className="faq-accordion">
          {faqs.map((faq, idx) => (
            <div
              key={idx}
              className={`faq-item ${activeFaq === idx ? 'open' : ''}`}
              onClick={() => setActiveFaq(activeFaq === idx ? null : idx)}
            >
              <div className="faq-question-row">
                <h3>{faq.q}</h3>
                <span className="faq-toggle-icon">
                  {activeFaq === idx ? '−' : '+'}
                </span>
              </div>
              {activeFaq === idx && (
                <div className="faq-answer-body">
                  <p>{faq.a}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      </section>

      {/* 7. Bottom Call-To-Action Banner */}
      <section className="bottom-cta-card">
        <div className="cta-content">
          <div className="banner-badge">Ready to Level Up?</div>
          <h2>Master Java Problem-Solving Today</h2>
          <p>
            Join learners conquering algorithms with progressive SocraBuddy Socratic hints.
            No credit card, no paid AI API keys required.
          </p>
          <div className="cta-action-row">
            <Link to="/problems" className="btn btn-primary btn-lg">
              <span>Start Solving Challenges</span>
              <ArrowRight size={18} />
            </Link>
            {!isAuthenticated && (
              <Link to="/register" className="btn btn-yellow btn-lg">
                <span>Create Free Account</span>
                <Sparkles size={18} />
              </Link>
            )}
          </div>
        </div>
      </section>
    </div>
  );
};
