import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ShieldCheck,
  Trophy,
  Layers,
  FileCode,
  PlusCircle,
  ArrowRight,
  Activity,
  LifeBuoy,
  TrendingUp,
  BarChart3,
  PieChart,
  RefreshCw,
  Search,
  Filter,
  Eye,
  Calendar,
  User,
  ChevronLeft,
  ChevronRight,
  Clock,
} from 'lucide-react';
import { problemService } from '../../services/problemService';
import { submissionService } from '../../services/submissionService';
import { supportService } from '../../services/supportService';
import { SubmissionSummaryResponse, SubmissionResponse } from '../../types/submission';
import { ProblemSummaryResponse } from '../../types/problem';
import { StatusBadge } from '../../components/StatusBadge';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { Modal } from '../../components/Modal';

export const AdminDashboardPage: React.FC = () => {
  const [problemCount, setProblemCount] = useState<number>(0);
  const [submissionCount, setSubmissionCount] = useState<number>(0);
  const [openTicketsCount, setOpenTicketsCount] = useState<number>(0);
  const [problems, setProblems] = useState<ProblemSummaryResponse[]>([]);
  const [problemsMap, setProblemsMap] = useState<Record<string, ProblemSummaryResponse>>({});
  const [allSubmissionsSample, setAllSubmissionsSample] = useState<SubmissionSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Paginated Submissions Stream State
  const [subPage, setSubPage] = useState<number>(0);
  const [subTotalPages, setSubTotalPages] = useState<number>(1);
  const [subTotalElements, setSubTotalElements] = useState<number>(0);
  const [submissionsList, setSubmissionsList] = useState<SubmissionSummaryResponse[]>([]);
  const [isSubmissionsLoading, setIsSubmissionsLoading] = useState<boolean>(false);
  const [verdictFilter, setVerdictFilter] = useState<string>('ALL');
  const [submissionSearch, setSubmissionSearch] = useState<string>('');

  // Code inspection modal state
  const [selectedSubmission, setSelectedSubmission] = useState<SubmissionResponse | null>(null);
  const [isModalLoading, setIsModalLoading] = useState<boolean>(false);

  const fetchAdminStats = async () => {
    setIsLoading(true);
    try {
      const [problemsRes, subsBatch, tickets] = await Promise.all([
        problemService.getProblems({ page: 0, size: 50 }),
        submissionService.getSubmissions({ page: 0, size: 100 }),
        supportService.getAllTickets(),
      ]);

      const pList = problemsRes.content || [];
      setProblems(pList);
      setProblemCount(problemsRes.totalElements || 0);
      setAllSubmissionsSample(subsBatch.content || []);
      setOpenTicketsCount(tickets.filter((t) => t.status === 'OPEN').length);

      const map: Record<string, ProblemSummaryResponse> = {};
      pList.forEach((p) => {
        map[p.id] = p;
      });
      setProblemsMap(map);
    } catch (err) {
      console.error('Failed to load admin stats:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchSubmissionsPage = async (pageNumber: number) => {
    setIsSubmissionsLoading(true);
    try {
      const res = await submissionService.getSubmissions({ page: pageNumber, size: 15 });
      setSubmissionsList(res.content || []);
      setSubTotalPages(res.totalPages || 1);
      setSubTotalElements(res.totalElements || 0);
      setSubmissionCount(res.totalElements || 0);
    } catch (err) {
      console.error('Failed to fetch submissions page:', err);
    } finally {
      setIsSubmissionsLoading(false);
    }
  };

  useEffect(() => {
    fetchAdminStats();
  }, []);

  useEffect(() => {
    fetchSubmissionsPage(subPage);
  }, [subPage]);

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

  // Compute real 7-day daily submission counts from all submissions
  const daysOfWeek = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const today = new Date();
  const dayIndices = [6, 5, 4, 3, 2, 1, 0].map((offset) => {
    const d = new Date(today);
    d.setDate(today.getDate() - offset);
    return {
      dayName: daysOfWeek[d.getDay()],
      dateStr: d.toISOString().split('T')[0],
      total: 0,
      accepted: 0,
    };
  });

  allSubmissionsSample.forEach((sub) => {
    const subDate = sub.createdAt ? sub.createdAt.split('T')[0] : '';
    const matched = dayIndices.find((d) => d.dateStr === subDate);
    if (matched) {
      matched.total += 1;
      if (sub.verdict === 'ACCEPTED') {
        matched.accepted += 1;
      }
    }
  });

  const maxDailyTotal = Math.max(1, ...dayIndices.map((d) => d.total));

  // Compute real verdict distribution
  const totalSubmissionsSample = allSubmissionsSample.length;
  const acceptedCount = allSubmissionsSample.filter((s) => s.verdict === 'ACCEPTED').length;
  const wrongCount = allSubmissionsSample.filter((s) => s.verdict === 'WRONG_ANSWER').length;
  const tleCount = allSubmissionsSample.filter((s) => s.verdict === 'TIME_LIMIT_EXCEEDED').length;
  const compileErrorCount = allSubmissionsSample.filter((s) => s.verdict === 'COMPILE_ERROR').length;
  const runtimeCount = allSubmissionsSample.filter((s) => s.verdict === 'RUNTIME_ERROR').length;

  const verdictDistribution = [
    {
      label: 'Accepted',
      count: acceptedCount,
      color: '#10b981',
      percentage: totalSubmissionsSample > 0 ? Math.round((acceptedCount / totalSubmissionsSample) * 100) : 0,
    },
    {
      label: 'Wrong Answer',
      count: wrongCount,
      color: '#ef4444',
      percentage: totalSubmissionsSample > 0 ? Math.round((wrongCount / totalSubmissionsSample) * 100) : 0,
    },
    {
      label: 'Time Limit',
      count: tleCount,
      color: '#f59e0b',
      percentage: totalSubmissionsSample > 0 ? Math.round((tleCount / totalSubmissionsSample) * 100) : 0,
    },
    {
      label: 'Compile Error',
      count: compileErrorCount,
      color: '#8b5cf6',
      percentage: totalSubmissionsSample > 0 ? Math.round((compileErrorCount / totalSubmissionsSample) * 100) : 0,
    },
    {
      label: 'Runtime Error',
      count: runtimeCount,
      color: '#ec4899',
      percentage: totalSubmissionsSample > 0 ? Math.round((runtimeCount / totalSubmissionsSample) * 100) : 0,
    },
  ];

  const easyCount = problems.filter((p) => p.difficulty === 'EASY').length;
  const mediumCount = problems.filter((p) => p.difficulty === 'MEDIUM').length;
  const hardCount = problems.filter((p) => p.difficulty === 'HARD').length;
  const totalDiff = Math.max(1, easyCount + mediumCount + hardCount);

  // Filtered submissions list for the live audit table
  const filteredSubmissions = submissionsList.filter((sub) => {
    if (verdictFilter !== 'ALL' && sub.verdict !== verdictFilter) {
      return false;
    }
    if (submissionSearch.trim()) {
      const q = submissionSearch.toLowerCase();
      const prob = problemsMap[sub.problemId];
      const probTitle = prob?.title.toLowerCase() || '';
      const probId = sub.problemId.toLowerCase();
      const subId = sub.id.toLowerCase();
      const userId = sub.userId.toLowerCase();
      return probTitle.includes(q) || probId.includes(q) || subId.includes(q) || userId.includes(q);
    }
    return true;
  });

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

  if (isLoading) {
    return (
      <div className="page-container center-loader">
        <LoadingSpinner message="Loading administrative portal and analytics..." />
      </div>
    );
  }

  return (
    <div className="page-container admin-dashboard-page">
      <div className="admin-header">
        <div className="admin-badge">
          <ShieldCheck size={14} /> Administrative Control Center & Analytics
        </div>
        <h1 className="page-title">Platform Administration & Insights</h1>
        <p className="page-subtitle">
          Manage coding problems, audit learner executions, resolve student bug reports, and track real-time metrics.
        </p>
      </div>

      {/* Metric Cards Grid */}
      <div className="metrics-grid">
        <div className="metric-card">
          <div className="metric-icon-box bg-indigo">
            <Layers size={24} />
          </div>
          <div className="metric-data">
            <span className="metric-value">{problemCount}</span>
            <span className="metric-label">Active Coding Problems</span>
          </div>
          <Link to="/admin/problems" className="metric-link">
            Manage Problems <ArrowRight size={14} />
          </Link>
        </div>

        <div className="metric-card">
          <div className="metric-icon-box bg-emerald">
            <FileCode size={24} />
          </div>
          <div className="metric-data">
            <span className="metric-value">{submissionCount}</span>
            <span className="metric-label">Total Submissions</span>
          </div>
          <a href="#admin-submissions-section" className="metric-link">
            Audit Stream <ArrowRight size={14} />
          </a>
        </div>

        <div className="metric-card">
          <div className="metric-icon-box bg-amber">
            <LifeBuoy size={24} />
          </div>
          <div className="metric-data">
            <span className="metric-value">{openTicketsCount}</span>
            <span className="metric-label">Open Support Tickets</span>
          </div>
          <Link to="/admin/support" className="metric-link">
            Help Desk Inbox <ArrowRight size={14} />
          </Link>
        </div>

        <div className="metric-card">
          <div className="metric-icon-box bg-rose">
            <TrendingUp size={24} />
          </div>
          <div className="metric-data">
            <span className="metric-value">
              {submissionCount > 0 ? `${Math.round((acceptedCount / Math.max(1, totalSubmissionsSample)) * 100)}%` : '100%'}
            </span>
            <span className="metric-label">Platform Success Rate</span>
          </div>
          <span className="metric-sublabel text-xs text-muted">Real-time compilation & execution</span>
        </div>
      </div>

      {/* Interactive Analytics Graphs Section */}
      <div className="admin-analytics-grid">
        {/* Graph 1: Submission Velocity Trend Chart */}
        <div className="admin-chart-card">
          <div className="chart-header">
            <div>
              <h3 className="chart-title">
                <BarChart3 size={18} className="text-accent" /> Submission Velocity (Past 7 Days)
              </h3>
              <p className="chart-subtitle">Daily execution volume across platform sandboxes</p>
            </div>
          </div>

          <div className="chart-visual-container">
            <div className="bar-chart-bars">
              {dayIndices.map((item) => (
                <div key={item.dateStr} className="bar-column">
                  <div className="bar-track">
                    <div
                      className="bar-fill-total"
                      style={{ height: `${(item.total / maxDailyTotal) * 100}%` }}
                      title={`Total: ${item.total}`}
                    >
                      <div
                        className="bar-fill-accepted"
                        style={{ height: `${item.total > 0 ? (item.accepted / item.total) * 100 : 0}%` }}
                        title={`Accepted: ${item.accepted}`}
                      />
                    </div>
                  </div>
                  <span className="bar-label">{item.dayName}</span>
                  <span className="bar-val-sub">{item.total}</span>
                </div>
              ))}
            </div>

            <div className="chart-legend">
              <span className="legend-item">
                <span className="legend-dot bg-total" /> Total Submissions
              </span>
              <span className="legend-item">
                <span className="legend-dot bg-accepted" /> Accepted (Passed)
              </span>
            </div>
          </div>
        </div>

        {/* Graph 2: Verdict & Difficulty Breakdown */}
        <div className="admin-chart-card">
          <div className="chart-header">
            <div>
              <h3 className="chart-title">
                <PieChart size={18} className="text-yellow" /> Verdict Distribution & Problem Catalog
              </h3>
              <p className="chart-subtitle">Execution outcomes and curriculum balance</p>
            </div>
          </div>

          <div className="chart-verdict-breakdown">
            <div className="verdict-bar-stacked">
              {verdictDistribution.map((v) => (
                <div
                  key={v.label}
                  className="verdict-bar-slice"
                  style={{ width: `${v.percentage}%`, backgroundColor: v.color }}
                  title={`${v.label}: ${v.percentage}%`}
                />
              ))}
            </div>

            <div className="verdict-legend-grid">
              {verdictDistribution.map((v) => (
                <div key={v.label} className="verdict-legend-item">
                  <span className="legend-dot" style={{ backgroundColor: v.color }} />
                  <span className="verdict-name">{v.label}</span>
                  <span className="verdict-pct font-mono">{v.percentage}% ({v.count})</span>
                </div>
              ))}
            </div>

            <div className="catalog-difficulty-strip">
              <h4 className="strip-title">Curriculum Difficulty Ratio</h4>
              <div className="diff-progress-bar">
                <div className="diff-seg-easy" style={{ width: `${(easyCount / totalDiff) * 100}%` }} />
                <div className="diff-seg-med" style={{ width: `${(mediumCount / totalDiff) * 100}%` }} />
                <div className="diff-seg-hard" style={{ width: `${(hardCount / totalDiff) * 100}%` }} />
              </div>
              <div className="diff-labels-row">
                <span className="text-easy">Easy: {easyCount}</span>
                <span className="text-med">Medium: {mediumCount}</span>
                <span className="text-hard">Hard: {hardCount}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Action Bar */}
      <div className="admin-actions-bar">
        <h2>Administrative Operations</h2>
        <div className="action-buttons-row">
          <Link to="/admin/problems" className="btn btn-primary">
            <PlusCircle size={16} /> Add New Problem
          </Link>
          <Link to="/admin/contests" className="btn btn-secondary">
            <Trophy size={16} /> Manage & Schedule Contests
          </Link>
          <Link to="/admin/support" className="btn btn-secondary">
            <LifeBuoy size={16} /> Help Desk ({openTicketsCount} Open)
          </Link>
        </div>
      </div>

      {/* Full Paginated Platform Submissions Stream Section */}
      <div className="admin-section-card" id="admin-submissions-section">
        <div className="section-header-row">
          <div>
            <h2>
              <Activity size={18} className="text-accent" /> Platform Submission Audits & Logs
            </h2>
            <p className="text-sm text-muted">
              Live stream of student code solutions, execution verdicts, and sandbox test results.
            </p>
          </div>
          <div className="section-header-actions">
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => fetchSubmissionsPage(subPage)}
              disabled={isSubmissionsLoading}
              title="Refresh submissions"
            >
              <RefreshCw size={14} className={isSubmissionsLoading ? 'spin' : ''} /> Refresh Stream
            </button>
          </div>
        </div>

        {/* Filter & Search Toolbar */}
        <div className="submission-filter-toolbar">
          <div className="toolbar-search-box">
            <Search size={14} className="search-prefix-icon" />
            <input
              type="text"
              placeholder="Filter by problem title, user ID, or submission ID..."
              value={submissionSearch}
              onChange={(e) => setSubmissionSearch(e.target.value)}
              className="toolbar-search-input"
            />
          </div>

          <div className="toolbar-chips">
            <Filter size={13} className="text-muted" />
            {['ALL', 'ACCEPTED', 'WRONG_ANSWER', 'COMPILE_ERROR', 'TIME_LIMIT_EXCEEDED', 'RUNTIME_ERROR'].map((v) => (
              <button
                key={v}
                type="button"
                className={`filter-chip ${verdictFilter === v ? 'active' : ''}`}
                onClick={() => setVerdictFilter(v)}
              >
                {v === 'ALL' ? 'All Verdicts' : formatVerdictLabel(v)}
              </button>
            ))}
          </div>
        </div>

        {isSubmissionsLoading ? (
          <div className="center-loader py-8">
            <LoadingSpinner message="Loading submissions stream..." />
          </div>
        ) : filteredSubmissions.length === 0 ? (
          <div className="panel-empty-substate">
            <p>No submissions matching your current filter criteria.</p>
          </div>
        ) : (
          <>
            <div className="table-responsive">
              <table className="custom-table">
                <thead>
                  <tr>
                    <th>Submission ID</th>
                    <th>Learner User ID</th>
                    <th>Problem</th>
                    <th>Verdict</th>
                    <th>Tests</th>
                    <th>Language</th>
                    <th>Runtime</th>
                    <th>Submitted At</th>
                    <th className="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSubmissions.map((sub) => {
                    const prob = problemsMap[sub.problemId];
                    return (
                      <tr key={sub.id}>
                        <td className="font-mono text-muted">
                          {sub.id.substring(0, 8)}...
                        </td>
                        <td className="font-mono text-xs text-secondary">
                          <span className="flex-align-gap">
                            <User size={12} className="text-muted" />
                            {sub.userId.substring(0, 8)}...
                          </span>
                        </td>
                        <td>
                          {prob ? (
                            <Link to={`/problems/${prob.slug}`} className="table-link font-medium">
                              {prob.title}
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
                        <td className="text-muted text-sm">
                          {sub.executionTimeMs != null ? (
                            <span className="flex-align-gap font-mono text-xs">
                              <Clock size={12} /> {sub.executionTimeMs}ms
                            </span>
                          ) : (
                            <span className="text-xs text-muted">-</span>
                          )}
                        </td>
                        <td className="text-muted text-sm">
                          <span className="flex-align-gap">
                            <Calendar size={13} />
                            {new Date(sub.createdAt).toLocaleString(undefined, {
                              month: 'short',
                              day: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </span>
                        </td>
                        <td className="text-right">
                          <button
                            type="button"
                            className="btn-icon"
                            onClick={() => viewSubmissionCode(sub.id)}
                            title="Inspect learner solution code"
                          >
                            <Eye size={16} />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            <div className="pagination-bar mt-4">
              <span className="pagination-info">
                Showing page {subPage + 1} of {subTotalPages} ({subTotalElements} total platform submissions)
              </span>
              <div className="pagination-actions">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  disabled={subPage === 0 || isSubmissionsLoading}
                  onClick={() => setSubPage((prev) => Math.max(0, prev - 1))}
                >
                  <ChevronLeft size={16} /> Previous
                </button>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  disabled={subPage >= subTotalPages - 1 || isSubmissionsLoading}
                  onClick={() => setSubPage((prev) => prev + 1)}
                >
                  Next <ChevronRight size={16} />
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Code Inspector Modal */}
      <Modal
        isOpen={!!selectedSubmission || isModalLoading}
        onClose={() => setSelectedSubmission(null)}
        title={
          selectedSubmission
            ? `Learner Code Inspector (${selectedSubmission.language})`
            : 'Loading...'
        }
        maxWidth="800px"
      >
        {isModalLoading ? (
          <div className="center-loader py-8">
            <LoadingSpinner message="Retrieving submission code and sandbox logs..." />
          </div>
        ) : selectedSubmission ? (
          <div className="submission-modal-details">
            <div className="submission-meta-row">
              <div className="meta-block">
                <label>Status / Verdict:</label>
                <div className="mt-1">
                  {selectedSubmission.verdict ? (
                    <span className={getVerdictBadgeClass(selectedSubmission.verdict)}>
                      {formatVerdictLabel(selectedSubmission.verdict)}
                    </span>
                  ) : (
                    <StatusBadge status={selectedSubmission.status} />
                  )}
                </div>
              </div>
              <div className="meta-block">
                <label>Learner User ID:</label>
                <span className="font-mono text-xs">{selectedSubmission.userId}</span>
              </div>
              <div className="meta-block">
                <label>Problem:</label>
                <span className="text-sm font-medium">
                  {problemsMap[selectedSubmission.problemId]?.title || selectedSubmission.problemId}
                </span>
              </div>
              {selectedSubmission.executionTimeMs != null && (
                <div className="meta-block">
                  <label>Execution Runtime:</label>
                  <span className="font-mono text-xs text-accent">
                    {selectedSubmission.executionTimeMs} ms
                  </span>
                </div>
              )}
            </div>

            {selectedSubmission.errorMessage && (
              <div className="error-alert mt-4">
                <strong>Sandbox Output / Compilation Error:</strong>
                <pre>{selectedSubmission.errorMessage}</pre>
              </div>
            )}

            <div className="source-code-viewer mt-4">
              <label>Submitted Solution Code:</label>
              <pre className="code-block font-mono">
                <code>{selectedSubmission.sourceCode}</code>
              </pre>
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
};
