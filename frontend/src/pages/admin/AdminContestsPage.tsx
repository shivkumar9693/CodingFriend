import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Trophy,
  Plus,
  Calendar,
  Clock,
  Users,
  Search,
  Filter,
  CheckCircle2,
  Trash2,
  ExternalLink,
  ShieldCheck,
  Sparkles,
  AlertTriangle,
} from 'lucide-react';
import { contestService } from '../../services/contestService';
import { problemService } from '../../services/problemService';
import { Contest, ContestStatus } from '../../types/contest';
import { ProblemSummaryResponse } from '../../types/problem';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { Modal } from '../../components/Modal';
import { ErrorMessage } from '../../components/ErrorMessage';
import { ApiError } from '../../services/apiClient';

interface ProblemSetupRow {
  problemId: string;
  points: number;
}

export const AdminContestsPage: React.FC = () => {
  const [contests, setContests] = useState<Contest[]>([]);
  const [availableProblems, setAvailableProblems] = useState<ProblemSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Create Contest Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Form State
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [startDateTime, setStartDateTime] = useState('');
  const [durationMinutes, setDurationMinutes] = useState<number>(90);
  const [selectedProblems, setSelectedProblems] = useState<ProblemSetupRow[]>([
    { problemId: '', points: 100 },
  ]);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [contestsList, problemsRes] = await Promise.all([
        contestService.getAllContests(),
        problemService.getProblems({ page: 0, size: 100 }),
      ]);
      setContests(contestsList || []);
      setAvailableProblems(problemsRes.content || []);
    } catch (err) {
      console.error('Failed to load admin contest data:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openCreateModal = () => {
    setTitle('');
    setDescription('');
    // Default start time to 2 hours in the future formatted for datetime-local
    const defaultDate = new Date();
    defaultDate.setHours(defaultDate.getHours() + 2);
    defaultDate.setMinutes(0);
    defaultDate.setSeconds(0);
    const localIso = new Date(defaultDate.getTime() - defaultDate.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);
    setStartDateTime(localIso);
    setDurationMinutes(90);

    // Default first problem if available
    const initialProblemId = availableProblems.length > 0 ? availableProblems[0].id : '';
    setSelectedProblems([{ problemId: initialProblemId, points: 100 }]);
    setErrorMsg('');
    setSuccessMsg('');
    setIsModalOpen(true);
  };

  const handleAddProblemRow = () => {
    const nextUnused = availableProblems.find(
      (p) => !selectedProblems.some((sp) => sp.problemId === p.id)
    );
    const defaultPoints = (selectedProblems.length + 1) * 100;
    setSelectedProblems([
      ...selectedProblems,
      { problemId: nextUnused ? nextUnused.id : '', points: defaultPoints },
    ]);
  };

  const handleRemoveProblemRow = (index: number) => {
    if (selectedProblems.length <= 1) return;
    const updated = selectedProblems.filter((_, i) => i !== index);
    setSelectedProblems(updated);
  };

  const handleProblemChange = (index: number, field: 'problemId' | 'points', value: string | number) => {
    const updated = [...selectedProblems];
    if (field === 'problemId') {
      updated[index].problemId = String(value);
    } else {
      updated[index].points = Number(value);
    }
    setSelectedProblems(updated);
  };

  const handleCreateContest = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!title.trim()) {
      setErrorMsg('Contest title is required.');
      return;
    }

    if (!startDateTime) {
      setErrorMsg('Please specify a scheduled start date and time.');
      return;
    }

    const startDate = new Date(startDateTime);
    if (startDate.getTime() <= Date.now()) {
      setErrorMsg('Contest start time must be in the future.');
      return;
    }

    if (durationMinutes < 15) {
      setErrorMsg('Contest duration must be at least 15 minutes.');
      return;
    }

    const validProblems = selectedProblems.filter((p) => p.problemId.trim() !== '');
    if (validProblems.length === 0) {
      setErrorMsg('Please select at least one problem for the contest.');
      return;
    }

    // Check for duplicate problems
    const pIds = validProblems.map((p) => p.problemId);
    if (new Set(pIds).size !== pIds.length) {
      setErrorMsg('Duplicate problems detected. Each contest problem must be unique.');
      return;
    }

    setIsSaving(true);
    try {
      const payload = {
        title: title.trim(),
        description: description.trim() || undefined,
        startTime: startDate.toISOString(),
        durationMinutes: Number(durationMinutes),
        problems: validProblems.map((p, idx) => ({
          problemId: p.problemId,
          problemOrder: idx + 1,
          points: Number(p.points),
        })),
      };

      const created = await contestService.createContest(payload);
      setSuccessMsg(`Contest "${created.title}" successfully created and scheduled!`);
      setTimeout(() => {
        setIsModalOpen(false);
        loadData();
      }, 1200);
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to create contest. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  // Stats calculation
  const totalContestsCount = contests.length;
  const liveCount = contests.filter((c) => c.status === 'LIVE').length;
  const upcomingCount = contests.filter((c) => c.status === 'UPCOMING').length;
  const endedCount = contests.filter((c) => c.status === 'ENDED' || c.status === 'ARCHIVED').length;

  const filteredContests = contests.filter((c) => {
    const matchesSearch =
      c.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.slug.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (c.description && c.description.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesStatus = statusFilter === 'ALL' || c.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const getStatusBadge = (status: ContestStatus) => {
    switch (status) {
      case 'LIVE':
        return <span className="status-live animate-pulse">● LIVE NOW</span>;
      case 'UPCOMING':
        return <span className="status-upcoming">Upcoming</span>;
      case 'ENDED':
        return <span className="status-ended">Completed</span>;
      default:
        return <span className="status-archived">{status}</span>;
    }
  };

  return (
    <div className="page-container admin-contests-page">
      {/* Header */}
      <div className="admin-header">
        <div className="admin-badge">
          <ShieldCheck size={14} /> Competitive Arena Control
        </div>
        <h1 className="page-title">Contests & Championships Management</h1>
        <p className="page-subtitle">
          Schedule competitive programming rounds, curate weighted problem sets, track live participation, and oversee anti-cheat telemetry.
        </p>
      </div>

      {/* Metrics Row */}
      <div className="metrics-grid">
        <div className="metric-card">
          <div className="metric-icon-box bg-indigo">
            <Trophy size={24} />
          </div>
          <div className="metric-data">
            <span className="metric-value">{totalContestsCount}</span>
            <span className="metric-label">Total Arena Contests</span>
          </div>
        </div>

        <div className="metric-card">
          <div className="metric-icon-box bg-emerald">
            <Sparkles size={24} />
          </div>
          <div className="metric-data">
            <span className="metric-value">{liveCount}</span>
            <span className="metric-label">Live Active Rounds</span>
          </div>
        </div>

        <div className="metric-card">
          <div className="metric-icon-box bg-amber">
            <Calendar size={24} />
          </div>
          <div className="metric-data">
            <span className="metric-value">{upcomingCount}</span>
            <span className="metric-label">Scheduled Upcoming</span>
          </div>
        </div>

        <div className="metric-card">
          <div className="metric-icon-box bg-rose">
            <Users size={24} />
          </div>
          <div className="metric-data">
            <span className="metric-value">
              {contests.reduce((acc, c) => acc + (c.participantCount || 0), 0)}
            </span>
            <span className="metric-label">Total Competitors</span>
          </div>
        </div>
      </div>

      {/* Action Bar */}
      <div className="admin-actions-bar">
        <h2>Contest Schedule & Archives</h2>
        <div className="action-buttons-row">
          <button type="button" className="btn btn-primary" onClick={openCreateModal}>
            <Plus size={16} /> Schedule New Contest
          </button>
          <Link to="/contests" className="btn btn-secondary">
            <ExternalLink size={16} /> View Public Arena
          </Link>
        </div>
      </div>

      {/* Contests Table Section */}
      <div className="admin-section-card">
        {/* Search & Status Filters */}
        <div className="submission-filter-toolbar">
          <div className="toolbar-search-box">
            <Search size={14} className="search-prefix-icon" />
            <input
              type="text"
              placeholder="Search contests by title, slug, or details..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="toolbar-search-input"
            />
          </div>

          <div className="toolbar-chips">
            <Filter size={13} className="text-muted" />
            {[
              { id: 'ALL', label: `All (${totalContestsCount})` },
              { id: 'LIVE', label: `Live (${liveCount})` },
              { id: 'UPCOMING', label: `Upcoming (${upcomingCount})` },
              { id: 'ENDED', label: `Completed (${endedCount})` },
            ].map((st) => (
              <button
                key={st.id}
                type="button"
                className={`filter-chip ${statusFilter === st.id ? 'active' : ''}`}
                onClick={() => setStatusFilter(st.id)}
              >
                {st.label}
              </button>
            ))}
          </div>
        </div>

        {isLoading ? (
          <div className="center-loader py-8">
            <LoadingSpinner message="Loading competitive contests list..." />
          </div>
        ) : filteredContests.length === 0 ? (
          <div className="panel-empty-substate">
            <Trophy size={42} className="text-muted mx-auto mb-2" />
            <p>No competitive contests found matching your filter criteria.</p>
            <button type="button" className="btn btn-primary btn-sm mt-3" onClick={openCreateModal}>
              <Plus size={14} /> Schedule First Contest
            </button>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Contest Title</th>
                  <th>Status</th>
                  <th>Start Time</th>
                  <th>Duration</th>
                  <th>Curated Problems</th>
                  <th>Competitors</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredContests.map((contest) => (
                  <tr key={contest.id}>
                    <td>
                      <div className="contest-table-title-box">
                        <span className="font-bold text-white">{contest.title}</span>
                        <code className="text-xs text-muted font-mono">/{contest.slug}</code>
                      </div>
                    </td>
                    <td>{getStatusBadge(contest.status)}</td>
                    <td>
                      <div className="flex-align-gap text-sm text-secondary font-mono">
                        <Calendar size={13} className="text-muted" />
                        {new Date(contest.startTime).toLocaleString(undefined, {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </div>
                    </td>
                    <td>
                      <div className="flex-align-gap text-sm font-mono text-muted">
                        <Clock size={13} />
                        {contest.durationMinutes} mins
                      </div>
                    </td>
                    <td>
                      <div className="contest-problem-pills-row">
                        {contest.problems && contest.problems.length > 0 ? (
                          contest.problems.map((p, i) => (
                            <span
                              key={p.problemId || i}
                              className="contest-problem-pill"
                              title={`${p.title} (${p.points} pts)`}
                            >
                              P{p.problemOrder}: {p.points}pts
                            </span>
                          ))
                        ) : (
                          <span className="text-xs text-muted">0 Problems</span>
                        )}
                      </div>
                    </td>
                    <td>
                      <div className="flex-align-gap text-sm font-bold text-white">
                        <Users size={13} className="text-accent" />
                        {contest.participantCount || 0}
                      </div>
                    </td>
                    <td className="text-right">
                      <Link
                        to={`/contests/${contest.slug}`}
                        className="btn btn-secondary btn-sm"
                        title="View Public Contest & Standings"
                      >
                        <ExternalLink size={14} /> Open Arena
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Schedule Contest Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => !isSaving && setIsModalOpen(false)}
        title="Schedule Competitive Programming Contest"
        maxWidth="760px"
      >
        <form onSubmit={handleCreateContest} className="admin-contest-form">
          {errorMsg && <ErrorMessage message={errorMsg} onDismiss={() => setErrorMsg('')} />}

          {successMsg && (
            <div className="success-alert">
              <CheckCircle2 size={18} />
              <span>{successMsg}</span>
            </div>
          )}

          {/* Section 1: Title & Duration */}
          <div className="form-grid-2">
            <div className="form-group">
              <label htmlFor="contestTitle" className="form-label">
                Contest Title <span className="text-accent">*</span>
              </label>
              <input
                id="contestTitle"
                type="text"
                className="form-input"
                placeholder="e.g. Weekly Challenge Round 42"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                disabled={isSaving}
              />
            </div>

            <div className="form-group">
              <label htmlFor="durationMinutes" className="form-label">
                Duration (Minutes) <span className="text-accent">*</span>
              </label>
              <input
                id="durationMinutes"
                type="number"
                min={15}
                max={600}
                className="form-input"
                placeholder="90"
                value={durationMinutes}
                onChange={(e) => setDurationMinutes(Number(e.target.value))}
                required
                disabled={isSaving}
              />
            </div>
          </div>

          {/* Section 2: Start Date & Time */}
          <div className="form-group">
            <label htmlFor="startDateTime" className="form-label">
              Scheduled Start Date & Time <span className="text-accent">*</span>
            </label>
            <div className="input-with-icon">
              <Calendar size={16} className="input-prefix-icon" />
              <input
                id="startDateTime"
                type="datetime-local"
                className="form-input has-prefix"
                value={startDateTime}
                onChange={(e) => setStartDateTime(e.target.value)}
                required
                disabled={isSaving}
              />
            </div>
            <span className="text-xs text-muted mt-1">
              Contest start time must be in the future. Once reached, the contest becomes LIVE automatically.
            </span>
          </div>

          {/* Section 3: Description */}
          <div className="form-group">
            <label htmlFor="contestDesc" className="form-label">
              Contest Rules & Description (Optional)
            </label>
            <textarea
              id="contestDesc"
              className="form-textarea"
              rows={2}
              placeholder="Welcome to the Weekly Championship! 4 algorithmic challenges, ICPC penalties, and anti-cheat tab switch tracking enabled."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={isSaving}
            />
          </div>

          {/* Section 4: Curated Contest Problems */}
          <div className="form-group">
            <div className="label-row-between mb-2">
              <label className="form-label font-bold">
                Curated Contest Problems & Point Weights <span className="text-accent">*</span>
              </label>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={handleAddProblemRow}
                disabled={isSaving || availableProblems.length === 0}
              >
                <Plus size={14} /> Add Problem
              </button>
            </div>

            {availableProblems.length === 0 ? (
              <div className="error-alert">
                <AlertTriangle size={16} />
                <span>No coding problems available in the catalog. Please add problems first in Problem Management.</span>
              </div>
            ) : (
              <div className="contest-problem-picker-list">
                {selectedProblems.map((row, index) => (
                  <div key={index} className="contest-problem-picker-row">
                    <span className="problem-order-badge">#{index + 1}</span>

                    <div className="problem-picker-select-wrap">
                      <select
                        className="modal-select"
                        value={row.problemId}
                        onChange={(e) => handleProblemChange(index, 'problemId', e.target.value)}
                        required
                        disabled={isSaving}
                      >
                        <option value="" disabled>
                          Select a problem from catalog...
                        </option>
                        {availableProblems.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.title} ({p.difficulty})
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="problem-picker-points-wrap">
                      <input
                        type="number"
                        min={50}
                        step={50}
                        className="modal-input"
                        placeholder="Points"
                        value={row.points}
                        onChange={(e) => handleProblemChange(index, 'points', e.target.value)}
                        required
                        disabled={isSaving}
                        title="Score Points Value"
                      />
                      <span className="pts-label">pts</span>
                    </div>

                    {selectedProblems.length > 1 && (
                      <button
                        type="button"
                        className="btn-remove-row"
                        onClick={() => handleRemoveProblemRow(index)}
                        disabled={isSaving}
                        title="Remove problem from contest"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Modal Footer */}
          <div className="modal-footer" style={{ marginTop: '1.5rem' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsModalOpen(false)}
              disabled={isSaving}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSaving || availableProblems.length === 0}
            >
              {isSaving ? <LoadingSpinner size="sm" /> : <><Trophy size={16} /> Schedule Contest</>}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
