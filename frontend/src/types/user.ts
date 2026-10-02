import { UserRole } from './auth';

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  bio?: string | null;
  githubUsername?: string | null;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  linkedinUrl?: string | null;
  contestRating?: number;
  duelRating?: number;
  currentStreak?: number;
  maxStreak?: number;
  role: UserRole;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  bio?: string | null;
  githubUsername?: string | null;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  linkedinUrl?: string | null;
}

export interface LearnerStatistics {
  totalSubmissions: number;
  totalAccepted: number;
  accuracyPercentage: number;
  problemsSolved: number;
  problemsAttempted: number;
  easySolved?: number;
  totalEasy?: number;
  mediumSolved?: number;
  totalMedium?: number;
  hardSolved?: number;
  totalHard?: number;
  totalProblems?: number;
  contestsAttended?: number;
  averageAttemptsPerProblem: number;
  mostFrequentMistake?: string | null;
  mistakeBreakdown?: Record<string, number>;
  weakTopics?: Record<string, number>;
}

export interface LearnerMistakeSummary {
  id: string;
  mistakeType: string;
  frequency: number;
  firstOccurrence: string;
  lastOccurrence: string;
  relatedTopics: string[];
}
