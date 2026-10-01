import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Search, Filter, ChevronLeft, ChevronRight, BookOpen, AlertCircle, RefreshCw, Tag as TagIcon } from 'lucide-react';
import { problemService } from '../services/problemService';
import { PageResponse, ProblemDifficulty, ProblemSummary } from '../types/problem';
import { DifficultyBadge } from '../components/DifficultyBadge';
import { LoadingSpinner } from '../components/LoadingSpinner';

const COMMON_TAGS = ['ARRAY', 'STRING', 'HASH_TABLE', 'TWO_POINTERS', 'DYNAMIC_PROGRAMMING', 'MATH', 'SORTING'];

export const ProblemsPage: React.FC = () => {
  const [problemsPage, setProblemsPage] = useState<PageResponse<ProblemSummary> | null>(null);
  const [selectedDifficulty, setSelectedDifficulty] = useState<ProblemDifficulty | undefined>(undefined);
  const [selectedTag, setSelectedTag] = useState<string | undefined>(undefined);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProblems = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await problemService.getProblems({
        difficulty: selectedDifficulty,
        tag: selectedTag,
        page: currentPage,
        size: pageSize,
      });
      setProblemsPage(response);
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : 'Failed to load problems. Please ensure the backend is running.'
      );
    } finally {
      setIsLoading(false);
    }
  }, [selectedDifficulty, selectedTag, currentPage, pageSize]);

  useEffect(() => {
    fetchProblems();
  }, [fetchProblems]);

  const handleDifficultyChange = (diff?: ProblemDifficulty) => {
    setSelectedDifficulty(diff);
    setCurrentPage(0);
  };

  const handleTagChange = (tag?: string) => {
    setSelectedTag(tag);
    setCurrentPage(0);
  };

  const filteredProblems = (problemsPage?.content || []).filter((p) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      p.title.toLowerCase().includes(q) ||
      p.slug.toLowerCase().includes(q) ||
      p.tags.some((t) => t.toLowerCase().includes(q))
    );
  });

  return (
    <div className="page-container problems-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Java Coding Challenges</h1>
          <p className="page-subtitle">
            Sharpen your algorithmic thinking with interactive challenges and SocraBuddy AI guidance.
          </p>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="filter-panel">
        <div className="search-box">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            className="search-input"
            placeholder="Search problems by name or tag..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div className="filter-group">
          <span className="filter-label">
            <Filter size={14} /> Difficulty:
          </span>
          <div className="pill-group">
            <button
              className={`pill-btn ${selectedDifficulty === undefined ? 'active' : ''}`}
              onClick={() => handleDifficultyChange(undefined)}
            >
              All
            </button>
            <button
              className={`pill-btn pill-easy ${selectedDifficulty === 'EASY' ? 'active' : ''}`}
              onClick={() => handleDifficultyChange('EASY')}
            >
              Easy
            </button>
            <button
              className={`pill-btn pill-medium ${selectedDifficulty === 'MEDIUM' ? 'active' : ''}`}
              onClick={() => handleDifficultyChange('MEDIUM')}
            >
              Medium
            </button>
            <button
              className={`pill-btn pill-hard ${selectedDifficulty === 'HARD' ? 'active' : ''}`}
              onClick={() => handleDifficultyChange('HARD')}
            >
              Hard
            </button>
          </div>
        </div>
      </div>

      {/* Tags Filter Bar */}
      <div className="tags-filter-bar">
        <span className="tags-label">
          <TagIcon size={14} /> Tags:
        </span>
        <button
          className={`tag-chip ${selectedTag === undefined ? 'active' : ''}`}
          onClick={() => handleTagChange(undefined)}
        >
          All Topics
        </button>
        {COMMON_TAGS.map((tag) => (
          <button
            key={tag}
            className={`tag-chip ${selectedTag === tag ? 'active' : ''}`}
            onClick={() => handleTagChange(selectedTag === tag ? undefined : tag)}
          >
            {tag.replace('_', ' ')}
          </button>
        ))}
      </div>

      {/* Content Area */}
      {isLoading ? (
        <div className="problems-loading">
          <LoadingSpinner message="Loading coding challenges..." />
        </div>
      ) : error ? (
        <div className="problems-error-card">
          <AlertCircle size={36} className="error-card-icon" />
          <h3>Unable to fetch problems</h3>
          <p>{error}</p>
          <button className="btn btn-secondary" onClick={fetchProblems}>
            <RefreshCw size={16} /> Try Again
          </button>
        </div>
      ) : filteredProblems.length === 0 ? (
        <div className="problems-empty-card">
          <BookOpen size={36} className="empty-card-icon" />
          <h3>No problems found</h3>
          <p>Try resetting filters or adjusting your search keywords.</p>
          {(selectedDifficulty || selectedTag || searchQuery) && (
            <button
              className="btn btn-secondary"
              onClick={() => {
                setSelectedDifficulty(undefined);
                setSelectedTag(undefined);
                setSearchQuery('');
              }}
            >
              Clear Filters
            </button>
          )}
        </div>
      ) : (
        <div className="problems-table-container">
          <table className="problems-table">
            <thead>
              <tr>
                <th style={{ width: '8%' }}>#</th>
                <th style={{ width: '45%' }}>Title</th>
                <th style={{ width: '15%' }}>Difficulty</th>
                <th style={{ width: '22%' }}>Tags</th>
                <th style={{ width: '10%', textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredProblems.map((problem, index) => (
                <tr key={problem.id} className="problem-row">
                  <td className="row-number">
                    {currentPage * pageSize + index + 1}
                  </td>
                  <td className="row-title">
                    <Link to={`/problems/${problem.slug}`} className="problem-title-link">
                      {problem.title}
                    </Link>
                  </td>
                  <td>
                    <DifficultyBadge difficulty={problem.difficulty} />
                  </td>
                  <td>
                    <div className="row-tags">
                      {problem.tags?.map((tag) => (
                        <span key={tag} className="problem-tag">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <Link to={`/problems/${problem.slug}`} className="btn btn-outline btn-sm">
                      Solve
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination Controls */}
      {problemsPage && problemsPage.totalPages > 1 && (
        <div className="pagination-bar">
          <div className="pagination-info">
            Showing {problemsPage.pageNumber * problemsPage.pageSize + 1} to{' '}
            {Math.min(
              (problemsPage.pageNumber + 1) * problemsPage.pageSize,
              problemsPage.totalElements
            )}{' '}
            of {problemsPage.totalElements} problems
          </div>
          <div className="pagination-buttons">
            <button
              className="btn btn-secondary btn-sm"
              disabled={problemsPage.isFirst || isLoading}
              onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 0))}
            >
              <ChevronLeft size={16} /> Previous
            </button>
            <span className="page-indicator">
              Page {problemsPage.pageNumber + 1} of {problemsPage.totalPages}
            </span>
            <button
              className="btn btn-secondary btn-sm"
              disabled={problemsPage.isLast || isLoading}
              onClick={() => setCurrentPage((prev) => prev + 1)}
            >
              Next <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
