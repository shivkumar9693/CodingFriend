export type DuelStatus = 'WAITING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface Duel {
  id: string;
  roomCode: string;
  player1Id: string;
  player1Name: string;
  player1Rating: number;
  player2Id?: string;
  player2Name?: string;
  player2Rating?: number;
  problemId?: string;
  problemTitle?: string;
  problemSlug?: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  status: DuelStatus;
  winnerId?: string;
  winnerName?: string;
  startTime?: string;
  endTime?: string;
  player1Progress: number;
  player2Progress: number;
  createdAt: string;
}

export interface CreateDuelPayload {
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  problemId?: string;
}

export interface ActivityHeatmapData {
  currentStreak: number;
  maxStreak: number;
  totalActiveDays: number;
  totalSubmissions: number;
  totalProblemsSolved: number;
  dailyActivities: {
    date: string;
    count: number;
    solvedCount: number;
  }[];
}
