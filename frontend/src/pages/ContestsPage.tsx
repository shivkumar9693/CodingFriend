import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Trophy,
  Calendar,
  Clock,
  Users,
  PlusCircle,
  CheckCircle2,
  AlertCircle,
  Play,
  X,
  Award,
  Swords,
  ChevronRight,
  Search,
} from 'lucide-react';
import { Contest, ContestLeaderboard, CreateContestPayload } from '../types/contest';
import { ProblemSummary } from '../types/problem';
import { contestService } from '../services/contestService';
import { problemService } from '../services/problemService';
import { useAuth } from '../context/AuthContext';

export const ContestsPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [contests, setContests] = useState<Contest[]>([]);
  const [activeTab, setActiveTab] = useState<'all' | 'live' | 'upcoming' | 'past'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [errorNotice, setErrorNotice] = useState<string | null>(null);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);

  // Leaderboard Modal State
  const [selectedLeaderboardSlug, setSelectedLeaderboardSlug] = useState<string | null>(null);
  const [leaderboardData, setLeaderboardData] = useState<ContestLeaderboard | null>(null);
  const [isLeaderboardLoading, setIsLeaderboardLoading] = useState(false);

  // Admin Create Contest Modal State
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [availableProblems, setAvailableProblems] = useState<ProblemSummary[]>([]);
  const [createTitle, setCreateTitle] = useState('');
  const [createDesc, setCreateDesc] = useState('');
  const [createStartTime, setCreateStartTime] = useState('');
  const [createDuration, setCreateDuration] = useState(90);
  const [selectedProblems, setSelectedProblems] = useState<{ problemId: string; problemOrder: number; points: number }[]>([]);
  const [isCreating, setIsCreating] = useState(false);

  const fetchContests = async () => {
    try {
      setIsLoading(true);
      const data = await contestService.getAllContests();
      setContests(data);
    } catch (err: any) {
      setErrorNotice(err.message || 'Failed to load contests list');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchContests();
  }, []);

  const openCreateModal = async () => {
    setShowCreateModal(true);
    try {
      const pRes = await problemService.getProblems({ size: 50 });
      setAvailableProblems(pRes.content);
      if (pRes.content.length > 0 && selectedProblems.length === 0) {
        setSelectedProblems([
          { problemId: pRes.content[0].id, problemOrder: 1, points: 500 },
          { problemId: pRes.content[1]?.id || pRes.content[0].id, problemOrder: 2, points: 1000 },
        ]);
      }
    } catch {
      // ignore
    }
  };

  const handleCreateContest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createTitle.trim() || !createStartTime || selectedProblems.length === 0) {
      setErrorNotice('Please provide title, start time, and at least one problem');
      return;
    }

    try {
      setIsCreating(true);
      const payload: CreateContestPayload = {
        title: createTitle.trim(),
        description: createDesc.trim(),
        startTime: new Date(createStartTime).toISOString(),
        durationMinutes: Number(createDuration),
        problems: selectedProblems,
      };

      await contestService.createContest(payload);
      setSuccessNotice('Contest created and scheduled successfully!');
      setShowCreateModal(false);
      setCreateTitle('');
      setCreateDesc('');
      fetchContests();
    } catch (err: any) {
      setErrorNotice(err.message || 'Failed to create contest');
    } finally {
      setIsCreating(false);
    }
  };

  const handleRegister = async (slug: string) => {
    try {
      await contestService.registerForContest(slug);
      setSuccessNotice('Registered successfully for the contest!');
      fetchContests();
    } catch (err: any) {
      setErrorNotice(err.message || 'Failed to register for contest');
    }
  };

  const viewLeaderboard = async (slug: string) => {
    setSelectedLeaderboardSlug(slug);
    setIsLeaderboardLoading(true);
    try {
      const data = await contestService.getLeaderboard(slug);
      setLeaderboardData(data);
    } catch (err: any) {
      setErrorNotice(err.message || 'Failed to load leaderboard');
    } finally {
      setIsLeaderboardLoading(false);
    }
  };

  const filteredContests = useMemo(() => {
    return contests.filter((c) => {
      if (activeTab === 'live' && c.status !== 'LIVE') return false;
      if (activeTab === 'upcoming' && c.status !== 'UPCOMING') return false;
      if (activeTab === 'past' && c.status !== 'ENDED') return false;
      if (searchQuery.trim() && !c.title.toLowerCase().includes(searchQuery.toLowerCase())) return false;
      return true;
    });
  }, [contests, activeTab, searchQuery]);

  return (
    <div className="contests-page-container">
      {/* Toast notifications */}
      {successNotice && (
        <div className="contest-toast success">
          <CheckCircle2 size={16} />
          <span>{successNotice}</span>
          <button type="button" onClick={() => setSuccessNotice(null)}>×</button>
        </div>
      )}
      {errorNotice && (
        <div className="contest-toast error">
          <AlertCircle size={16} />
          <span>{errorNotice}</span>
          <button type="button" onClick={() => setErrorNotice(null)}>×</button>
        </div>
      )}

      {/* Hero Header */}
      <div className="contests-hero">
        <div className="contests-hero-text">
          <div className="badge-pill-gold">
            <Trophy size={14} /> Competitive Programming Arena
          </div>
          <h1>Time-Bound Contests & Rankings</h1>
          <p>
            Compete in real-time coding contests, climb the global ELO ladder, and test your skills in proctored, anti-cheat isolation.
          </p>
        </div>

        <div className="contests-hero-actions">
          <button
            type="button"
            className="btn-duel-cta"
            onClick={() => navigate('/duels')}
          >
            <Swords size={16} />
            <span>1v1 Code Duels</span>
          </button>

          {isAdmin && (
            <button
              type="button"
              className="btn-create-contest-cta"
              onClick={openCreateModal}
            >
              <PlusCircle size={16} />
              <span>Host Contest (Admin)</span>
            </button>
          )}
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="contests-toolbar">
        <div className="contests-tabs-group">
          <button
            type="button"
            className={`contest-filter-tab ${activeTab === 'all' ? 'active' : ''}`}
            onClick={() => setActiveTab('all')}
          >
            All Contests ({contests.length})
          </button>
          <button
            type="button"
            className={`contest-filter-tab ${activeTab === 'live' ? 'active' : ''}`}
            onClick={() => setActiveTab('live')}
          >
            <span className="live-dot" />
            Live Now ({contests.filter((c) => c.status === 'LIVE').length})
          </button>
          <button
            type="button"
            className={`contest-filter-tab ${activeTab === 'upcoming' ? 'active' : ''}`}
            onClick={() => setActiveTab('upcoming')}
          >
            Upcoming ({contests.filter((c) => c.status === 'UPCOMING').length})
          </button>
          <button
            type="button"
            className={`contest-filter-tab ${activeTab === 'past' ? 'active' : ''}`}
            onClick={() => setActiveTab('past')}
          >
            Past Archive ({contests.filter((c) => c.status === 'ENDED').length})
          </button>
        </div>

        <div className="contests-search-box">
          <Search size={15} />
          <input
            type="text"
            placeholder="Search contests..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Contests Cards Grid */}
      {isLoading ? (
        <div className="contests-loading-grid">
          <div className="skeleton" style={{ height: '220px', borderRadius: '16px' }} />
          <div className="skeleton" style={{ height: '220px', borderRadius: '16px' }} />
        </div>
      ) : filteredContests.length === 0 ? (
        <div className="contests-empty-state">
          <Trophy size={48} className="text-muted" />
          <h3>No contests found in this category</h3>
          <p>Check back soon or create a contest if you are an administrator.</p>
        </div>
      ) : (
        <div className="contests-grid">
          {filteredContests.map((contest) => (
            <div key={contest.id} className={`contest-card status-${contest.status.toLowerCase()}`}>
              <div className="contest-card-header">
                <span className={`contest-status-badge badge-${contest.status.toLowerCase()}`}>
                  {contest.status === 'LIVE' && <span className="live-pulse" />}
                  {contest.status}
                </span>

                <span className="contest-participants-badge">
                  <Users size={13} />
                  <span>{contest.participantCount} registered</span>
                </span>
              </div>

              <h2 className="contest-card-title">{contest.title}</h2>
              <p className="contest-card-desc">
                {contest.description || 'Solve algorithm challenges against the clock with live ICPC scoring.'}
              </p>

              <div className="contest-card-meta">
                <div className="meta-col">
                  <Calendar size={14} className="text-muted" />
                  <span>{new Date(contest.startTime).toLocaleDateString()}</span>
                </div>
                <div className="meta-col">
                  <Clock size={14} className="text-muted" />
                  <span>{contest.durationMinutes} Mins</span>
                </div>
                <div className="meta-col">
                  <Award size={14} className="text-muted" />
                  <span>{contest.problems?.length || 0} Problems</span>
                </div>
              </div>

              <div className="contest-card-footer">
                {contest.status === 'LIVE' ? (
                  <button
                    type="button"
                    className="btn-enter-arena"
                    onClick={() => navigate(`/contests/${contest.slug}`)}
                  >
                    <Play size={14} />
                    <span>Enter Contest Arena</span>
                  </button>
                ) : contest.status === 'UPCOMING' ? (
                  contest.isRegistered ? (
                    <button type="button" className="btn-registered" disabled>
                      <CheckCircle2 size={14} />
                      <span>Registered</span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="btn-register"
                      onClick={() => handleRegister(contest.slug)}
                    >
                      <span>Register Now</span>
                      <ChevronRight size={14} />
                    </button>
                  )
                ) : (
                  <button
                    type="button"
                    className="btn-view-standings"
                    onClick={() => viewLeaderboard(contest.slug)}
                  >
                    <Trophy size={14} />
                    <span>View Standings</span>
                  </button>
                )}

                <button
                  type="button"
                  className="btn-subtle-standings"
                  onClick={() => viewLeaderboard(contest.slug)}
                  title="View Leaderboard"
                >
                  <Trophy size={15} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Leaderboard Modal */}
      {selectedLeaderboardSlug && (
        <div className="contest-modal-overlay">
          <div className="contest-leaderboard-modal">
            <div className="modal-header">
              <div className="modal-title-group">
                <Trophy size={20} className="text-yellow" />
                <h2>Contest Standings & Leaderboard</h2>
              </div>
              <button
                type="button"
                className="btn-modal-close"
                onClick={() => setSelectedLeaderboardSlug(null)}
              >
                <X size={18} />
              </button>
            </div>

            <div className="modal-body">
              {isLeaderboardLoading ? (
                <div className="skeleton" style={{ height: '240px', borderRadius: '12px' }} />
              ) : !leaderboardData || leaderboardData.standings.length === 0 ? (
                <div className="empty-leaderboard">
                  <p>No participant submissions recorded yet for this contest.</p>
                </div>
              ) : (
                <table className="contest-standings-table">
                  <thead>
                    <tr>
                      <th>Rank</th>
                      <th>Participant</th>
                      <th>Score</th>
                      <th>Penalty</th>
                      <th>Warnings</th>
                    </tr>
                  </thead>
                  <tbody>
                    {leaderboardData.standings.map((p, idx) => (
                      <tr key={p.id} className={idx < 3 ? `top-${idx + 1}` : ''}>
                        <td>
                          <span className={`rank-pill rank-${idx + 1}`}>#{idx + 1}</span>
                        </td>
                        <td className="participant-name-cell">
                          <strong>{p.fullName}</strong>
                        </td>
                        <td className="score-cell text-green">{p.totalScore} pts</td>
                        <td className="penalty-cell">{p.totalPenaltyMinutes}m</td>
                        <td>
                          {p.tabSwitchCount > 0 ? (
                            <span className="warning-pill">{p.tabSwitchCount} alerts</span>
                          ) : (
                            <span className="clean-pill">Clean</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Admin Create Contest Modal */}
      {showCreateModal && (
        <div className="contest-modal-overlay">
          <div className="contest-create-modal">
            <div className="modal-header">
              <div className="modal-title-group">
                <PlusCircle size={20} className="text-yellow" />
                <h2>Host & Schedule New Contest</h2>
              </div>
              <button
                type="button"
                className="btn-modal-close"
                onClick={() => setShowCreateModal(false)}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleCreateContest} className="create-contest-form">
              <div className="form-group">
                <label>Contest Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Weekly Algorithm Championship #1"
                  value={createTitle}
                  onChange={(e) => setCreateTitle(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label>Description & Rules</label>
                <textarea
                  rows={3}
                  placeholder="Rules, scoring guidelines, and topics..."
                  value={createDesc}
                  onChange={(e) => setCreateDesc(e.target.value)}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Start Date & Time</label>
                  <input
                    type="datetime-local"
                    required
                    value={createStartTime}
                    onChange={(e) => setCreateStartTime(e.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label>Duration (Minutes)</label>
                  <input
                    type="number"
                    min={15}
                    max={360}
                    value={createDuration}
                    onChange={(e) => setCreateDuration(Number(e.target.value))}
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Select Problems & Point Values</label>
                <div className="contest-problem-picker">
                  {selectedProblems.map((sp, idx) => (
                    <div key={idx} className="problem-picker-row">
                      <select
                        value={sp.problemId}
                        onChange={(e) => {
                          const updated = [...selectedProblems];
                          updated[idx].problemId = e.target.value;
                          setSelectedProblems(updated);
                        }}
                      >
                        {availableProblems.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.title} ({p.difficulty})
                          </option>
                        ))}
                      </select>
                      <input
                        type="number"
                        placeholder="Points"
                        value={sp.points}
                        onChange={(e) => {
                          const updated = [...selectedProblems];
                          updated[idx].points = Number(e.target.value);
                          setSelectedProblems(updated);
                        }}
                        style={{ width: '90px' }}
                      />
                      <button
                        type="button"
                        className="btn-remove-row"
                        onClick={() => setSelectedProblems(selectedProblems.filter((_, i) => i !== idx))}
                      >
                        ×
                      </button>
                    </div>
                  ))}

                  {availableProblems.length > 0 && (
                    <button
                      type="button"
                      className="btn-add-problem-row"
                      onClick={() =>
                        setSelectedProblems([
                          ...selectedProblems,
                          {
                            problemId: availableProblems[0].id,
                            problemOrder: selectedProblems.length + 1,
                            points: 500,
                          },
                        ])
                      }
                    >
                      + Add Another Problem
                    </button>
                  )}
                </div>
              </div>

              <div className="form-actions">
                <button
                  type="button"
                  className="btn-cancel"
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancel
                </button>
                <button type="submit" className="btn-submit-create" disabled={isCreating}>
                  {isCreating ? 'Publishing...' : 'Publish Contest'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
