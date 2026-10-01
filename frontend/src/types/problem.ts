export type ProblemDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface TestCase {
  id?: string;
  inputData: string;
  expectedOutput: string;
  isHidden?: boolean;
  sequenceOrder: number;
  explanation?: string | null;
  metadata?: string | null;
}

export interface ProblemSummary {
  id: string;
  title: string;
  slug: string;
  difficulty: ProblemDifficulty;
  supportedLanguage: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface ProblemDetail {
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
  supportedLanguages?: string[];
  starterTemplates?: Record<string, string>;
  tags: string[];
  sampleTestCases: TestCase[];
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
}

export interface ProblemFilterParams {
  difficulty?: ProblemDifficulty;
  tag?: string;
  page?: number;
  size?: number;
}
