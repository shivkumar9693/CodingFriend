import { describe, it, expect, vi, beforeEach } from 'vitest';
import { leaderboardService } from './leaderboardService';
import { apiClient } from './apiClient';

describe('leaderboardService', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('should fetch real leaderboard rankings from backend API', async () => {
    const mockEntries = [
      {
        rank: 1,
        userId: 'u-101',
        fullName: 'Real Leader A',
        email: 'leader.a@example.com',
        avatarUrl: '',
        tier: 'Grandmaster' as const,
        rating: 2900,
        solvedCount: 50,
        solvedEasy: 25,
        solvedMedium: 20,
        solvedHard: 5,
        accuracy: 95.0,
        streakDays: 14,
        badge: '💎 SocraTitan',
      },
      {
        rank: 2,
        userId: 'u-102',
        fullName: 'Real Leader B',
        email: 'leader.b@example.com',
        avatarUrl: '',
        tier: 'Diamond' as const,
        rating: 2600,
        solvedCount: 40,
        solvedEasy: 20,
        solvedMedium: 15,
        solvedHard: 5,
        accuracy: 90.0,
        streakDays: 10,
        badge: '🔷 Code Architect',
      },
    ];

    vi.spyOn(apiClient, 'get').mockResolvedValueOnce(mockEntries);

    const entries = await leaderboardService.getLeaderboard();
    expect(entries.length).toBe(2);
    expect(entries[0].fullName).toBe('Real Leader A');
    expect(entries[0].rank).toBe(1);
    expect(entries[1].rank).toBe(2);
  });
});
