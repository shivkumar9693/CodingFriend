import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Trophy, Medal, Crown, Flame, Search, Star, X, Code2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { leaderboardService } from '../services/leaderboardService';
import { LeaderboardEntry, RankTier } from '../types/leaderboard';
import { LoadingSpinner } from '../components/LoadingSpinner';

export const LeaderboardPage: React.FC = () => {
  const { user } = useAuth();
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTier, setSelectedTier] = useState<string>('ALL');

  useEffect(() => {
    const fetchLeaderboard = async () => {
      setIsLoading(true);
      try {
        const data = await leaderboardService.getLeaderboard();
        setEntries(data);
      } catch (err) {
        console.error('Failed to fetch leaderboard:', err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchLeaderboard();
  }, [user]);

  const filteredEntries = entries.filter((entry) => {
    const matchesSearch =
      entry.fullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      entry.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (entry.badge && entry.badge.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesTier = selectedTier === 'ALL' || entry.tier.toUpperCase() === selectedTier.toUpperCase();
    return matchesSearch && matchesTier;
  });

  const top3 = entries.slice(0, 3);

  const getTierClass = (tier: RankTier) => {
    switch (tier) {
      case 'Grandmaster':
        return 'tier-grandmaster';
      case 'Diamond':
        return 'tier-diamond';
      case 'Platinum':
        return 'tier-platinum';
      case 'Gold':
        return 'tier-gold';
      case 'Silver':
        return 'tier-silver';
      default:
        return 'tier-bronze';
    }
  };

  const getRankMedal = (rank: number) => {
    if (rank === 1) return <Crown className="rank-crown text-gold" size={22} />;
    if (rank === 2) return <Medal className="rank-medal text-silver" size={20} />;
    if (rank === 3) return <Medal className="rank-medal text-bronze" size={20} />;
    return <span className="rank-num">#{rank}</span>;
  };

  const getAvatar = (entry: LeaderboardEntry) => {
    if (entry.avatarUrl && entry.avatarUrl.trim()) {
      return entry.avatarUrl;
    }
    if (user && (entry.userId === user.id || entry.email === user.email)) {
      const local = localStorage.getItem(`avatar_${user.id}`);
      if (local && local.trim()) return local;
    }
    return null;
  };

  if (isLoading) {
    return (
      <div className="page-container center-loader">
        <LoadingSpinner message="Calculating global student rankings..." />
      </div>
    );
  }

  return (
    <div className="page-container leaderboard-page">
      <div className="page-header text-center">
        <div className="hero-badge mx-auto">
          <Trophy size={14} className="text-yellow" />
          <span>Global Socratic Hall of Fame</span>
        </div>
        <h1 className="page-title">Learner Rankings & Leaderboard</h1>
        <p className="page-subtitle">
          Celebrate mastery, continuous problem-solving streaks, and algorithmic excellence.
        </p>
      </div>

      {/* Top 3 Podium Cards */}
      {top3.length >= 3 && (
        <div className="podium-grid">
          {/* Rank 2 */}
          <div className="podium-card podium-rank-2">
            <div className="podium-crown-badge">
              <Medal size={28} className="text-silver" />
              <span>#2 Rank</span>
            </div>
            <div className="podium-avatar-wrapper">
              {getAvatar(top3[1]) ? (
                <img src={getAvatar(top3[1])!} alt={top3[1].fullName} className="podium-avatar" />
              ) : (
                <div className="podium-avatar podium-avatar-fallback">
                  {top3[1].fullName ? top3[1].fullName.charAt(0).toUpperCase() : 'U'}
                </div>
              )}
              <span className={`podium-tier-tag ${getTierClass(top3[1].tier)}`}>{top3[1].tier}</span>
            </div>
            <h3 className="podium-name">{top3[1].fullName}</h3>
            <div className="podium-rating">
              <Star size={14} className="text-yellow" /> {top3[1].rating} ELO
            </div>
            <div className="podium-stats">
              <span>{top3[1].solvedCount} Solved</span>
              <span>•</span>
              <span className="text-flame"><Flame size={13} /> {top3[1].streakDays}d Streak</span>
            </div>
          </div>

          {/* Rank 1 */}
          <div className="podium-card podium-rank-1">
            <div className="champion-glow"></div>
            <div className="podium-crown-badge">
              <Crown size={32} className="text-gold animate-bounce" />
              <span>#1 Grandmaster</span>
            </div>
            <div className="podium-avatar-wrapper rank-1-avatar">
              {getAvatar(top3[0]) ? (
                <img src={getAvatar(top3[0])!} alt={top3[0].fullName} className="podium-avatar" />
              ) : (
                <div className="podium-avatar podium-avatar-fallback champion-avatar-fallback">
                  {top3[0].fullName ? top3[0].fullName.charAt(0).toUpperCase() : 'U'}
                </div>
              )}
              <span className={`podium-tier-tag ${getTierClass(top3[0].tier)}`}>{top3[0].tier}</span>
            </div>
            <h3 className="podium-name champion-name">{top3[0].fullName}</h3>
            <div className="podium-badge-label">{top3[0].badge}</div>
            <div className="podium-rating champion-rating">
              <Star size={16} className="text-gold" /> {top3[0].rating} ELO
            </div>
            <div className="podium-stats">
              <span>{top3[0].solvedCount} Solved</span>
              <span>•</span>
              <span className="text-flame"><Flame size={14} /> {top3[0].streakDays}d Streak</span>
              <span>•</span>
              <span>{top3[0].accuracy}% Acc</span>
            </div>
          </div>

          {/* Rank 3 */}
          <div className="podium-card podium-rank-3">
            <div className="podium-crown-badge">
              <Medal size={28} className="text-bronze" />
              <span>#3 Rank</span>
            </div>
            <div className="podium-avatar-wrapper">
              {getAvatar(top3[2]) ? (
                <img src={getAvatar(top3[2])!} alt={top3[2].fullName} className="podium-avatar" />
              ) : (
                <div className="podium-avatar podium-avatar-fallback">
                  {top3[2].fullName ? top3[2].fullName.charAt(0).toUpperCase() : 'U'}
                </div>
              )}
              <span className={`podium-tier-tag ${getTierClass(top3[2].tier)}`}>{top3[2].tier}</span>
            </div>
            <h3 className="podium-name">{top3[2].fullName}</h3>
            <div className="podium-rating">
              <Star size={14} className="text-yellow" /> {top3[2].rating} ELO
            </div>
            <div className="podium-stats">
              <span>{top3[2].solvedCount} Solved</span>
              <span>•</span>
              <span className="text-flame"><Flame size={13} /> {top3[2].streakDays}d Streak</span>
            </div>
          </div>
        </div>
      )}

      {/* Filter and Search Bar Toolbar */}
      <div className="leaderboard-toolbar">
        <div className="leaderboard-search-wrapper">
          <Search size={18} className="leaderboard-search-icon" />
          <input
            type="text"
            className="form-input leaderboard-search-input"
            placeholder="Search learners by name or badge..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button
              type="button"
              className="leaderboard-search-clear"
              onClick={() => setSearchQuery('')}
              title="Clear search"
            >
              <X size={14} />
            </button>
          )}
        </div>

        <div className="tier-filter-chips">
          {['ALL', 'Grandmaster', 'Diamond', 'Platinum', 'Gold', 'Silver', 'Bronze'].map((tier) => (
            <button
              key={tier}
              className={`tier-chip ${selectedTier.toUpperCase() === tier.toUpperCase() ? 'active' : ''}`}
              onClick={() => setSelectedTier(tier)}
            >
              {tier}
            </button>
          ))}
        </div>
      </div>

      {/* Leaderboard Table Card */}
      <div className="leaderboard-table-card">
        {filteredEntries.length === 0 ? (
          <div className="empty-leaderboard-state">
            <div className="empty-leaderboard-icon">
              <Trophy size={48} className="text-yellow" />
            </div>
            <h3 className="empty-leaderboard-title">No Learners Found</h3>
            <p className="empty-leaderboard-desc">
              {searchQuery || selectedTier !== 'ALL'
                ? `No ranking records match the filter criteria "${searchQuery || selectedTier}".`
                : 'Solve your first Java challenge to start earning ELO skill points and rise up the global rankings!'}
            </p>
            <div className="empty-leaderboard-actions">
              {searchQuery || selectedTier !== 'ALL' ? (
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => {
                    setSearchQuery('');
                    setSelectedTier('ALL');
                  }}
                >
                  Reset Filters
                </button>
              ) : (
                <Link to="/problems" className="btn btn-primary btn-sm">
                  <Code2 size={16} /> Explore Java Challenges
                </Link>
              )}
            </div>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="custom-table leaderboard-table">
              <thead>
                <tr>
                  <th style={{ width: '80px' }}>Rank</th>
                  <th>Learner</th>
                  <th>Tier</th>
                  <th>Skill Rating</th>
                  <th>Problems Solved</th>
                  <th>Accuracy</th>
                  <th>Active Streak</th>
                  <th>Mastery Badge</th>
                </tr>
              </thead>
              <tbody>
                {filteredEntries.map((entry) => {
                  const isCurrentUser = user && (entry.userId === user.id || entry.email === user.email);
                  return (
                    <tr
                      key={entry.userId || entry.email}
                      className={`leaderboard-row ${isCurrentUser ? 'current-user-row' : ''}`}
                    >
                      <td className="rank-cell">{getRankMedal(entry.rank)}</td>
                      <td>
                        <div className="learner-meta-cell">
                          {getAvatar(entry) ? (
                            <img src={getAvatar(entry)!} alt={entry.fullName} className="learner-avatar-sm" />
                          ) : (
                            <div className="learner-avatar-fallback">
                              {entry.fullName ? entry.fullName.charAt(0).toUpperCase() : 'U'}
                            </div>
                          )}
                          <div>
                            <div className="learner-name-row">
                              <span className="learner-name">{entry.fullName}</span>
                              {isCurrentUser && <span className="you-pill">You</span>}
                            </div>
                            <span className="learner-sub-email">{entry.email}</span>
                          </div>
                        </div>
                      </td>
                      <td>
                        <span className={`tier-badge ${getTierClass(entry.tier)}`}>
                          {entry.tier}
                        </span>
                      </td>
                      <td>
                        <span className="rating-value">{entry.rating}</span>
                        <span className="rating-pts"> pts</span>
                      </td>
                      <td>
                        <div className="solved-breakdown-cell">
                          <span className="solved-total">{entry.solvedCount}</span>
                          <div className="solved-mini-bar">
                            <div
                              className="mini-bar-easy"
                              style={{ width: `${(entry.solvedEasy / (entry.solvedCount || 1)) * 100}%` }}
                              title={`Easy: ${entry.solvedEasy}`}
                            />
                            <div
                              className="mini-bar-med"
                              style={{ width: `${(entry.solvedMedium / (entry.solvedCount || 1)) * 100}%` }}
                              title={`Medium: ${entry.solvedMedium}`}
                            />
                            <div
                              className="mini-bar-hard"
                              style={{ width: `${(entry.solvedHard / (entry.solvedCount || 1)) * 100}%` }}
                              title={`Hard: ${entry.solvedHard}`}
                            />
                          </div>
                        </div>
                      </td>
                      <td>
                        <span className="accuracy-cell">{entry.accuracy}%</span>
                      </td>
                      <td>
                        <div className="streak-badge">
                          <Flame size={14} className="text-flame" />
                          <span>{entry.streakDays}d</span>
                        </div>
                      </td>
                      <td>
                        <span className="mastery-badge-tag">{entry.badge}</span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
