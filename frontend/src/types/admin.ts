import { ProblemDifficulty } from './problem';

export interface AdminTestCase {
  id: string;
  inputData: string;
  expectedOutput: string;
  isHidden: boolean;
  sequenceOrder: number;
  explanation?: string | null;
  metadata?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminProblemDetail {
  id: string;
  title: string;
  slug: string;
  description: string;
  difficulty: ProblemDifficulty;
  constraints?: string | null;
  inputFormat?: string | null;
  outputFormat?: string | null;
  examples?: string | null;
  starterCode?: string | null;
  supportedLanguage: string;
  tags: string[];
  testCases: AdminTestCase[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateProblemRequest {
  title: string;
  slug: string;
  description: string;
  difficulty: ProblemDifficulty;
  constraints?: string | null;
  inputFormat?: string | null;
  outputFormat?: string | null;
  examples?: string | null;
  starterCode?: string | null;
  supportedLanguage?: string;
  tags?: string[];
  testCases?: CreateTestCaseRequest[];
}

export interface UpdateProblemRequest {
  title: string;
  slug: string;
  description: string;
  difficulty: ProblemDifficulty;
  constraints?: string | null;
  inputFormat?: string | null;
  outputFormat?: string | null;
  examples?: string | null;
  starterCode?: string | null;
  supportedLanguage?: string;
  tags?: string[];
}

export interface CreateTestCaseRequest {
  inputData: string;
  expectedOutput: string;
  isHidden?: boolean;
  sequenceOrder?: number;
  explanation?: string | null;
  metadata?: string | null;
}

export interface UpdateTestCaseRequest {
  inputData: string;
  expectedOutput: string;
  isHidden: boolean;
  sequenceOrder: number;
  explanation?: string | null;
  metadata?: string | null;
}
