export type SubmissionStatus = 'QUEUED' | 'RUNNING' | 'EVALUATING' | 'COMPLETED' | 'FAILED';

export type EvaluationVerdict =
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'COMPILE_ERROR'
  | 'RUNTIME_ERROR'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED';

export interface TestCaseEvaluationResult {
  testNumber: number;
  isHidden: boolean;
  passed: boolean;
  verdict: EvaluationVerdict;
  input?: string | null;
  expectedOutput?: string | null;
  actualOutput?: string | null;
  executionTimeMs: number;
  errorMessage?: string | null;
}

export interface CreateSubmissionRequest {
  problemId: string;
  sourceCode: string;
  language?: string;
}

export interface SubmissionResponse {
  id: string;
  userId: string;
  problemId: string;
  language: string;
  sourceCode: string;
  status: SubmissionStatus;
  verdict?: EvaluationVerdict | null;
  testsPassed: number;
  totalTests: number;
  executionTimeMs: number;
  evaluationDetails?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SubmissionSummaryResponse {
  id: string;
  userId: string;
  problemId: string;
  language: string;
  status: SubmissionStatus;
  verdict?: EvaluationVerdict | null;
  testsPassed?: number;
  totalTests?: number;
  executionTimeMs?: number;
  createdAt: string;
  updatedAt: string;
}

export interface SubmissionHintRequest {
  level: number;
}

export interface SubmissionHintResponse {
  mistakeType: string;
  hintLevel: number;
  hint: string;
  concept?: string;
  revealsSolution: boolean;
}

