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
  Search,
  X,
  Tag,
  SlidersHorizontal,
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
  const [searchQuery, setSearchQuery] = useState('');
  const [difficultyFilter, setDifficultyFilter] = useState<string>('ALL');

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

  const [selectedTag, setSelectedTag] = useState<string | null>(null);

  // Stats calculation
  const totalCount = problems.length;
  const easyCount = problems.filter((p) => p.difficulty === 'EASY').length;
  const mediumCount = problems.filter((p) => p.difficulty === 'MEDIUM').length;
  const hardCount = problems.filter((p) => p.difficulty === 'HARD').length;

  const filteredProblems = problems.filter((p) => {
    const query = searchQuery.toLowerCase().trim();
    const matchesSearch =
      !query ||
      p.title.toLowerCase().includes(query) ||
      p.slug.toLowerCase().includes(query) ||
      (p.tags && p.tags.some((t) => t.toLowerCase().includes(query)));

    const matchesDiff = difficultyFilter === 'ALL' || p.difficulty === difficultyFilter;
    const matchesTag = !selectedTag || (p.tags && p.tags.includes(selectedTag));
    return matchesSearch && matchesDiff && matchesTag;
  });

  const hasActiveFilters = Boolean(searchQuery.trim() || difficultyFilter !== 'ALL' || selectedTag);

  const resetAllFilters = () => {
    setSearchQuery('');
    setDifficultyFilter('ALL');
    setSelectedTag(null);
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

      <div className="admin-table-card">
        {/* Enhanced Search & Filter Toolbar */}
        <div className="admin-problem-toolbar-enhanced">
          <div className="toolbar-top-row">
            <div className="toolbar-search-box-enhanced">
              <Search size={15} className="search-prefix-icon" />
              <input
                type="text"
                placeholder="Search by title, slug, or concept tag (e.g. array, dp)..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="toolbar-search-input-enhanced"
              />
              {searchQuery && (
                <button
                  type="button"
                  className="btn-clear-search"
                  onClick={() => setSearchQuery('')}
                  title="Clear search"
                >
                  <X size={14} />
                </button>
              )}
            </div>

            <div className="toolbar-chips-group">
              <div className="chips-label">
                <SlidersHorizontal size={13} />
                <span>Difficulty:</span>
              </div>
              
              <button
                type="button"
                className={`filter-chip-pill chip-all ${difficultyFilter === 'ALL' ? 'active' : ''}`}
                onClick={() => setDifficultyFilter('ALL')}
              >
                <span>All</span>
                <span className="chip-badge-count">{totalCount}</span>
              </button>

              <button
                type="button"
                className={`filter-chip-pill chip-easy ${difficultyFilter === 'EASY' ? 'active' : ''}`}
                onClick={() => setDifficultyFilter('EASY')}
              >
                <span className="chip-dot dot-easy" />
                <span>Easy</span>
                <span className="chip-badge-count">{easyCount}</span>
              </button>

              <button
                type="button"
                className={`filter-chip-pill chip-medium ${difficultyFilter === 'MEDIUM' ? 'active' : ''}`}
                onClick={() => setDifficultyFilter('MEDIUM')}
              >
                <span className="chip-dot dot-medium" />
                <span>Medium</span>
                <span className="chip-badge-count">{mediumCount}</span>
              </button>

              <button
                type="button"
                className={`filter-chip-pill chip-hard ${difficultyFilter === 'HARD' ? 'active' : ''}`}
                onClick={() => setDifficultyFilter('HARD')}
              >
                <span className="chip-dot dot-hard" />
                <span>Hard</span>
                <span className="chip-badge-count">{hardCount}</span>
              </button>
            </div>
          </div>

          {/* Active Status & Filter Summary Bar */}
          <div className="toolbar-summary-bar">
            <div className="summary-left-info">
              <span className="text-sm font-semibold">
                Showing <strong className="text-accent">{filteredProblems.length}</strong> of{' '}
                <strong>{totalCount}</strong> problems
              </span>

              {selectedTag && (
                <span className="active-filter-tag-pill">
                  <Tag size={12} />
                  <span>Tag: {selectedTag}</span>
                  <button
                    type="button"
                    className="btn-remove-tag-filter"
                    onClick={() => setSelectedTag(null)}
                    title="Remove tag filter"
                  >
                    <X size={12} />
                  </button>
                </span>
              )}
            </div>

            {hasActiveFilters && (
              <button
                type="button"
                className="btn-reset-filters-link"
                onClick={resetAllFilters}
              >
                Reset All Filters
              </button>
            )}
          </div>
        </div>

        {isLoading ? (
          <div className="center-loader py-8">
            <LoadingSpinner message="Loading problems catalog..." />
          </div>
        ) : filteredProblems.length === 0 ? (
          <div className="panel-empty-substate py-10">
            <Layers size={42} className="text-muted mx-auto mb-2" />
            <h3 className="text-base font-bold text-white mb-1">No matching problems found</h3>
            <p className="text-sm text-muted">
              {hasActiveFilters
                ? 'Try adjusting your search keywords or resetting your difficulty/tag filters.'
                : 'Your catalog is currently empty. Click below to add the first challenge.'}
            </p>
            <div className="flex-align-gap justify-center mt-4">
              {hasActiveFilters && (
                <button type="button" className="btn btn-secondary btn-sm" onClick={resetAllFilters}>
                  Reset Filters
                </button>
              )}
              <button type="button" className="btn btn-primary btn-sm" onClick={openCreateModal}>
                <Plus size={14} /> Create Problem
              </button>
            </div>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Title & Slug</th>
                  <th>Difficulty</th>
                  <th>Language</th>
                  <th>Concept Tags</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredProblems.map((prob) => (
                  <tr key={prob.id}>
                    <td>
                      <div className="problem-cell-title">
                        <button
                          type="button"
                          className="problem-title-btn"
                          onClick={() => openEditModal(prob)}
                          title="Click to edit problem details"
                        >
                          {prob.title}
                        </button>
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
                        {prob.tags && prob.tags.length > 0 ? (
                          prob.tags.map((t) => (
                            <button
                              key={t}
                              type="button"
                              className={`tag-chip-sm tag-chip-clickable ${selectedTag === t ? 'tag-chip-active' : ''}`}
                              onClick={() => setSelectedTag(selectedTag === t ? null : t)}
                              title={`Filter by tag: ${t}`}
                            >
                              {t}
                            </button>
                          ))
                        ) : (
                          <span className="text-xs text-muted">No tags</span>
                        )}
                      </div>
                    </td>
                    <td className="text-right">
                      <div className="action-buttons-group">
                        <Link
                          to={`/admin/problems/${prob.id}/test-cases`}
                          className="btn-action-testcases"
                          title="Manage Public & Hidden Test Cases"
                        >
                          <FlaskConical size={14} /> Test Cases
                        </Link>
                        <button
                          type="button"
                          className="btn-action-edit"
                          onClick={() => openEditModal(prob)}
                          title="Edit Problem Details"
                        >
                          <Edit2 size={13} /> Edit
                        </button>
                        <button
                          type="button"
                          className="btn-action-delete"
                          onClick={() => handleDelete(prob.id, prob.title)}
                          title="Delete Problem"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

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
