export type RankTier = 'Grandmaster' | 'Diamond' | 'Platinum' | 'Gold' | 'Silver' | 'Bronze';

export interface LeaderboardEntry {
  rank: number;
  userId: string;
  fullName: string;
  email: string;
  avatarUrl?: string;
  tier: RankTier;
  rating: number;
  solvedCount: number;
  solvedEasy: number;
  solvedMedium: number;
  solvedHard: number;
  accuracy: number;
  streakDays: number;
  badge: string;
}
