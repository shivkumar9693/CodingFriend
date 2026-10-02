import { apiClient } from './apiClient';
import { CreateDuelPayload, Duel } from '../types/duel';

export const duelService = {
  async queueOrStartDuel(payload: CreateDuelPayload): Promise<Duel> {
    return apiClient.post<Duel>('/api/duels/queue', payload);
  },

  async joinDuelRoom(roomCode: string): Promise<Duel> {
    return apiClient.post<Duel>('/api/duels/join', { roomCode });
  },

  async getDuelRoom(roomCode: string): Promise<Duel> {
    return apiClient.get<Duel>(`/api/duels/room/${encodeURIComponent(roomCode)}`);
  },

  async updateProgress(roomCode: string, progressPercent: number): Promise<Duel> {
    return apiClient.post<Duel>(`/api/duels/room/${encodeURIComponent(roomCode)}/progress`, { progressPercent });
  },

  async declareComplete(roomCode: string): Promise<Duel> {
    return apiClient.post<Duel>(`/api/duels/room/${encodeURIComponent(roomCode)}/complete`);
  },

  async leaveDuelRoom(roomCode: string): Promise<void> {
    return apiClient.post<void>(`/api/duels/room/${encodeURIComponent(roomCode)}/leave`);
  },

  async getDuelHistory(): Promise<Duel[]> {
    return apiClient.get<Duel[]>('/api/duels/history');
  },
};
