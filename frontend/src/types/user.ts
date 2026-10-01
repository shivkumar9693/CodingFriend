import { UserRole } from './auth';

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  bio?: string | null;
  githubUsername?: string | null;
  role: UserRole;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  bio?: string | null;
  githubUsername?: string | null;
}

export interface LearnerStatistics {
  totalSubmissions: number;
  totalAccepted: number;
  accuracyPercentage: number;
  problemsSolved: number;
  problemsAttempted: number;
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
