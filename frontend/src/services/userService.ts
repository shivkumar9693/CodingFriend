import { apiClient } from './apiClient';
import { UpdateProfileRequest, UserProfile, LearnerStatistics, LearnerMistakeSummary } from '../types/user';

export const userService = {
  async getProfile(): Promise<UserProfile> {
    return apiClient.get<UserProfile>('/api/users/me');
  },

  async updateProfile(data: UpdateProfileRequest): Promise<UserProfile> {
    const updated = await apiClient.put<UserProfile>('/api/users/me', data);
    const existing = localStorage.getItem('user_data');
    if (existing) {
      try {
        const userObj = JSON.parse(existing);
        localStorage.setItem(
          'user_data',
          JSON.stringify({ ...userObj, fullName: updated.fullName, bio: updated.bio, githubUsername: updated.githubUsername })
        );
      } catch {
        // ignore
      }
    }
    return updated;
  },

  async getLearnerStatistics(): Promise<LearnerStatistics> {
    return apiClient.get<LearnerStatistics>('/api/users/me/statistics');
  },

  async getLearnerMistakes(): Promise<LearnerMistakeSummary[]> {
    return apiClient.get<LearnerMistakeSummary[]>('/api/users/me/mistakes');
  },
};
