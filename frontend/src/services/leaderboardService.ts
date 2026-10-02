import { apiClient } from './apiClient';
import { LeaderboardEntry } from '../types/leaderboard';

class LeaderboardService {
  async getLeaderboard(): Promise<LeaderboardEntry[]> {
    try {
      const liveData = await apiClient.get<LeaderboardEntry[]>('/api/leaderboard');
      if (Array.isArray(liveData)) {
        return liveData;
      }
    } catch (err) {
      console.warn('Failed to fetch real leaderboard rankings from backend:', err);
    }
    return [];
  }
}

export const leaderboardService = new LeaderboardService();
