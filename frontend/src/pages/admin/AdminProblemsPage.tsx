import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Layers,
  Plus,
  Edit2,
  Trash2,
  FlaskConical,
  RefreshCw,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { problemService } from '../../services/problemService';
import { adminService } from '../../services/adminService';
import { ProblemSummary, ProblemDifficulty } from '../../types/problem';
import { CreateProblemRequest, UpdateProblemRequest } from '../../types/admin';
import { DifficultyBadge } from '../../components/DifficultyBadge';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { Modal } from '../../components/Modal';
import { ErrorMessage } from '../../components/ErrorMessage';

const DEFAULT_STARTER = `class Solution {
    // Write your solution here
}
`;

export const AdminProblemsPage: React.FC = () => {
  const [problems, setProblems] = useState<ProblemSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProblemId, setEditingProblemId] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Form fields
  const [title, setTitle] = useState('');
  const [slug, setSlug] = useState('');
  const [difficulty, setDifficulty] = useState<ProblemDifficulty>('EASY');
  const [description, setDescription] = useState('');
  const [constraints, setConstraints] = useState('');
  const [inputFormat, setInputFormat] = useState('');
  const [outputFormat, setOutputFormat] = useState('');
  const [examples, setExamples] = useState('');
  const [starterCode, setStarterCode] = useState(DEFAULT_STARTER);
  const [tagsInput, setTagsInput] = useState('ARRAY, HASH_TABLE');

  const fetchProblems = async (pageNumber: number) => {
    setIsLoading(true);
    try {
      const res = await problemService.getProblems({ page: pageNumber, size: 20 });
      setProblems(res.content || []);
      setTotalPages(res.totalPages || 1);
    } catch (err) {
      console.error('Failed to load problems:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProblems(page);
  }, [page]);

  const openCreateModal = () => {
    setEditingProblemId(null);
    setTitle('');
    setSlug('');
    setDifficulty('EASY');
    setDescription('');
    setConstraints('');
    setInputFormat('');
    setOutputFormat('');
    setExamples('');
    setStarterCode(DEFAULT_STARTER);
    setTagsInput('');
    setErrorMsg('');
    setIsModalOpen(true);
  };

  const openEditModal = async (probSummary: ProblemSummary) => {
    setEditingProblemId(probSummary.id);
    setErrorMsg('');
    setIsSaving(true);
    setIsModalOpen(true);
    try {
      const detail = await problemService.getProblemByIdOrSlug(probSummary.id);
      setTitle(detail.title);
      setSlug(detail.slug);
      setDifficulty(detail.difficulty);
      setDescription(detail.description);
      setConstraints(detail.constraints || '');
      setInputFormat(detail.inputFormat || '');
      setOutputFormat(detail.outputFormat || '');
      setExamples(detail.examples || '');
      setStarterCode(detail.starterCode || DEFAULT_STARTER);
      setTagsInput(detail.tags ? detail.tags.join(', ') : '');
    } catch (err) {
      setErrorMsg('Failed to load problem details for editing.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleTitleChange = (val: string) => {
    setTitle(val);
    if (!editingProblemId) {
      // Auto-generate slug
      const generatedSlug = val
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/(^-|-$)+/g, '');
      setSlug(generatedSlug);
    }
  };

  const handleSaveProblem = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!title.trim() || !slug.trim() || !description.trim()) {
      setErrorMsg('Title, slug, and description are mandatory.');
      return;
    }

    const parsedTags = tagsInput
      .split(',')
      .map((t) => t.trim().toUpperCase())
      .filter((t) => t.length > 0);

    setIsSaving(true);
    try {
      if (editingProblemId) {
        const updatePayload: UpdateProblemRequest = {
          title: title.trim(),
          slug: slug.trim(),
          description: description.trim(),
          difficulty,
          constraints: constraints.trim() || null,
          inputFormat: inputFormat.trim() || null,
          outputFormat: outputFormat.trim() || null,
          examples: examples.trim() || null,
          starterCode: starterCode.trim() || null,
          supportedLanguage: 'JAVA',
          tags: parsedTags,
        };
        await adminService.updateProblem(editingProblemId, updatePayload);
        setSuccessMsg('Problem updated successfully!');
      } else {
        const createPayload: CreateProblemRequest = {
          title: title.trim(),
          slug: slug.trim(),
          description: description.trim(),
          difficulty,
          constraints: constraints.trim() || null,
          inputFormat: inputFormat.trim() || null,
          outputFormat: outputFormat.trim() || null,
          examples: examples.trim() || null,
          starterCode: starterCode.trim() || null,
          supportedLanguage: 'JAVA',
          tags: parsedTags,
        };
        await adminService.createProblem(createPayload);
        setSuccessMsg('New problem created successfully!');
      }

      setIsModalOpen(false);
      fetchProblems(page);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Failed to save problem.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (id: string, problemTitle: string) => {
    if (window.confirm(`Are you sure you want to delete problem "${problemTitle}"? This will also remove all associated test cases.`)) {
      try {
        await adminService.deleteProblem(id);
        fetchProblems(page);
      } catch (err) {
        alert(err instanceof Error ? err.message : 'Failed to delete problem.');
      }
    }
  };

  return (
    <div className="page-container admin-problems-page">
      <div className="page-header flex-between">
        <div>
          <h1 className="page-title flex-align-gap">
            <Layers size={26} className="text-accent" /> Problem Catalog Management
          </h1>
          <p className="page-subtitle">
            Create, update, and configure algorithmic challenges and starter templates.
          </p>
        </div>
        <div className="flex-align-gap">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchProblems(page)}
          >
            <RefreshCw size={14} /> Refresh
          </button>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={openCreateModal}
          >
            <Plus size={16} /> Create Problem
          </button>
        </div>
      </div>

      {successMsg && (
        <div className="success-alert">
          <CheckCircle2 size={16} /> {successMsg}
        </div>
      )}

      {isLoading ? (
        <div className="center-loader">
          <LoadingSpinner message="Loading problems catalog..." />
        </div>
      ) : (
        <div className="admin-table-card">
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Title & Slug</th>
                  <th>Difficulty</th>
                  <th>Language</th>
                  <th>Tags</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {problems.map((prob) => (
                  <tr key={prob.id}>
                    <td>
                      <div className="problem-cell-title">
                        <Link to={`/problems/${prob.slug}`} className="font-bold text-accent">
                          {prob.title}
                        </Link>
                        <span className="slug-text">/{prob.slug}</span>
                      </div>
                    </td>
                    <td>
                      <DifficultyBadge difficulty={prob.difficulty} />
                    </td>
                    <td>
                      <span className="lang-pill">{prob.supportedLanguage}</span>
                    </td>
                    <td>
                      <div className="tags-container-sm">
                        {prob.tags?.map((t) => (
                          <span key={t} className="tag-chip-sm">
                            {t}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="text-right">
                      <div className="action-buttons-group">
                        <Link
                          to={`/admin/problems/${prob.id}/test-cases`}
                          className="btn-action-testcases"
                          title="Manage Public & Hidden Test Cases"
                        >
                          <FlaskConical size={15} /> Test Cases
                        </Link>
                        <button
                          type="button"
                          className="btn-icon"
                          onClick={() => openEditModal(prob)}
                          title="Edit Problem"
                        >
                          <Edit2 size={15} />
                        </button>
                        <button
                          type="button"
                          className="btn-icon btn-icon-danger"
                          onClick={() => handleDelete(prob.id, prob.title)}
                          title="Delete Problem"
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

          <div className="pagination-bar">
            <span className="pagination-info">
              Showing page {page + 1} of {totalPages}
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

      {/* Create / Edit Problem Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingProblemId ? 'Edit Coding Problem' : 'Create New Problem'}
        maxWidth="800px"
      >
        <ErrorMessage message={errorMsg} onDismiss={() => setErrorMsg('')} />

        <form onSubmit={handleSaveProblem} className="admin-modal-form">
          <div className="form-grid-2">
            <div className="form-group">
              <label className="form-label">Problem Title *</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Reverse Linked List"
                value={title}
                onChange={(e) => handleTitleChange(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Slug * (URL Identifier)</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. reverse-linked-list"
                value={slug}
                onChange={(e) => setSlug(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-group">
              <label className="form-label">Difficulty *</label>
              <select
                className="form-input"
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value as ProblemDifficulty)}
              >
                <option value="EASY">EASY</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HARD">HARD</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Concept Tags (Comma Separated)</label>
              <input
                type="text"
                className="form-input"
                placeholder="ARRAY, TWO_POINTERS, DYNAMIC_PROGRAMMING"
                value={tagsInput}
                onChange={(e) => setTagsInput(e.target.value)}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Problem Description * (Markdown Supported)</label>
            <textarea
              className="form-textarea"
              rows={5}
              placeholder="Detailed algorithmic problem statement..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Examples</label>
            <textarea
              className="form-textarea"
              rows={3}
              placeholder="Example 1:&#10;Input: nums = [2,7,11,15], target = 9&#10;Output: [0,1]"
              value={examples}
              onChange={(e) => setExamples(e.target.value)}
            />
          </div>

          <div className="form-grid-2">
            <div className="form-group">
              <label className="form-label">Constraints</label>
              <textarea
                className="form-textarea"
                rows={3}
                placeholder="1 <= nums.length <= 10^4&#10;-10^9 <= nums[i] <= 10^9"
                value={constraints}
                onChange={(e) => setConstraints(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Input / Output Formats</label>
              <input
                type="text"
                className="form-input mb-2"
                placeholder="Input format description"
                value={inputFormat}
                onChange={(e) => setInputFormat(e.target.value)}
              />
              <input
                type="text"
                className="form-input"
                placeholder="Output format description"
                value={outputFormat}
                onChange={(e) => setOutputFormat(e.target.value)}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Java Starter Code Template</label>
            <textarea
              className="form-textarea font-mono"
              rows={6}
              value={starterCode}
              onChange={(e) => setStarterCode(e.target.value)}
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
              {isSaving ? 'Saving...' : editingProblemId ? 'Save Changes' : 'Create Problem'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
