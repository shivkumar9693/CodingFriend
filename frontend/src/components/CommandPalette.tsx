import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Code2,
  Layers,
  Trophy,
  Swords,
  BookOpen,
  Sparkles,
  Sun,
  Moon,
  LifeBuoy,
  X,
  ArrowRight,
  Command,
  CornerDownLeft,
} from 'lucide-react';
import { problemService } from '../services/problemService';
import { ProblemSummary } from '../types/problem';
import { useTheme } from '../context/ThemeContext';
import { DifficultyBadge } from './DifficultyBadge';

export interface CommandPaletteProps {
  isOpen: boolean;
  onClose: () => void;
  onOpenSupport?: () => void;
  onOpenMentor?: () => void;
}

interface QuickAction {
  id: string;
  title: string;
  category: 'Navigation' | 'Action' | 'Problem';
  icon: React.ReactNode;
  subtitle?: string;
  badge?: string;
  action: () => void;
}

export const CommandPalette: React.FC<CommandPaletteProps> = ({
  isOpen,
  onClose,
  onOpenSupport,
  onOpenMentor,
}) => {
  const [query, setQuery] = useState('');
  const [problems, setProblems] = useState<ProblemSummary[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  // Load problem catalog in background for instant search
  useEffect(() => {
    if (isOpen && problems.length === 0) {
      problemService
        .getProblems({ page: 0, size: 100 })
        .then((res) => {
          if (res && res.content) {
            setProblems(res.content);
          }
        })
        .catch(() => {});
    }
  }, [isOpen, problems.length]);

  // Focus input and reset on open
  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setSelectedIndex(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  // Static navigation and quick actions
  const staticActions: QuickAction[] = useMemo(
    () => [
      {
        id: 'nav-problems',
        title: 'Explore Problems Catalog',
        category: 'Navigation',
        subtitle: 'Practice curated DSA challenges & test suites',
        icon: <Layers size={16} className="text-blue-400" />,
        action: () => {
          navigate('/problems');
          onClose();
        },
      },
      {
        id: 'nav-notes',
        title: 'Revision Notes & Spaced Repetition',
        category: 'Navigation',
        subtitle: 'AI-generated mnemonics, counter-examples & takeaways',
        badge: 'AI Mnemonic',
        icon: <BookOpen size={16} className="text-yellow-400" />,
        action: () => {
          navigate('/notes');
          onClose();
        },
      },
      {
        id: 'nav-contests',
        title: 'Contest Arena',
        category: 'Navigation',
        subtitle: 'Participate in scheduled rated rounds',
        icon: <Trophy size={16} className="text-amber-400" />,
        action: () => {
          navigate('/contests');
          onClose();
        },
      },
      {
        id: 'nav-duels',
        title: '1v1 Real-Time Duels',
        category: 'Navigation',
        subtitle: 'Head-to-head live coding battles',
        badge: 'Live',
        icon: <Swords size={16} className="text-red-400" />,
        action: () => {
          navigate('/duels');
          onClose();
        },
      },
      {
        id: 'nav-leaderboard',
        title: 'Global Rankings & Leaderboard',
        category: 'Navigation',
        subtitle: 'View top learners & rating percentiles',
        icon: <Trophy size={16} className="text-purple-400" />,
        action: () => {
          navigate('/leaderboard');
          onClose();
        },
      },
      {
        id: 'act-mentor',
        title: 'Ask SocraBuddy Socratic AI',
        category: 'Action',
        subtitle: 'Get progressive Socratic hints without solution spoilers',
        icon: <Sparkles size={16} className="text-yellow-400" />,
        action: () => {
          onClose();
          if (onOpenMentor) onOpenMentor();
        },
      },
      {
        id: 'act-theme',
        title: `Switch to ${theme === 'dark' ? 'Light' : 'Dark'} Mode`,
        category: 'Action',
        subtitle: 'Toggle platform interface color theme',
        icon: theme === 'dark' ? <Sun size={16} className="text-yellow-400" /> : <Moon size={16} className="text-indigo-400" />,
        action: () => {
          toggleTheme();
          onClose();
        },
      },
      {
        id: 'act-support',
        title: 'Help & Bug Reports',
        category: 'Action',
        subtitle: 'Report an issue or reach out to support',
        icon: <LifeBuoy size={16} className="text-emerald-400" />,
        action: () => {
          onClose();
          if (onOpenSupport) onOpenSupport();
        },
      },
    ],
    [navigate, onClose, onOpenMentor, onOpenSupport, theme, toggleTheme]
  );

  // Filtered list combining problem search and actions
  const filteredItems = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return staticActions;

    const matchedActions = staticActions.filter(
      (a) => a.title.toLowerCase().includes(q) || (a.subtitle && a.subtitle.toLowerCase().includes(q))
    );

    const matchedProblems: QuickAction[] = problems
      .filter((p) => {
        return (
          p.title.toLowerCase().includes(q) ||
          p.slug.toLowerCase().includes(q) ||
          (p.tags && p.tags.some((t) => t.toLowerCase().includes(q)))
        );
      })
      .slice(0, 8)
      .map((p) => ({
        id: `prob-${p.id}`,
        title: p.title,
        category: 'Problem' as const,
        subtitle: p.tags ? p.tags.join(' • ') : 'Coding Challenge',
        icon: <Code2 size={16} className="text-accent" />,
        badge: p.difficulty,
        action: () => {
          navigate(`/problems/${p.slug}`);
          onClose();
        },
      }));

    return [...matchedProblems, ...matchedActions];
  }, [query, staticActions, problems, navigate, onClose]);

  // Handle Keyboard Navigation inside palette
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return;

      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev + 1) % Math.max(1, filteredItems.length));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev - 1 + filteredItems.length) % Math.max(1, filteredItems.length));
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (filteredItems[selectedIndex]) {
          filteredItems[selectedIndex].action();
        }
      } else if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, filteredItems, selectedIndex, onClose]);

  // Auto-scroll selected item into view
  useEffect(() => {
    if (listRef.current) {
      const activeEl = listRef.current.querySelector('.command-item.selected');
      if (activeEl) {
        activeEl.scrollIntoView({ block: 'nearest' });
      }
    }
  }, [selectedIndex]);

  if (!isOpen) return null;

  return (
    <div className="command-palette-backdrop" onClick={onClose} role="dialog" aria-modal="true">
      <div className="command-palette-window" onClick={(e) => e.stopPropagation()}>
        {/* Search Header */}
        <div className="command-palette-search-bar">
          <Search size={18} className="command-search-icon" />
          <input
            ref={inputRef}
            type="text"
            className="command-search-input"
            placeholder="Search problems, topics, actions, or pages..."
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setSelectedIndex(0);
            }}
          />
          {query && (
            <button
              type="button"
              className="command-clear-btn"
              onClick={() => setQuery('')}
              title="Clear search"
            >
              <X size={15} />
            </button>
          )}
          <span className="command-shortcut-hint">ESC</span>
        </div>

        {/* Results List */}
        <div className="command-palette-results" ref={listRef}>
          {filteredItems.length === 0 ? (
            <div className="command-empty-state">
              <p>No results found for &ldquo;{query}&rdquo;</p>
              <span>Try searching for problem names, tags (e.g., &ldquo;Tree&rdquo;, &ldquo;DP&rdquo;), or navigation destinations.</span>
            </div>
          ) : (
            filteredItems.map((item, idx) => (
              <div
                key={item.id}
                className={`command-item ${selectedIndex === idx ? 'selected' : ''}`}
                onClick={item.action}
                onMouseEnter={() => setSelectedIndex(idx)}
              >
                <div className="command-item-icon-wrap">{item.icon}</div>

                <div className="command-item-content">
                  <div className="command-item-title-row">
                    <span className="command-item-title">{item.title}</span>
                    {item.badge && (
                      item.category === 'Problem' ? (
                        <DifficultyBadge difficulty={item.badge as any} />
                      ) : (
                        <span className="command-item-badge">{item.badge}</span>
                      )
                    )}
                  </div>
                  {item.subtitle && <span className="command-item-subtitle">{item.subtitle}</span>}
                </div>

                <div className="command-item-action-hint">
                  {selectedIndex === idx ? (
                    <CornerDownLeft size={13} className="text-accent" />
                  ) : (
                    <ArrowRight size={13} className="text-muted" />
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        {/* Palette Footer */}
        <div className="command-palette-footer">
          <div className="palette-footer-shortcuts">
            <span className="shortcut-tag">
              <Command size={11} /> <span>↑↓ to navigate</span>
            </span>
            <span className="shortcut-tag">
              <CornerDownLeft size={11} /> <span>to select</span>
            </span>
            <span className="shortcut-tag">
              <span>ESC to close</span>
            </span>
          </div>
          <span className="palette-footer-brand">CodingFriend Spotlight</span>
        </div>
      </div>
    </div>
  );
};
