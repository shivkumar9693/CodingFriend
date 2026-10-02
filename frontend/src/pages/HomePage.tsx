import React, { useState, useEffect } from 'react';
import { Link, Navigate } from 'react-router-dom';
import {
  Sparkles,
  Shield,
  ArrowRight,
  Layers,
  Code2,
  Zap,
  ChevronRight,
  Check,
  Flame,
  BrainCircuit,
  Lock,
  Swords,
  Trophy,
  Server,
  Play,
  Pause,
  Target,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface LanguageSnippet {
  id: string;
  name: string;
  extension: string;
  badge: string;
  filename: string;
  code: Array<{ ln: number; content: React.ReactNode; isError?: boolean; isHighlight?: boolean; note?: string }>;
}

type SimStage = 'scanning' | 'pinpoint' | 'reasoning' | 'hint';

export const HomePage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  const [activeHintLevel, setActiveHintLevel] = useState<number>(1);
  const [activeLanguage, setActiveLanguage] = useState<string>('java');
  const [activeFaq, setActiveFaq] = useState<number | null>(0);
  const [simStage, setSimStage] = useState<SimStage>('scanning');
  const [isAutoPlaying, setIsAutoPlaying] = useState<boolean>(true);

  // Automatic AI Socratic Discovery Demonstration Loop
  useEffect(() => {
    if (!isAutoPlaying) return;

    const timer1 = setTimeout(() => {
      setSimStage('pinpoint');
    }, 1800);

    const timer2 = setTimeout(() => {
      setSimStage('reasoning');
    }, 3200);

    const timer3 = setTimeout(() => {
      setSimStage('hint');
    }, 4500);

    const timer4 = setTimeout(() => {
      setActiveHintLevel((prev) => (prev % 4) + 1);
      setSimStage('scanning');
    }, 8500);

    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
      clearTimeout(timer3);
      clearTimeout(timer4);
    };
  }, [simStage, isAutoPlaying]);

  const triggerDiagnosticScan = (level?: number, lang?: string) => {
    if (level !== undefined) setActiveHintLevel(level);
    if (lang !== undefined) setActiveLanguage(lang);
    setSimStage('scanning');
  };

  if (isAuthenticated) {
    return <Navigate to={user?.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />;
  }

  const languages: Record<string, LanguageSnippet> = {
    java: {
      id: 'java',
      name: 'Java 17/21',
      extension: '.java',
      badge: 'OpenJDK 17',
      filename: 'Solution.java (Java 17 OpenJDK)',
      code: [
        { ln: 1, content: <span><span className="kw">class</span> <span className="cls">Solution</span> &#123;</span> },
        { ln: 2, content: <span className="pl-4"><span className="kw">public int</span>[] <span className="fn">twoSum</span>(<span className="kw">int</span>[] nums, <span className="kw">int</span> target) &#123;</span> },
        { ln: 3, content: <span className="pl-8">Map&lt;Integer, Integer&gt; map = <span className="kw">new</span> HashMap&lt;&gt;();</span> },
        { ln: 4, content: <span className="pl-8"><span className="kw">for</span> (<span className="kw">int</span> i = 0; i &lt; nums.length; i++) &#123;</span> },
        { ln: 5, isHighlight: true, content: <span className="pl-12"><span className="kw">int</span> complement = target - nums[i];</span> },
        { ln: 6, isError: true, note: '▲ Diagnostic Check', content: <span className="pl-12"><span className="kw">if</span> (map.containsKey(complement)) &#123;</span> },
        { ln: 7, content: <span className="pl-16"><span className="kw">return new int</span>[] &#123; map.get(complement), i &#125;;</span> },
        { ln: 8, content: <span className="pl-12">&#125;</span> },
        { ln: 9, content: <span className="pl-12">map.put(nums[i], i);</span> },
        { ln: 10, content: <span className="pl-8">&#125;</span> },
        { ln: 11, content: <span className="pl-8"><span className="kw">return new int</span>[]&#123;&#125;;</span> },
        { ln: 12, content: <span className="pl-4">&#125;</span> },
        { ln: 13, content: <span>&#125;</span> },
      ],
    },
    python: {
      id: 'python',
      name: 'Python 3.12',
      extension: '.py',
      badge: 'CPython 3.12',
      filename: 'solution.py (Python 3.12)',
      code: [
        { ln: 1, content: <span><span className="kw">class</span> <span className="cls">Solution</span>:</span> },
        { ln: 2, content: <span className="pl-4"><span className="kw">def</span> <span className="fn">twoSum</span>(self, nums: list[int], target: int) -&gt; list[int]:</span> },
        { ln: 3, content: <span className="pl-8">seen = &#123;&#125;</span> },
        { ln: 4, content: <span className="pl-8"><span className="kw">for</span> i, num <span className="kw">in</span> enumerate(nums):</span> },
        { ln: 5, isHighlight: true, content: <span className="pl-12">complement = target - num</span> },
        { ln: 6, isError: true, note: '▲ Diagnostic Check', content: <span className="pl-12"><span className="kw">if</span> complement <span className="kw">in</span> seen:</span> },
        { ln: 7, content: <span className="pl-16"><span className="kw">return</span> [seen[complement], i]</span> },
        { ln: 8, content: <span className="pl-12">seen[num] = i</span> },
        { ln: 9, content: <span className="pl-8"><span className="kw">return</span> []</span> },
      ],
    },
    cpp: {
      id: 'cpp',
      name: 'C++ 20',
      extension: '.cpp',
      badge: 'GCC 13 / C++20',
      filename: 'solution.cpp (C++20 STL)',
      code: [
        { ln: 1, content: <span>#<span className="kw">include</span> &lt;unordered_map&gt;</span> },
        { ln: 2, content: <span>#<span className="kw">include</span> &lt;vector&gt;</span> },
        { ln: 3, content: <span><span className="kw">class</span> <span className="cls">Solution</span> &#123;</span> },
        { ln: 4, content: <span><span className="kw">public</span>:</span> },
        { ln: 5, content: <span className="pl-4">std::vector&lt;<span className="kw">int</span>&gt; <span className="fn">twoSum</span>(std::vector&lt;<span className="kw">int</span>&gt;&amp; nums, <span className="kw">int</span> target) &#123;</span> },
        { ln: 6, content: <span className="pl-8">std::unordered_map&lt;<span className="kw">int</span>, <span className="kw">int</span>&gt; numMap;</span> },
        { ln: 7, content: <span className="pl-8"><span className="kw">for</span> (<span className="kw">int</span> i = 0; i &lt; nums.size(); i++) &#123;</span> },
        { ln: 8, isHighlight: true, content: <span className="pl-12"><span className="kw">int</span> comp = target - nums[i];</span> },
        { ln: 9, isError: true, note: '▲ Diagnostic Check', content: <span className="pl-12"><span className="kw">if</span> (numMap.find(comp) != numMap.end())</span> },
        { ln: 10, content: <span className="pl-16"><span className="kw">return</span> &#123; numMap[comp], i &#125;;</span> },
        { ln: 11, content: <span className="pl-12">numMap[nums[i]] = i;</span> },
        { ln: 12, content: <span className="pl-8">&#125;</span> },
        { ln: 13, content: <span className="pl-8"><span className="kw">return</span> &#123;&#125;;</span> },
        { ln: 14, content: <span className="pl-4">&#125;</span> },
        { ln: 15, content: <span>&#125;;</span> },
      ],
    },
    typescript: {
      id: 'typescript',
      name: 'TypeScript',
      extension: '.ts',
      badge: 'TypeScript 5.x',
      filename: 'solution.ts (TypeScript 5.x / Node 20)',
      code: [
        { ln: 1, content: <span><span className="kw">function</span> <span className="fn">twoSum</span>(nums: <span className="kw">number</span>[], target: <span className="kw">number</span>): <span className="kw">number</span>[] &#123;</span> },
        { ln: 2, content: <span className="pl-4"><span className="kw">const</span> map = <span className="kw">new</span> Map&lt;<span className="kw">number</span>, <span className="kw">number</span>&gt;();</span> },
        { ln: 3, content: <span className="pl-4"><span className="kw">for</span> (<span className="kw">let</span> i = 0; i &lt; nums.length; i++) &#123;</span> },
        { ln: 4, isHighlight: true, content: <span className="pl-8"><span className="kw">const</span> comp = target - nums[i];</span> },
        { ln: 5, isError: true, note: '▲ Diagnostic Check', content: <span className="pl-8"><span className="kw">if</span> (map.has(comp)) &#123;</span> },
        { ln: 6, content: <span className="pl-12"><span className="kw">return</span> [map.get(comp)!, i];</span> },
        { ln: 7, content: <span className="pl-8">&#125;</span> },
        { ln: 8, content: <span className="pl-8">map.set(nums[i], i);</span> },
        { ln: 9, content: <span className="pl-4">&#125;</span> },
        { ln: 10, content: <span className="pl-4"><span className="kw">return</span> [];</span> },
        { ln: 11, content: <span>&#125;</span> },
      ],
    },
    go: {
      id: 'go',
      name: 'Go 1.22',
      extension: '.go',
      badge: 'Go 1.22 Runtime',
      filename: 'solution.go (Go 1.22)',
      code: [
        { ln: 1, content: <span><span className="kw">package</span> main</span> },
        { ln: 2, content: <span><span className="kw">func</span> <span className="fn">twoSum</span>(nums []<span className="kw">int</span>, target <span className="kw">int</span>) []<span className="kw">int</span> &#123;</span> },
        { ln: 3, content: <span className="pl-4">m := <span className="kw">make</span>(<span className="kw">map</span>[<span className="kw">int</span>]<span className="kw">int</span>)</span> },
        { ln: 4, content: <span className="pl-4"><span className="kw">for</span> i, num := <span className="kw">range</span> nums &#123;</span> },
        { ln: 5, isHighlight: true, content: <span className="pl-8">comp := target - num</span> },
        { ln: 6, isError: true, note: '▲ Diagnostic Check', content: <span className="pl-8"><span className="kw">if</span> idx, ok := m[comp]; ok &#123;</span> },
        { ln: 7, content: <span className="pl-12"><span className="kw">return</span> []<span className="kw">int</span>&#123;idx, i&#125;</span> },
        { ln: 8, content: <span className="pl-8">&#125;</span> },
        { ln: 9, content: <span className="pl-8">m[num] = i</span> },
        { ln: 10, content: <span className="pl-4">&#125;</span> },
        { ln: 11, content: <span className="pl-4"><span className="kw">return</span> <span className="kw">nil</span></span> },
        { ln: 12, content: <span>&#125;</span> },
      ],
    },
  };

  const hintDemos = [
    {
      level: 1,
      title: 'Level 1: Gentle Nudge',
      tag: 'OBSERVATIONAL',
      aiResponse: 'Take a close look at your hash map lookup. What happens when the complement (target - current_element) matches an element at the identical index?',
      benefit: 'Guides your attention to the edge condition without revealing any logic.',
    },
    {
      level: 2,
      title: 'Level 2: Conceptual Insight',
      tag: 'ALGORITHMIC INVARIANT',
      aiResponse: 'Remember each array element may only be used once. If your map stores indices as you iterate, ensure you do not pair an index with itself before recording.',
      benefit: 'Identifies the structural programming invariant and boundary rules.',
    },
    {
      level: 3,
      title: 'Level 3: Algorithmic Pattern',
      tag: 'DESIGN PATTERN',
      aiResponse: 'Apply the One-Pass Hash Table pattern: as you visit index i, query the hash map for complement. If found, immediately return [map[complement], i]; otherwise insert the pair.',
      benefit: 'Provides the optimal algorithmic pattern without writing copy-paste code.',
    },
    {
      level: 4,
      title: 'Level 4: Targeted Pointer',
      tag: 'SYNTAX & LOGIC POINTER',
      aiResponse: 'Refactor your loop check to verify key existence before inserting into your map, ensuring O(N) single-pass runtime and zero duplicate index pairing.',
      benefit: 'Pinpoints the exact construct fix while keeping your hands actively coding.',
    },
  ];

  const problemCategories = [
    {
      title: 'Arrays & Two Pointers',
      count: '4 Challenges',
      desc: 'Master two-pointer traversals, index mapping, and sliding window boundaries.',
      tags: ['ARRAY', 'TWO_POINTERS', 'HASH_TABLE'],
      highlight: 'Two Sum, Valid Palindrome',
      slug: 'two-sum',
    },
    {
      title: 'Dynamic Programming',
      count: '2 Challenges',
      desc: "Develop Kadane's algorithm, state memoization, and optimal subproblems.",
      tags: ['DYNAMIC_PROGRAMMING', 'MEMOIZATION'],
      highlight: 'Maximum Subarray, Stock Buying',
      slug: 'maximum-subarray',
    },
    {
      title: 'Hash Tables & Sets',
      count: '3 Challenges',
      desc: 'O(1) average lookups, frequency hashing, and duplicate detection pipelines.',
      tags: ['HASH_SET', 'SORTING', 'STRING'],
      highlight: 'Contains Duplicate, Valid Anagram',
      slug: 'contains-duplicate',
    },
    {
      title: 'Strings & Simulation',
      count: '2 Challenges',
      desc: 'In-place character manipulation, palindrome verification, and simulation logic.',
      tags: ['STRING', 'SIMULATION', 'MATH'],
      highlight: 'Reverse String, Fizz Buzz',
      slug: 'reverse-string',
    },
  ];

  const faqs = [
    {
      q: 'How does SocraBuddy Socratic Hinting differ from traditional AI code generators?',
      a: 'Standard AI tools give away full solution code immediately, which harms long-term memory retention and interview readiness. SocraBuddy Socratic Mentor diagnoses your exact mistake (syntax, logic, runtime, or timeout) and provides progressive nudges (Nudge → Concept → Pattern → Pointer) to guide your intuition without writing the code for you.',
    },
    {
      q: 'Which programming languages are supported?',
      a: 'The platform is engineered for modern multi-language competitive programming and algorithm mastery, with first-class support for Java (17/21 OpenJDK), Python (3.12), C++ (GCC 20 STL), TypeScript/JavaScript, and Go (1.22).',
    },
    {
      q: 'How does the Docker Execution Sandbox guarantee isolation and security?',
      a: 'Untrusted user code is executed in dedicated, ephemeral container sandboxes with strictly enforced resource limits: 256MB RAM memory cap, single-core CPU quota, 5-second wall-clock timeout, and completely disabled network access (--network none). Learner code never runs directly on the core host JVM.',
    },
    {
      q: 'Are hidden test cases protected from inspection?',
      a: 'Yes. The backend enforces strict cryptographic and architectural isolation between public sample test cases (returned to the client for local validation) and hidden grading test suites. Hidden inputs and expected assertions are never transmitted through public APIs.',
    },
    {
      q: 'Is CodingFriend free for learners and students?',
      a: 'Yes! CodingFriend is completely free for learners. You get full access to the curated multi-language problem catalog, interactive Monaco IDE, real-time 1v1 duels, weekly competitive contests, and unlimited SocraBuddy Socratic AI coaching without any paywalls or subscriptions.',
    },
    {
      q: 'How do 1v1 Duels and Competitive Contests work?',
      a: 'Learners can join live timed coding contests or invite rivals to real-time 1v1 Duel Arenas. Live submissions, test case pass counts, anti-cheat tab-switch detection, and automatic Elo rating calibrations (Bronze → Silver → Gold → Knight → Grandmaster) are computed instantly.',
    },
  ];

  const currentLang = languages[activeLanguage] || languages.java;

  return (
    <div className="home-page-root">
      {/* 1. Hero Section with Multi-Language Socratic Simulator */}
      <section className="hero-section">
        <div className="hero-badge">
          <Flame size={14} className="text-red" />
          <span>Next-Gen Multi-Language Coding Platform</span>
          <span className="badge-sep">•</span>
          <span className="text-yellow">SocraBuddy AI Mentor</span>
        </div>

        <h1 className="hero-heading">
          Stop Copy-Pasting Solutions. <br />
          <span className="text-gradient">Master Modern Problem Solving.</span>
        </h1>

        <p className="hero-lead">
          Solve algorithmic challenges across Java, Python, C++, TypeScript, and Go.
          When you get stuck, SocraBuddy provides progressive Socratic hints — training your intuition without spoiling the answer.
        </p>

        <div className="hero-action-buttons">
          <Link to="/problems" className="btn btn-primary btn-lg glow-on-hover">
            <span>Explore Challenges</span>
            <ArrowRight size={18} />
          </Link>
          <Link to="/duels" className="btn btn-secondary btn-lg">
            <Swords size={18} className="text-teal-400" />
            <span>1v1 Duel Arena</span>
          </Link>
          <Link to="/contests" className="btn btn-yellow btn-lg">
            <Trophy size={18} />
            <span>Competitive Contests</span>
          </Link>
        </div>

        {/* Live Interactive Hero IDE / Multi-Language Simulator */}
        <div className="hero-ide-preview">
          {/* Live Diagnostic Pipeline Step Tracker */}
          <div className="diagnostic-pipeline-tracker">
            <div className={`pipeline-step ${simStage === 'scanning' ? 'step-active' : 'step-completed'}`}>
              <span className="step-num">1</span>
              <span className="step-label">AST Scan</span>
            </div>
            <div className="pipeline-arrow">➔</div>
            <div className={`pipeline-step ${simStage === 'pinpoint' ? 'step-active' : (simStage === 'reasoning' || simStage === 'hint' ? 'step-completed' : '')}`}>
              <span className="step-num">2</span>
              <span className="step-label">Flaw Pinpoint</span>
            </div>
            <div className="pipeline-arrow">➔</div>
            <div className={`pipeline-step ${simStage === 'reasoning' ? 'step-active' : (simStage === 'hint' ? 'step-completed' : '')}`}>
              <span className="step-num">3</span>
              <span className="step-label">Logic Classify</span>
            </div>
            <div className="pipeline-arrow">➔</div>
            <div className={`pipeline-step ${simStage === 'hint' ? 'step-active' : ''}`}>
              <span className="step-num">4</span>
              <span className="step-label">Socratic Hint</span>
            </div>
          </div>

          {/* Language Switcher Bar */}
          <div className="ide-language-tabs-bar">
            <div className="ide-lang-label">
              <Code2 size={15} />
              <span>Select Language:</span>
            </div>
            <div className="ide-lang-pills">
              {Object.values(languages).map((lang) => (
                <button
                  key={lang.id}
                  type="button"
                  className={`ide-lang-pill ${activeLanguage === lang.id ? 'active' : ''}`}
                  onClick={() => triggerDiagnosticScan(undefined, lang.id)}
                >
                  <span className="lang-dot" />
                  <span>{lang.name}</span>
                </button>
              ))}
            </div>
            <div className="ide-action-controls-row">
              <button
                type="button"
                className={`ide-scan-ast-btn ${simStage === 'scanning' ? 'scanning' : ''}`}
                onClick={() => triggerDiagnosticScan()}
                title="Trigger automated AST Scan & Socratic diagnostic"
              >
                <Zap size={13} className={simStage === 'scanning' ? 'animate-bounce text-yellow' : 'text-yellow'} />
                <span>Scan AST</span>
              </button>
              <button
                type="button"
                className={`ide-autoplay-btn ${isAutoPlaying ? 'playing' : ''}`}
                onClick={() => setIsAutoPlaying(!isAutoPlaying)}
                title={isAutoPlaying ? 'Pause automatic animation' : 'Resume automatic animation'}
              >
                {isAutoPlaying ? <Pause size={12} /> : <Play size={12} />}
                <span>{isAutoPlaying ? 'Auto-Cycle' : 'Paused'}</span>
              </button>
            </div>
          </div>

          <div className="ide-window-header">
            <div className="window-dots">
              <span className="dot dot-red"></span>
              <span className="dot dot-yellow"></span>
              <span className="dot dot-green"></span>
            </div>
            <div className="window-filename">
              <Code2 size={14} className="text-yellow" />
              <span>{currentLang.filename}</span>
            </div>
            <div className={`window-pill socra-diagnostic-pill ${simStage === 'scanning' ? 'pill-scanning' : simStage === 'pinpoint' ? 'pill-pinpoint' : ''}`}>
              <span className="live-radar-dot" />
              <span className="font-bold">SocraBuddy Diagnostic Live</span>
              <span className="pill-subtext">
                {simStage === 'scanning' && '• Scanning AST...'}
                {simStage === 'pinpoint' && '• Flaw Pinpointed'}
                {simStage === 'reasoning' && '• Classifying Mistake...'}
                {simStage === 'hint' && '• Socratic Nudge Active'}
              </span>
            </div>
          </div>

          <div className="ide-window-body">
            <div className="ide-code-panel font-mono relative">
              {simStage === 'scanning' && (
                <div className="laser-scanner-beam-container">
                  <div className="laser-scanner-beam" />
                  <div className="laser-scanner-tag">🔍 Parsing Abstract Syntax Tree...</div>
                </div>
              )}
              {currentLang.code.map((line) => {
                const isTargetLine = line.isError;
                const isPinpointingNow = isTargetLine && (simStage === 'pinpoint' || simStage === 'reasoning' || simStage === 'hint');
                return (
                  <div
                    key={line.ln}
                    className={`code-line ${line.isHighlight ? 'code-line-highlight' : ''} ${
                      line.isError ? 'code-line-error' : ''
                    } ${isPinpointingNow ? 'code-line-pinpointed' : ''}`}
                  >
                    <span className="ln">{line.ln}</span>
                    {line.content}
                    {line.isError && (
                      <span className={`diagnostic-check-tag ${isPinpointingNow ? 'active-flaw-tag' : ''}`}>
                        ▲ Diagnostic Check
                      </span>
                    )}
                    {isPinpointingNow && (
                      <span className="pinpoint-target-chip">
                        <Target size={12} className="text-red animate-spin-slow" />
                        <span>Flaw: Potential index self-pairing collision</span>
                      </span>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Socratic Mentor Diagnostic Box */}
            <div className="ide-mentor-panel">
              <div className="mentor-panel-header">
                <div className="mentor-avatar">
                  <BrainCircuit size={18} className={simStage === 'reasoning' ? 'animate-pulse text-yellow' : ''} />
                </div>
                <div className="mentor-header-info">
                  <h4 className="mentor-name">SocraBuddy Socratic Mentor</h4>
                  <div className="mentor-status-row">
                    <span className="mentor-status">
                      ● Live Mistake Classification
                    </span>
                    <span className={`mistake-type-pill ${simStage === 'scanning' ? 'analyzing' : 'identified'}`}>
                      {simStage === 'scanning' && 'Scanning code tree...'}
                      {simStage === 'pinpoint' && 'Logic & Invariant Mismatch'}
                      {simStage === 'reasoning' && 'Formulating Socratic Nudge...'}
                      {simStage === 'hint' && 'Logic Invariant (Edge Case)'}
                    </span>
                    {/* Animated Soundwave / Waveform visualizer */}
                    <div className="soundwave-bars">
                      <span className={`sw-bar bar-1 ${simStage !== 'scanning' ? 'active-wave' : ''}`} />
                      <span className={`sw-bar bar-2 ${simStage !== 'scanning' ? 'active-wave' : ''}`} />
                      <span className={`sw-bar bar-3 ${simStage !== 'scanning' ? 'active-wave' : ''}`} />
                      <span className={`sw-bar bar-4 ${simStage !== 'scanning' ? 'active-wave' : ''}`} />
                    </div>
                  </div>
                </div>
              </div>

              {/* Hint Level Selector */}
              <div className="hint-tier-selector">
                {[1, 2, 3, 4].map((lvl) => (
                  <button
                    key={lvl}
                    type="button"
                    className={`hint-tier-btn ${activeHintLevel === lvl ? 'active' : ''}`}
                    onClick={() => triggerDiagnosticScan(lvl)}
                  >
                    Level {lvl}
                  </button>
                ))}
              </div>

              <div className={`mentor-dialogue-card ${simStage === 'hint' ? 'card-hint-revealed' : simStage === 'reasoning' ? 'card-reasoning' : ''}`}>
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
                  <Sparkles size={13} className="text-yellow animate-pulse" />
                  <span>{hintDemos[activeHintLevel - 1].benefit}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Platform Telemetry Metrics Strip (Direct from README) */}
      <section className="telemetry-strip">
        <div className="telemetry-item">
          <span className="telemetry-number text-yellow">4-Tier</span>
          <span className="telemetry-label">Progressive Socratic Nudges</span>
        </div>
        <div className="telemetry-divider"></div>
        <div className="telemetry-item">
          <span className="telemetry-number text-red">5+ Runtimes</span>
          <span className="telemetry-label">Java, Python, C++, TS, Go</span>
        </div>
        <div className="telemetry-divider"></div>
        <div className="telemetry-item">
          <span className="telemetry-number text-yellow">100% Isolated</span>
          <span className="telemetry-label">Safe Code Runner</span>
        </div>
        <div className="telemetry-divider"></div>
        <div className="telemetry-item">
          <span className="telemetry-number text-red">100% Free</span>
          <span className="telemetry-label">Unlimited Socratic Hints</span>
        </div>
      </section>

      {/* 3. Core Architecture & Key Pillars */}
      <section className="pillars-section">
        <div className="section-heading-wrapper">
          <div className="badge-outline">Core Platform Capabilities</div>
          <h2 className="features-heading">Engineered for Deep Algorithmic Mastery</h2>
          <p className="features-subheading">
            A comprehensive platform combining step-by-step guidance, real-time battles, and instant compiler diagnostics.
          </p>
        </div>

        <div className="pillars-grid">
          <div className="pillar-card">
            <div className="pillar-icon-box bg-red">
              <BrainCircuit size={24} />
            </div>
            <h3>Socratic AI Mentor</h3>
            <p>
              Diagnoses syntactic, logical, algorithmic, and runtime mistakes without spoiling answers.
              Progresses through 4 tiers: Nudge → Concept → Pattern → Targeted Pointer.
            </p>
            <div className="pillar-tags">
              <span>Interactive Nudges</span>
              <span>Long-Term Retention</span>
              <span>Zero Spoilers</span>
            </div>
          </div>

          <div className="pillar-card">
            <div className="pillar-icon-box bg-yellow">
              <Shield size={24} />
            </div>
            <h3>Isolated Code Sandbox</h3>
            <p>
              Your code runs in ultra-fast, isolated execution sandboxes with instant compile feedback,
              test case assertions, and memory diagnostics.
            </p>
            <div className="pillar-tags">
              <span>Ultra-Fast Runner</span>
              <span>Instant Test Checks</span>
              <span>Safe Execution</span>
            </div>
          </div>

          <div className="pillar-card">
            <div className="pillar-icon-box bg-teal">
              <Swords size={24} />
            </div>
            <h3>1v1 Duels & Contests</h3>
            <p>
              Challenge friends or rivals to live head-to-head coding battles or participate in weekly
              scheduled competitive rounds with live leaderboard updates.
            </p>
            <div className="pillar-tags">
              <span>Live Duel Arena</span>
              <span>Elo Calibration</span>
              <span>Live Rankings</span>
            </div>
          </div>

          <div className="pillar-card">
            <div className="pillar-icon-box bg-blue">
              <Lock size={24} />
            </div>
            <h3>Hidden Test Suite Protection</h3>
            <p>
              Public sample cases help you debug locally, while rigorous hidden evaluation suites
              and boundary edge cases test your solution thoroughly.
            </p>
            <div className="pillar-tags">
              <span>Edge Case Checks</span>
              <span>Public Samples</span>
              <span>Fair Grading</span>
            </div>
          </div>

          <div className="pillar-card">
            <div className="pillar-icon-box bg-purple">
              <Server size={24} />
            </div>
            <h3>Comprehensive Learning Ledger</h3>
            <p>
              Track your daily consistency streaks, problem-solving accuracy breakdowns,
              365-day submission heatmaps, and competitive rating progression.
            </p>
            <div className="pillar-tags">
              <span>365-Day Heatmap</span>
              <span>Streak Badges</span>
              <span>Personal Stats</span>
            </div>
          </div>

          <div className="pillar-card">
            <div className="pillar-icon-box bg-orange">
              <Code2 size={24} />
            </div>
            <h3>Monaco Interactive IDE</h3>
            <p>
              Professional Monaco code editor with multi-language syntax highlighting, local autosave,
              dark/light themes, keyboard shortcuts, and instant compiler diagnostics.
            </p>
            <div className="pillar-tags">
              <span>Monaco Editor</span>
              <span>Instant Autosave</span>
              <span>Syntax Colors</span>
            </div>
          </div>
        </div>
      </section>

      {/* 4. Multi-Language Support Hub */}
      <section className="languages-showcase-section">
        <div className="section-heading-wrapper">
          <div className="badge-outline">Supported Programming Languages</div>
          <h2 className="features-heading">Code in Your Language of Choice</h2>
          <p className="features-subheading">
            Compilers and runtime harnesses tuned for optimal algorithmic problem solving and interview prep.
          </p>
        </div>

        <div className="languages-grid">
          <div className="language-badge-card">
            <div className="lang-icon-wrap lang-java">
              <span className="lang-code-tag">☕</span>
            </div>
            <h4>Java 17 / 21 LTS</h4>
            <p>Standard Collections Framework, Streams API, and OpenJDK compiler diagnostic pipelines.</p>
            <span className="lang-version-pill">OpenJDK 17/21</span>
          </div>

          <div className="language-badge-card">
            <div className="lang-icon-wrap lang-python">
              <span className="lang-code-tag">🐍</span>
            </div>
            <h4>Python 3.12</h4>
            <p>Fast algorithmic prototyping with list comprehensions, heaps, and bisect modules.</p>
            <span className="lang-version-pill">CPython 3.12</span>
          </div>

          <div className="language-badge-card">
            <div className="lang-icon-wrap lang-cpp">
              <span className="lang-code-tag">⚡</span>
            </div>
            <h4>C++ 20 (STL)</h4>
            <p>High-performance competitive programming with standard template library containers.</p>
            <span className="lang-version-pill">GCC 13 / C++20</span>
          </div>

          <div className="language-badge-card">
            <div className="lang-icon-wrap lang-ts">
              <span className="lang-code-tag">🔷</span>
            </div>
            <h4>TypeScript & JS</h4>
            <p>Modern Node.js runtime, asynchronous problem solving, and strictly typed structures.</p>
            <span className="lang-version-pill">Node 20 / TS 5</span>
          </div>

          <div className="language-badge-card">
            <div className="lang-icon-wrap lang-go">
              <span className="lang-code-tag">🐹</span>
            </div>
            <h4>Go 1.22</h4>
            <p>Fast compilation, lightweight concurrency routines, and clean data structures.</p>
            <span className="lang-version-pill">Go 1.22</span>
          </div>
        </div>
      </section>

      {/* 5. Curated Challenge Tracks */}
      <section className="tracks-section">
        <div className="section-heading-wrapper">
          <div className="badge-outline">Curated Challenge Tracks</div>
          <h2 className="features-heading">Targeted Algorithm & System Prep</h2>
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

      {/* 6. Comparison: CodingFriend vs Traditional Platforms */}
      <section className="comparison-section">
        <div className="section-heading-wrapper">
          <div className="badge-outline">Platform Comparison</div>
          <h2 className="features-heading">Why SocraBuddy Socratic Mentorship Wins</h2>
          <p className="features-subheading">
            Built for long-term algorithmic intuition, not short-term solution memorization.
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
                <td className="text-muted">Directly reveals full solution code or discussion spoilers</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Progressive 4-tier Socratic nudges (Nudge → Concept → Pattern → Logic Pointer)
                </td>
              </tr>
              <tr>
                <td className="font-bold">Code Execution Security</td>
                <td className="text-muted">Uncertain sandbox isolation or shared host execution</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Hard container boundaries with 0 network access, 256MB memory cap, and timeout
                </td>
              </tr>
              <tr>
                <td className="font-bold">Hidden Test Privacy</td>
                <td className="text-muted">Frequently leaked in public JSON API payloads</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Strict server separation: hidden grading test cases never leave the backend
                </td>
              </tr>
              <tr>
                <td className="font-bold">Multi-Language Runtimes</td>
                <td className="text-muted">Generic runners without idiomatic error diagnostics</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Tuned harnesses for Java, Python, C++, TypeScript, and Go
                </td>
              </tr>
              <tr>
                <td className="font-bold">Socratic AI Mentorship</td>
                <td className="text-muted">Generic code dump that reveals full answers without learning</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Progressive 4-tier interactive hints tailored to your exact mistake
                </td>
              </tr>
              <tr>
                <td className="font-bold">Competitive Arena</td>
                <td className="text-muted">Static problem lists with no live head-to-head battles</td>
                <td className="text-highlight">
                  <Check size={16} className="text-yellow mr-1" />
                  Real-time 1v1 Duels, room invites, anti-cheat tracking, and Elo rating ladders
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      {/* 7. Interactive FAQ Accordion */}
      <section className="faq-section">
        <div className="section-heading-wrapper">
          <div className="badge-outline">Frequently Asked Questions</div>
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

      {/* 8. Bottom Call-To-Action Banner */}
      <section className="bottom-cta-card">
        <div className="cta-content">
          <div className="banner-badge">Ready to Level Up?</div>
          <h2>Master Multi-Language Problem Solving Today</h2>
          <p>
            Join thousands of learners building algorithmic intuition with SocraBuddy.
            Start solving curated challenges or enter the 1v1 Duel Arena right now.
          </p>
          <div className="cta-action-row">
            <Link to="/problems" className="btn btn-primary btn-lg">
              <span>Start Solving Challenges</span>
              <ArrowRight size={18} />
            </Link>
            <Link to="/duels" className="btn btn-secondary btn-lg">
              <Swords size={18} />
              <span>Enter 1v1 Duels</span>
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
