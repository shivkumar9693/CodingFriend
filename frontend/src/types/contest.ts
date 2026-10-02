export type ContestStatus = 'UPCOMING' | 'LIVE' | 'ENDED' | 'ARCHIVED';

export interface ContestProblem {
  problemId: string;
  title: string;
  slug: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  problemOrder: number;
  points: number;
}

export interface Contest {
  id: string;
  title: string;
  slug: string;
  description?: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  status: ContestStatus;
  participantCount: number;
  isRegistered: boolean;
  problems: ContestProblem[];
  createdAt: string;
}

export interface ContestParticipant {
  id: string;
  userId: string;
  fullName: string;
  avatarUrl?: string;
  rank?: number;
  totalScore: number;
  totalPenaltyMinutes: number;
  ratingBefore: number;
  ratingAfter: number;
  tabSwitchCount: number;
  registeredAt: string;
}

export interface ContestLeaderboard {
  contestId: string;
  title: string;
  status: string;
  totalParticipants: number;
  standings: ContestParticipant[];
}

export interface CreateContestPayload {
  title: string;
  description?: string;
  startTime: string;
  durationMinutes: number;
  problems: {
    problemId: string;
    problemOrder: number;
    points: number;
  }[];
}
