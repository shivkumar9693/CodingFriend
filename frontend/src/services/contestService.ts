import { apiClient } from './apiClient';
import { Contest, ContestLeaderboard, ContestParticipant, CreateContestPayload } from '../types/contest';

export const contestService = {
  async getAllContests(): Promise<Contest[]> {
    return apiClient.get<Contest[]>('/api/contests');
  },

  async getContestBySlug(slug: string): Promise<Contest> {
    return apiClient.get<Contest>(`/api/contests/${encodeURIComponent(slug)}`);
  },

  async createContest(payload: CreateContestPayload): Promise<Contest> {
    return apiClient.post<Contest>('/api/contests', payload);
  },

  async registerForContest(slug: string): Promise<ContestParticipant> {
    return apiClient.post<ContestParticipant>(`/api/contests/${encodeURIComponent(slug)}/register`);
  },

  async getLeaderboard(slug: string): Promise<ContestLeaderboard> {
    return apiClient.get<ContestLeaderboard>(`/api/contests/${encodeURIComponent(slug)}/leaderboard`);
  },

  async reportTabSwitch(slug: string): Promise<void> {
    return apiClient.post<void>(`/api/contests/${encodeURIComponent(slug)}/tab-switch`);
  },
};
