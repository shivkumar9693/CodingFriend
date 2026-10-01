export type HintLevel =
  | 'LEVEL_1_NUDGE'
  | 'LEVEL_2_CONCEPT'
  | 'LEVEL_3_ALGORITHM'
  | 'LEVEL_4_TARGETED';

export type MistakeClassification =
  | 'SYNTAX_ERROR'
  | 'OFF_BY_ONE'
  | 'NULL_POINTER'
  | 'INDEX_OUT_OF_BOUNDS'
  | 'INFINITE_LOOP'
  | 'TIME_LIMIT_EXCEEDED'
  | 'OUTPUT_FORMAT_MISMATCH'
  | 'LOGIC_INCORRECT'
  | 'EDGE_CASE_MISSED'
  | 'GENERAL_BUG';

export interface SocraBuddyHintRequest {
  problemId: string;
  submissionId?: string;
  sourceCode: string;
  hintLevel: HintLevel;
  userQuestion?: string;
  lastErrorMessage?: string;
}

export interface SocraBuddyHintResponse {
  hintLevel: HintLevel;
  title: string;
  guidance: string;
  socraticQuestion: string;
  conceptKeywords: string[];
  modelUsed: string;
}

export interface SocraBuddyDiagnosisRequest {
  problemId: string;
  submissionId?: string;
  sourceCode: string;
  verdict?: string;
  errorMessage?: string;
  failedTestNumber?: number;
}

export interface SocraBuddyDiagnosisResponse {
  mistakeClassification: MistakeClassification;
  diagnosisSummary: string;
  socraticGuidance: string;
  recommendedAction: string;
  recommendedHintLevel: HintLevel;
}
