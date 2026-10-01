import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  BarChart3,
  CheckCircle2,
  TrendingUp,
  AlertTriangle,
  Award,
  BookOpen,
  ArrowRight,
  RefreshCw,
  Clock,
  Sparkles,
  History,
  Target
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { userService } from '../services/userService';
import { submissionService } from '../services/submissionService';
import { problemService } from '../services/problemService';
import { LearnerStatistics } from '../types/user';
import { SubmissionSummaryResponse } from '../types/submission';
import { ProblemSummary } from '../types/problem';
import { StatusBadge } from '../components/StatusBadge';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();

  const [statistics, setStatistics] = useState<LearnerStatistics | null>(null);
  const [recentSubmissions, setRecentSubmissions] = useState<SubmissionSummaryResponse[]>([]);
  const [problemsMap, setProblemsMap] = useState<Record<string, ProblemSummary>>({});

  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const loadDashboardData = async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const [statsRes, submissionsRes, problemsRes] = await Promise.all([
        userService.getLearnerStatistics().catch(() => null),
        submissionService.getSubmissions({ page: 0, size: 5, sortBy: 'createdAt', sortDirection: 'desc' }).catch(() => ({ content: [] })),
        problemService.getProblems({ page: 0, size: 50 }).catch(() => ({ content: [] })),
      ]);

      setStatistics(statsRes);
      setRecentSubmissions(submissionsRes?.content || []);

      const pMap: Record<string, ProblemSummary> = {};
      problemsRes?.content?.forEach((p) => {
        pMap[p.id] = p;
      });
      setProblemsMap(pMap);
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
            <span>Learner Analytics & Mastery</span>
          </div>
          <h1 className="dashboard-title">
            Welcome back, <span className="text-gradient">{user?.fullName || 'Learner'}</span>!
          </h1>
          <p className="dashboard-subtitle">
            Track your coding progress, identify recurring mistake patterns, and master algorithmic concepts with Socratic guidance.
          </p>
        </div>
        <div className="dashboard-header-actions">
          <Link to="/problems" className="btn btn-primary">
            <BookOpen size={16} /> Solve Problems
          </Link>
          <Link to="/submissions" className="btn btn-secondary">
            <History size={16} /> Full History
          </Link>
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

          {/* Recent Submissions Section */}
          <div className="dashboard-panel-card recent-submissions-card" data-testid="section-recent-submissions">
            <div className="panel-card-header">
              <div className="panel-header-title">
                <Clock size={20} className="text-muted" />
                <h3>Recent Submissions</h3>
              </div>
              <Link to="/submissions" className="btn btn-secondary btn-sm">
                View All <ArrowRight size={14} />
              </Link>
            </div>

            {recentSubmissions.length === 0 ? (
              <div className="panel-empty-substate">
                <p>No recent submissions found.</p>
              </div>
            ) : (
              <div className="table-responsive">
                <table className="custom-table">
                  <thead>
                    <tr>
                      <th>Problem</th>
                      <th>Verdict</th>
                      <th>Language</th>
                      <th>Runtime</th>
                      <th>Submitted At</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentSubmissions.map((sub) => {
                      const problem = problemsMap[sub.problemId];
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
                            <StatusBadge status={sub.status} />
                          </td>
                          <td>
                            <span className="lang-tag">{sub.language || 'JAVA'}</span>
                          </td>
                          <td className="text-muted">
                            {sub.executionTimeMs != null ? `${sub.executionTimeMs} ms` : '-'}
                          </td>
                          <td className="text-muted">
                            {formatDate(sub.createdAt)}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};
