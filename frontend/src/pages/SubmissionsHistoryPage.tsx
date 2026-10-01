import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { History, FileCode, RefreshCw, ChevronLeft, ChevronRight, Eye, Calendar } from 'lucide-react';
import { submissionService } from '../services/submissionService';
import { problemService } from '../services/problemService';
import { SubmissionSummaryResponse, SubmissionResponse } from '../types/submission';
import { ProblemSummary } from '../types/problem';
import { StatusBadge } from '../components/StatusBadge';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { Modal } from '../components/Modal';

export const SubmissionsHistoryPage: React.FC = () => {
  const [submissions, setSubmissions] = useState<SubmissionSummaryResponse[]>([]);
  const [problemsMap, setProblemsMap] = useState<Record<string, ProblemSummary>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedSubmission, setSelectedSubmission] = useState<SubmissionResponse | null>(null);
  const [isModalLoading, setIsModalLoading] = useState(false);

  const fetchSubmissions = async (pageNumber: number) => {
    setIsLoading(true);
    try {
      const res = await submissionService.getSubmissions({ page: pageNumber, size: 10 });
      setSubmissions(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);

      // Fetch problem titles for reference
      const probRes = await problemService.getProblems({ page: 0, size: 50 });
      const map: Record<string, ProblemSummary> = {};
      probRes.content?.forEach((p) => {
        map[p.id] = p;
      });
      setProblemsMap(map);
    } catch (err) {
      console.error('Failed to load submission history:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSubmissions(page);
  }, [page]);

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

  return (
    <div className="page-container submissions-page">
      <div className="page-header flex-between">
        <div>
          <h1 className="page-title flex-align-gap">
            <History size={26} className="text-accent" /> My Submission History
          </h1>
          <p className="page-subtitle">
            Track and review your submitted Java solutions and evaluation state.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => fetchSubmissions(page)}
          title="Refresh submissions"
        >
          <RefreshCw size={14} /> Refresh
        </button>
      </div>

      {isLoading ? (
        <div className="center-loader">
          <LoadingSpinner message="Loading your submission history..." />
        </div>
      ) : submissions.length === 0 ? (
        <div className="empty-state-card">
          <FileCode size={48} className="empty-icon text-muted" />
          <h2>No Submissions Yet</h2>
          <p>You haven't submitted any code solutions yet. Choose a problem to start practicing!</p>
          <Link to="/problems" className="btn btn-primary">
            Explore Problems Catalog
          </Link>
        </div>
      ) : (
        <div className="submissions-table-card">
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Submission ID</th>
                  <th>Problem</th>
                  <th>Language</th>
                  <th>Status</th>
                  <th>Submitted At</th>
                  <th className="text-right">Action</th>
                </tr>
              </thead>
              <tbody>
                {submissions.map((sub) => {
                  const prob = problemsMap[sub.problemId];
                  return (
                    <tr key={sub.id}>
                      <td className="font-mono text-muted">
                        {sub.id.substring(0, 8)}...
                      </td>
                      <td>
                        {prob ? (
                          <Link to={`/problems/${prob.slug}`} className="problem-table-title">
                            {prob.title}
                          </Link>
                        ) : (
                          <span className="text-muted">Problem ({sub.problemId.substring(0, 8)})</span>
                        )}
                      </td>
                      <td>
                        <span className="lang-pill">{sub.language}</span>
                      </td>
                      <td>
                        <StatusBadge status={sub.status} />
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
                          title="View submitted source code"
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

          {/* Pagination */}
          <div className="pagination-bar">
            <span className="pagination-info">
              Showing page {page + 1} of {totalPages} ({totalElements} total submissions)
            </span>
            <div className="pagination-actions">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                <ChevronLeft size={16} /> Previous
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Next <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Code Inspector Modal */}
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
