import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  FlaskConical,
  Plus,
  Eye,
  EyeOff,
  Edit2,
  Trash2,
  CheckCircle2,
  RefreshCw,
} from 'lucide-react';
import { adminService } from '../../services/adminService';
import { problemService } from '../../services/problemService';
import { AdminTestCase, CreateTestCaseRequest, UpdateTestCaseRequest } from '../../types/admin';
import { ProblemDetail } from '../../types/problem';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { Modal } from '../../components/Modal';
import { ErrorMessage } from '../../components/ErrorMessage';

export const AdminTestCasesPage: React.FC = () => {
  const { problemId } = useParams<{ problemId: string }>();
  const [problem, setProblem] = useState<ProblemDetail | null>(null);
  const [testCases, setTestCases] = useState<AdminTestCase[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTestCaseId, setEditingTestCaseId] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Form fields
  const [inputData, setInputData] = useState('');
  const [expectedOutput, setExpectedOutput] = useState('');
  const [isHidden, setIsHidden] = useState(true);
  const [sequenceOrder, setSequenceOrder] = useState(1);
  const [explanation, setExplanation] = useState('');
  const [metadata, setMetadata] = useState('');

  const fetchProblemAndTestCases = async () => {
    if (!problemId) return;
    setIsLoading(true);
    try {
      const [probDetail, tcList] = await Promise.all([
        problemService.getProblemByIdOrSlug(problemId),
        adminService.getTestCases(problemId),
      ]);
      setProblem(probDetail);
      setTestCases(tcList || []);
    } catch (err) {
      console.error('Failed to load test cases:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProblemAndTestCases();
  }, [problemId]);

  const openCreateModal = () => {
    setEditingTestCaseId(null);
    setInputData('');
    setExpectedOutput('');
    setIsHidden(true);
    setSequenceOrder(testCases.length + 1);
    setExplanation('');
    setMetadata('');
    setErrorMsg('');
    setIsModalOpen(true);
  };

  const openEditModal = (tc: AdminTestCase) => {
    setEditingTestCaseId(tc.id);
    setInputData(tc.inputData);
    setExpectedOutput(tc.expectedOutput);
    setIsHidden(tc.isHidden);
    setSequenceOrder(tc.sequenceOrder);
    setExplanation(tc.explanation || '');
    setMetadata(tc.metadata || '');
    setErrorMsg('');
    setIsModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!problemId) return;
    setErrorMsg('');
    setSuccessMsg('');

    if (!inputData.trim() || !expectedOutput.trim()) {
      setErrorMsg('Input data and expected output are required.');
      return;
    }

    setIsSaving(true);
    try {
      if (editingTestCaseId) {
        const updatePayload: UpdateTestCaseRequest = {
          inputData: inputData.trim(),
          expectedOutput: expectedOutput.trim(),
          isHidden,
          sequenceOrder,
          explanation: explanation.trim() || null,
          metadata: metadata.trim() || null,
        };
        await adminService.updateTestCase(problemId, editingTestCaseId, updatePayload);
        setSuccessMsg('Test case updated successfully!');
      } else {
        const createPayload: CreateTestCaseRequest = {
          inputData: inputData.trim(),
          expectedOutput: expectedOutput.trim(),
          isHidden,
          sequenceOrder,
          explanation: explanation.trim() || null,
          metadata: metadata.trim() || null,
        };
        await adminService.createTestCase(problemId, createPayload);
        setSuccessMsg('Test case created successfully!');
      }

      setIsModalOpen(false);
      fetchProblemAndTestCases();
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Failed to save test case.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (testCaseId: string) => {
    if (!problemId) return;
    if (window.confirm('Are you sure you want to delete this test case?')) {
      try {
        await adminService.deleteTestCase(problemId, testCaseId);
        fetchProblemAndTestCases();
      } catch (err) {
        alert(err instanceof Error ? err.message : 'Failed to delete test case.');
      }
    }
  };

  if (isLoading) {
    return (
      <div className="page-container center-loader">
        <LoadingSpinner message="Loading test suite configuration..." />
      </div>
    );
  }

  return (
    <div className="page-container admin-testcases-page">
      <div className="problem-detail-nav">
        <Link to="/admin/problems" className="back-link">
          <ArrowLeft size={16} /> Back to Problems Management
        </Link>
      </div>

      <div className="page-header flex-between">
        <div>
          <div className="badge-wrapper">
            <span className="badge-outline">
              <FlaskConical size={14} /> Test Suite Manager
            </span>
          </div>
          <h1 className="page-title">
            Test Cases: {problem?.title || 'Coding Problem'}
          </h1>
          <p className="page-subtitle">
            Configure public sample test cases (visible to learners) and hidden test suites (used solely for grading).
          </p>
        </div>

        <div className="flex-align-gap">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={fetchProblemAndTestCases}
          >
            <RefreshCw size={14} /> Refresh
          </button>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={openCreateModal}
          >
            <Plus size={16} /> Add Test Case
          </button>
        </div>
      </div>

      {successMsg && (
        <div className="success-alert">
          <CheckCircle2 size={16} /> {successMsg}
        </div>
      )}

      {/* Test Cases List */}
      {testCases.length === 0 ? (
        <div className="empty-state-card">
          <FlaskConical size={48} className="empty-icon text-muted" />
          <h2>No Test Cases Configured</h2>
          <p>This problem has no test cases yet. Add at least one public and one hidden test case.</p>
          <button type="button" className="btn btn-primary" onClick={openCreateModal}>
            <Plus size={16} /> Add First Test Case
          </button>
        </div>
      ) : (
        <div className="admin-table-card">
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Visibility</th>
                  <th>Input Data</th>
                  <th>Expected Output</th>
                  <th>Explanation / Metadata</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {testCases.map((tc) => (
                  <tr key={tc.id}>
                    <td className="font-bold text-muted">{tc.sequenceOrder}</td>
                    <td>
                      {tc.isHidden ? (
                        <span className="badge-hidden">
                          <EyeOff size={13} /> Hidden Case
                        </span>
                      ) : (
                        <span className="badge-public">
                          <Eye size={13} /> Public Sample
                        </span>
                      )}
                    </td>
                    <td>
                      <pre className="inline-code-cell">{tc.inputData}</pre>
                    </td>
                    <td>
                      <pre className="inline-code-cell">{tc.expectedOutput}</pre>
                    </td>
                    <td className="text-sm text-secondary">
                      {tc.explanation || <span className="text-muted">None</span>}
                    </td>
                    <td className="text-right">
                      <div className="action-buttons-group">
                        <button
                          type="button"
                          className="btn-icon"
                          onClick={() => openEditModal(tc)}
                          title="Edit Test Case"
                        >
                          <Edit2 size={15} />
                        </button>
                        <button
                          type="button"
                          className="btn-icon btn-icon-danger"
                          onClick={() => handleDelete(tc.id)}
                          title="Delete Test Case"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add / Edit Test Case Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingTestCaseId ? 'Edit Test Case' : 'Add New Test Case'}
        maxWidth="650px"
      >
        <ErrorMessage message={errorMsg} onDismiss={() => setErrorMsg('')} />

        <form onSubmit={handleSave} className="admin-modal-form">
          <div className="form-grid-2">
            <div className="form-group">
              <label className="form-label">Sequence Order *</label>
              <input
                type="number"
                min="1"
                className="form-input"
                value={sequenceOrder}
                onChange={(e) => setSequenceOrder(parseInt(e.target.value) || 1)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Visibility *</label>
              <div className="checkbox-row mt-2">
                <label className="custom-checkbox-label">
                  <input
                    type="checkbox"
                    checked={isHidden}
                    onChange={(e) => setIsHidden(e.target.checked)}
                  />
                  <span>Is Hidden Test Case (For grading only)</span>
                </label>
              </div>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Input Data *</label>
            <textarea
              className="form-textarea font-mono"
              rows={3}
              placeholder="e.g. [2, 7, 11, 15]&#10;9"
              value={inputData}
              onChange={(e) => setInputData(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Expected Output *</label>
            <textarea
              className="form-textarea font-mono"
              rows={2}
              placeholder="e.g. [0, 1]"
              value={expectedOutput}
              onChange={(e) => setExpectedOutput(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Explanation (Optional)</label>
            <input
              type="text"
              className="form-input"
              placeholder="Explanation for public test cases"
              value={explanation}
              onChange={(e) => setExplanation(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Internal Metadata (Optional JSON)</label>
            <input
              type="text"
              className="form-input font-mono"
              placeholder='{"category": "edge_case", "timeout_ms": 1000}'
              value={metadata}
              onChange={(e) => setMetadata(e.target.value)}
            />
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsModalOpen(false)}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={isSaving}>
              {isSaving ? 'Saving...' : editingTestCaseId ? 'Save Changes' : 'Create Test Case'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
