import React, { useEffect, useState, useCallback } from 'react';
import { Link, Navigate } from 'react-router-dom';
import {
  BarChart3,
  CheckCircle2,
  TrendingUp,
  AlertTriangle,
  Award,
  BookOpen,
  ArrowRight,
  RefreshCw,
  Sparkles,
  History,
  Target,
  ChevronLeft,
  ChevronRight,
  Eye,
  Calendar,
  Filter,
  Search,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { userService } from '../services/userService';
import { submissionService } from '../services/submissionService';
import { problemService } from '../services/problemService';
import { LearnerStatistics } from '../types/user';
import { SubmissionSummaryResponse, SubmissionResponse } from '../types/submission';
import { ProblemSummary } from '../types/problem';
import { StatusBadge } from '../components/StatusBadge';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { Modal } from '../components/Modal';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();

  if (user?.role === 'ADMIN') {
    return <Navigate to="/admin" replace />;
  }

  const [statistics, setStatistics] = useState<LearnerStatistics | null>(null);
  const [submissions, setSubmissions] = useState<SubmissionSummaryResponse[]>([]);
  const [problemsMap, setProblemsMap] = useState<Record<string, ProblemSummary>>({});

  const [isLoading, setIsLoading] = useState(true);
  const [isSubmissionsLoading, setIsSubmissionsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Pagination & Filter state for submissions
  const [subPage, setSubPage] = useState(0);
  const [subTotalPages, setSubTotalPages] = useState(1);
  const [subTotalElements, setSubTotalElements] = useState(0);
  const [verdictFilter, setVerdictFilter] = useState<string>('ALL');
  const [submissionSearch, setSubmissionSearch] = useState('');

  // Code inspection modal
  const [selectedSubmission, setSelectedSubmission] = useState<SubmissionResponse | null>(null);
  const [isModalLoading, setIsModalLoading] = useState(false);

  const fetchSubmissionsPage = useCallback(async (pageNumber: number) => {
    setIsSubmissionsLoading(true);
    try {
      const res = await submissionService.getSubmissions({
        page: pageNumber,
        size: 10,
        sortBy: 'createdAt',
        sortDirection: 'desc',
      });
      setSubmissions(res?.content || []);
      setSubTotalPages(res?.totalPages || 1);
      setSubTotalElements(res?.totalElements || 0);
    } catch (err) {
      console.error('Failed to load paginated submissions:', err);
    } finally {
      setIsSubmissionsLoading(false);
    }
  }, []);

  const loadDashboardData = async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const [statsRes, problemsRes] = await Promise.all([
        userService.getLearnerStatistics().catch(() => null),
        problemService.getProblems({ page: 0, size: 100 }).catch(() => ({ content: [] })),
      ]);

      setStatistics(statsRes);

      const pMap: Record<string, ProblemSummary> = {};
      problemsRes?.content?.forEach((p) => {
        pMap[p.id] = p;
      });
      setProblemsMap(pMap);

      await fetchSubmissionsPage(0);
      setSubPage(0);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load learner dashboard data.';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < subTotalPages) {
      setSubPage(newPage);
      fetchSubmissionsPage(newPage);
    }
  };

  const viewSubmissionCode = async (submissionId: string) => {
    setIsModalLoading(true);
    try {
      const sub = await submissionService.getSubmissionById(submissionId);
      setSelectedSubmission(sub);
    } catch (err) {
      console.error('Failed to fetch submission details:', err);
    } finally {
      setIsModalLoading(false);
    }
  };

  const formatMistakeName = (rawType: string): string => {
    return rawType
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  };

  const formatTopicName = (topic: string): string => {
    return topic
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  };

  const formatDate = (isoString: string): string => {
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  // Calculations for charts
  const totalSubmissions = statistics?.totalSubmissions || 0;
  const totalAccepted = statistics?.totalAccepted || 0;
  const accuracyPercentage = statistics?.accuracyPercentage || 0;
  const problemsSolved = statistics?.problemsSolved || 0;
  const problemsAttempted = statistics?.problemsAttempted || 0;
  const averageAttempts = statistics?.averageAttemptsPerProblem || 0;

  // Extract common mistakes list
  const mistakeList = Object.entries(statistics?.mistakeBreakdown || {})
    .map(([type, count]) => ({ type, count }))
    .sort((a, b) => b.count - a.count);

  const totalMistakeCount = mistakeList.reduce((acc, curr) => acc + curr.count, 0);

  // Extract weak topics list
  const weakTopicsList = Object.entries(statistics?.weakTopics || {})
    .map(([topic, count]) => ({ topic, count }))
    .sort((a, b) => b.count - a.count);

  const maxTopicCount = Math.max(...weakTopicsList.map((t) => t.count), 1);

  // Filter submissions by verdict and search query locally
  const filteredSubmissions = submissions.filter((sub) => {
    const prob = problemsMap[sub.problemId];
    const matchesVerdict = verdictFilter === 'ALL' || sub.verdict === verdictFilter;
    const matchesSearch =
      !submissionSearch.trim() ||
      (prob?.title?.toLowerCase().includes(submissionSearch.toLowerCase())) ||
      (sub.id.toLowerCase().includes(submissionSearch.toLowerCase()));
    return matchesVerdict && matchesSearch;
  });

  if (isLoading) {
    return (
      <div className="page-container center-loader">
        <LoadingSpinner message="Assembling your learning analytics..." />
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className="page-container">
        <ErrorMessage message={errorMessage} />
        <div style={{ marginTop: '1rem', textAlign: 'center' }}>
          <button className="btn btn-primary" onClick={loadDashboardData}>
            <RefreshCw size={16} /> Try Again
          </button>
        </div>
      </div>
    );
  }

  const isEmpty = totalSubmissions === 0;

  return (
    <div className="page-container dashboard-page">
      {/* Header Banner */}
      <div className="dashboard-header-banner">
        <div className="dashboard-welcome">
          <div className="welcome-tag">
            <Sparkles size={14} className="text-accent" />
            <span>Learner Analytics & Activity</span>
          </div>
          <h1 className="dashboard-title">
            Welcome back, <span className="text-gradient">{user?.fullName || 'Learner'}</span>!
          </h1>
          <p className="dashboard-subtitle">
            Track your coding progress, inspect recurring mistake patterns, and review all your past submissions.
          </p>
        </div>
        <div className="dashboard-header-actions">
          <Link to="/problems" className="btn btn-primary">
            <BookOpen size={16} /> Solve Problems
          </Link>
          <button onClick={loadDashboardData} className="btn btn-secondary" title="Refresh metrics">
            <RefreshCw size={15} /> Refresh
          </button>
        </div>
      </div>

      {isEmpty ? (
        /* Empty State */
        <div className="dashboard-empty-card">
          <div className="empty-icon-wrapper">
            <BarChart3 size={48} className="text-muted" />
          </div>
          <h2>No Submissions Yet</h2>
          <p>
            You haven't submitted any code solutions yet. Solve your first problem to start seeing your performance metrics, mistake diagnostics, and weak topic analysis!
          </p>
          <Link to="/problems" className="btn btn-primary btn-lg" style={{ marginTop: '1.25rem' }}>
            Start Solving Now <ArrowRight size={18} />
          </Link>
        </div>
      ) : (
        <>
          {/* Key Metrics Grid */}
          <div className="dashboard-metrics-grid">
            {/* Metric 1: Problems Solved */}
            <div className="metric-card" data-testid="metric-problems-solved">
              <div className="metric-icon-box bg-success-soft">
                <CheckCircle2 size={24} className="text-success" />
              </div>
              <div className="metric-content">
                <span className="metric-label">Problems Solved</span>
                <div className="metric-value-row">
                  <span className="metric-number">{problemsSolved}</span>
                  <span className="metric-subtext">/ {problemsAttempted} attempted</span>
                </div>
                <div className="metric-progress-track">
                  <div
                    className="metric-progress-bar bg-success"
                    style={{
                      width: `${problemsAttempted > 0 ? (problemsSolved / problemsAttempted) * 100 : 0}%`,
                    }}
                  />
                </div>
              </div>
            </div>

            {/* Metric 2: Accepted Submissions */}
            <div className="metric-card" data-testid="metric-accepted-submissions">
              <div className="metric-icon-box bg-accent-soft">
                <Award size={24} className="text-accent" />
              </div>
              <div className="metric-content">
                <span className="metric-label">Accepted Submissions</span>
                <div className="metric-value-row">
                  <span className="metric-number">{totalAccepted}</span>
                  <span className="metric-subtext">/ {totalSubmissions} total</span>
                </div>
                <div className="metric-progress-track">
                  <div
                    className="metric-progress-bar bg-accent"
                    style={{
                      width: `${totalSubmissions > 0 ? (totalAccepted / totalSubmissions) * 100 : 0}%`,
                    }}
                  />
                </div>
              </div>
            </div>

            {/* Metric 3: Acceptance Rate */}
            <div className="metric-card" data-testid="metric-acceptance-rate">
              <div className="metric-icon-box bg-info-soft">
                <TrendingUp size={24} className="text-info" />
              </div>
              <div className="metric-content">
                <span className="metric-label">Acceptance Rate</span>
                <div className="metric-value-row">
                  <span className="metric-number">{accuracyPercentage}%</span>
                  <span className="metric-subtext">pass ratio</span>
                </div>
                <div className="metric-progress-track">
                  <div
                    className="metric-progress-bar bg-info"
                    style={{ width: `${Math.min(accuracyPercentage, 100)}%` }}
                  />
                </div>
              </div>
            </div>

            {/* Metric 4: Average Attempts */}
            <div className="metric-card" data-testid="metric-average-attempts">
              <div className="metric-icon-box bg-warning-soft">
                <Target size={24} className="text-warning" />
              </div>
              <div className="metric-content">
                <span className="metric-label">Average Attempts</span>
                <div className="metric-value-row">
                  <span className="metric-number">{averageAttempts}</span>
                  <span className="metric-subtext">tries / problem</span>
                </div>
                <div className="metric-subnote">Lower means faster convergence</div>
              </div>
            </div>
          </div>

          {/* Deep Learning Analytics: Mistake Breakdown & Weak Topics */}
          <div className="dashboard-analytics-grid">
            {/* Common Mistakes Breakdown Card */}
            <div className="dashboard-panel-card" data-testid="section-common-mistakes">
              <div className="panel-card-header">
                <div className="panel-header-title">
                  <AlertTriangle size={20} className="text-warning" />
                  <h3>Common Mistakes Breakdown</h3>
                </div>
                <span className="panel-badge-pill">
                  {totalMistakeCount} {totalMistakeCount === 1 ? 'mistake' : 'mistakes'} recorded
                </span>
              </div>
              <p className="panel-description">
                Recurring logical and runtime pitfalls detected by the AI analyzer across your submissions.
              </p>

              {mistakeList.length === 0 ? (
                <div className="panel-empty-substate">
                  <CheckCircle2 size={32} className="text-success" />
                  <p>No recurring mistakes detected! Great job writing clean code.</p>
                </div>
              ) : (
                <div className="mistakes-visual-list">
                  {mistakeList.map((item) => {
                    const percent = totalMistakeCount > 0 ? Math.round((item.count / totalMistakeCount) * 100) : 0;
                    return (
                      <div key={item.type} className="mistake-item-row">
                        <div className="mistake-item-info">
                          <span className="mistake-name">{formatMistakeName(item.type)}</span>
                          <span className="mistake-count">
                            {item.count} ({percent}%)
                          </span>
                        </div>
                        <div className="mistake-bar-track">
                          <div
                            className="mistake-bar-fill"
                            style={{ width: `${percent}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Weak Topics Analysis Card */}
            <div className="dashboard-panel-card" data-testid="section-weak-topics">
              <div className="panel-card-header">
                <div className="panel-header-title">
                  <BookOpen size={20} className="text-accent" />
                  <h3>Weak Topics & Focus Areas</h3>
                </div>
                <span className="panel-badge-pill">
                  {weakTopicsList.length} topics need practice
                </span>
              </div>
              <p className="panel-description">
                Data structures and algorithmic patterns where your submissions faced the highest resistance.
              </p>

              {weakTopicsList.length === 0 ? (
                <div className="panel-empty-substate">
                  <Sparkles size={32} className="text-accent" />
                  <p>No weak topic bottlenecks identified. Keep exploring diverse challenges!</p>
                </div>
              ) : (
                <div className="weak-topics-list">
                  {weakTopicsList.map((item) => {
                    const intensityPercent = Math.round((item.count / maxTopicCount) * 100);
                    return (
                      <div key={item.topic} className="topic-card-item">
                        <div className="topic-card-header">
                          <span className="topic-badge">{formatTopicName(item.topic)}</span>
                          <span className="topic-resistance-tag">
                            {item.count} {item.count === 1 ? 'hiccup' : 'hiccups'}
                          </span>
                        </div>
                        <div className="topic-progress-track">
                          <div
                            className="topic-progress-bar"
                            style={{ width: `${Math.max(intensityPercent, 15)}%` }}
                          />
                        </div>
                        <div className="topic-footer">
                          <Link to={`/problems`} className="topic-practice-link">
                            Practice {formatTopicName(item.topic)} <ArrowRight size={12} />
                          </Link>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Full Submissions Activity Section with Pagination */}
          <div className="dashboard-panel-card recent-submissions-card" data-testid="section-recent-submissions">
            <div className="panel-card-header flex-between">
              <div className="panel-header-title">
                <History size={20} className="text-accent" />
                <h3>Submission History & Log</h3>
              </div>
              <span className="panel-badge-pill">
                {subTotalElements} Total Submissions
              </span>
            </div>

            {/* Submissions Filter Toolbar */}
            <div className="submission-filter-toolbar">
              <div className="toolbar-search-box">
                <Search size={14} className="search-prefix-icon" />
                <input
                  type="text"
                  placeholder="Filter by problem title or ID..."
                  value={submissionSearch}
                  onChange={(e) => setSubmissionSearch(e.target.value)}
                  className="toolbar-search-input"
                />
              </div>

              <div className="toolbar-chips">
                <Filter size={13} className="text-muted" />
                {['ALL', 'ACCEPTED', 'WRONG_ANSWER', 'COMPILE_ERROR', 'TIME_LIMIT_EXCEEDED'].map((v) => (
                  <button
                    key={v}
                    type="button"
                    className={`filter-chip ${verdictFilter === v ? 'active' : ''}`}
                    onClick={() => setVerdictFilter(v)}
                  >
                    {v === 'ALL' ? 'All Verdicts' : formatMistakeName(v)}
                  </button>
                ))}
              </div>
            </div>

            {isSubmissionsLoading ? (
              <div className="center-loader py-8">
                <LoadingSpinner message="Loading submissions page..." />
              </div>
            ) : filteredSubmissions.length === 0 ? (
              <div className="panel-empty-substate">
                <p>No submissions matching your current filter.</p>
              </div>
            ) : (
              <>
                <div className="table-responsive">
                  <table className="custom-table">
                    <thead>
                      <tr>
                        <th>Problem</th>
                        <th>Verdict</th>
                        <th>Tests</th>
                        <th>Language</th>
                        <th>Runtime</th>
                        <th>Submitted At</th>
                        <th className="text-right">Code</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredSubmissions.map((sub) => {
                        const problem = problemsMap[sub.problemId];
                        const getVerdictBadgeClass = (verdict?: string | null) => {
                          switch (verdict) {
                            case 'ACCEPTED':
                              return 'verdict-badge verdict-accepted';
                            case 'WRONG_ANSWER':
                              return 'verdict-badge verdict-wrong';
                            case 'COMPILE_ERROR':
                              return 'verdict-badge verdict-compile';
                            case 'RUNTIME_ERROR':
                              return 'verdict-badge verdict-runtime';
                            case 'TIME_LIMIT_EXCEEDED':
                              return 'verdict-badge verdict-timeout';
                            case 'MEMORY_LIMIT_EXCEEDED':
                              return 'verdict-badge verdict-memory';
                            default:
                              return 'verdict-badge';
                          }
                        };

                        const formatVerdictLabel = (verdict?: string | null) => {
                          if (!verdict) return 'Pending';
                          return verdict
                            .split('_')
                            .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
                            .join(' ');
                        };

                        return (
                          <tr key={sub.id}>
                            <td>
                              {problem ? (
                                <Link to={`/problems/${problem.slug}`} className="table-link font-medium">
                                  {problem.title}
                                </Link>
                              ) : (
                                <span className="text-muted">Problem #{sub.problemId.substring(0, 8)}</span>
                              )}
                            </td>
                            <td>
                              {sub.verdict ? (
                                <span className={getVerdictBadgeClass(sub.verdict)}>
                                  {formatVerdictLabel(sub.verdict)}
                                </span>
                              ) : (
                                <StatusBadge status={sub.status} />
                              )}
                            </td>
                            <td>
                              {sub.totalTests != null && sub.totalTests > 0 ? (
                                <span className="text-xs text-muted">
                                  {sub.testsPassed || 0} / {sub.totalTests}
                                </span>
                              ) : (
                                <span className="text-xs text-muted">-</span>
                              )}
                            </td>
                            <td>
                              <span className="lang-tag">{sub.language || 'JAVA'}</span>
                            </td>
                            <td className="text-muted">
                              {sub.executionTimeMs != null ? `${sub.executionTimeMs} ms` : '-'}
                            </td>
                            <td className="text-muted text-sm">
                              <span className="flex-align-gap">
                                <Calendar size={13} />
                                {formatDate(sub.createdAt)}
                              </span>
                            </td>
                            <td className="text-right">
                              <button
                                type="button"
                                className="btn-icon"
                                onClick={() => viewSubmissionCode(sub.id)}
                                title="Inspect source code and execution logs"
                              >
                                <Eye size={15} />
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Submissions Pagination Controls */}
                <div className="pagination-bar">
                  <span className="pagination-info">
                    Showing page {subPage + 1} of {subTotalPages} ({subTotalElements} total submissions)
                  </span>
                  <div className="pagination-actions">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      disabled={subPage === 0}
                      onClick={() => handlePageChange(subPage - 1)}
                    >
                      <ChevronLeft size={15} /> Previous
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      disabled={subPage >= subTotalPages - 1}
                      onClick={() => handlePageChange(subPage + 1)}
                    >
                      Next <ChevronRight size={15} />
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </>
      )}

      {/* Submission Code Inspector Modal */}
      <Modal
        isOpen={!!selectedSubmission || isModalLoading}
        onClose={() => setSelectedSubmission(null)}
        title={
          selectedSubmission
            ? `Submission Details (${selectedSubmission.language})`
            : 'Loading Submission...'
        }
        maxWidth="750px"
      >
        {isModalLoading ? (
          <LoadingSpinner message="Retrieving source code..." />
        ) : selectedSubmission ? (
          <div className="submission-modal-details">
            <div className="submission-meta-row">
              <div className="meta-block">
                <label>Status:</label>
                <StatusBadge status={selectedSubmission.status} />
              </div>
              {selectedSubmission.verdict && (
                <div className="meta-block">
                  <label>Verdict:</label>
                  <span className={`verdict-tag verdict-${selectedSubmission.verdict.toLowerCase().replace(/_/g, '-')}`}>
                    {selectedSubmission.verdict}
                  </span>
                </div>
              )}
              {selectedSubmission.totalTests != null && (
                <div className="meta-block">
                  <label>Tests Passed:</label>
                  <span>{selectedSubmission.testsPassed || 0} / {selectedSubmission.totalTests}</span>
                </div>
              )}
              {selectedSubmission.executionTimeMs != null && (
                <div className="meta-block">
                  <label>Runtime:</label>
                  <span>{selectedSubmission.executionTimeMs} ms</span>
                </div>
              )}
              <div className="meta-block">
                <label>Submitted:</label>
                <span>{new Date(selectedSubmission.createdAt).toLocaleString()}</span>
              </div>
            </div>

            {selectedSubmission.errorMessage && (
              <div className="error-alert">
                <strong>Error Output:</strong>
                <pre>{selectedSubmission.errorMessage}</pre>
              </div>
            )}

            <div className="source-code-viewer">
              <label>Source Code:</label>
              <pre className="code-block">
                <code>{selectedSubmission.sourceCode}</code>
              </pre>
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
};
