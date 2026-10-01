import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ShieldCheck, Code, Layers, FileCode, PlusCircle, ArrowRight, Activity, Users } from 'lucide-react';
import { problemService } from '../../services/problemService';
import { submissionService } from '../../services/submissionService';
import { SubmissionSummaryResponse } from '../../types/submission';
import { StatusBadge } from '../../components/StatusBadge';
import { LoadingSpinner } from '../../components/LoadingSpinner';

export const AdminDashboardPage: React.FC = () => {
  const [problemCount, setProblemCount] = useState<number>(0);
  const [submissionCount, setSubmissionCount] = useState<number>(0);
  const [recentSubmissions, setRecentSubmissions] = useState<SubmissionSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const fetchAdminStats = async () => {
      setIsLoading(true);
      try {
        const [problemsRes, subsRes] = await Promise.all([
          problemService.getProblems({ page: 0, size: 1 }),
          submissionService.getSubmissions({ page: 0, size: 5 }),
        ]);
        setProblemCount(problemsRes.totalElements || 0);
        setSubmissionCount(subsRes.totalElements || 0);
        setRecentSubmissions(subsRes.content || []);
      } catch (err) {
        console.error('Failed to load admin stats:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAdminStats();
  }, []);

  if (isLoading) {
    return (
      <div className="page-container center-loader">
        <LoadingSpinner message="Loading administrative portal..." />
      </div>
    );
  }

  return (
    <div className="page-container admin-dashboard-page">
      <div className="admin-header">
        <div className="admin-badge">
          <ShieldCheck size={14} /> Administrative Control Center
        </div>
        <h1 className="page-title">Platform Administration</h1>
        <p className="page-subtitle">
          Manage coding problems, curate public/hidden test suites, and audit platform submissions.
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
            <span className="metric-label">Platform Submissions</span>
          </div>
          <Link to="/admin/submissions" className="metric-link">
            Audit Submissions <ArrowRight size={14} />
          </Link>
        </div>

        <div className="metric-card">
          <div className="metric-icon-box bg-amber">
            <Users size={24} />
          </div>
          <div className="metric-data">
            <span className="metric-value">Java 17</span>
            <span className="metric-label">Primary Execution Target</span>
          </div>
          <Link to="/problems" className="metric-link">
            Learner View <ArrowRight size={14} />
          </Link>
        </div>
      </div>

      {/* Quick Action Bar */}
      <div className="admin-actions-bar">
        <h2>Quick Management Actions</h2>
        <div className="action-buttons-row">
          <Link to="/admin/problems" className="btn btn-primary">
            <PlusCircle size={16} /> Add New Problem
          </Link>
          <Link to="/admin/problems" className="btn btn-secondary">
            <Code size={16} /> Manage Problem Catalog
          </Link>
          <Link to="/admin/submissions" className="btn btn-secondary">
            <Activity size={16} /> Live Submission Stream
          </Link>
        </div>
      </div>

      {/* Recent Submissions Section */}
      <div className="admin-section-card">
        <div className="section-header-row">
          <h2>
            <Activity size={18} className="text-accent" /> Recent Platform Submissions
          </h2>
          <Link to="/admin/submissions" className="view-all-link">
            View All Submissions <ArrowRight size={14} />
          </Link>
        </div>

        {recentSubmissions.length === 0 ? (
          <p className="text-muted text-center py-4">No submissions recorded yet.</p>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Submission ID</th>
                  <th>User ID</th>
                  <th>Problem ID</th>
                  <th>Language</th>
                  <th>Status</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {recentSubmissions.map((sub) => (
                  <tr key={sub.id}>
                    <td className="font-mono text-muted">{sub.id.substring(0, 8)}...</td>
                    <td className="font-mono text-xs text-muted">{sub.userId.substring(0, 8)}...</td>
                    <td className="font-mono text-xs text-muted">{sub.problemId.substring(0, 8)}...</td>
                    <td>
                      <span className="lang-pill">{sub.language}</span>
                    </td>
                    <td>
                      <StatusBadge status={sub.status} size="sm" />
                    </td>
                    <td className="text-muted text-sm">{new Date(sub.createdAt).toLocaleTimeString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
