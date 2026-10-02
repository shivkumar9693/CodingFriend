export type MasteryStatus = 'TO_REVISE' | 'REVISED' | 'MASTERED' | 'ARCHIVED';

export type MistakeCategory =
  | 'LOGIC_ERROR'
  | 'OFF_BY_ONE'
  | 'CORNER_CASE'
  | 'TIME_LIMIT_EXCEEDED'
  | 'RUNTIME_EXCEPTION'
  | 'SYNTAX_ERROR'
  | 'INFINITE_LOOP'
  | 'MEMORY_LIMIT'
  | 'TYPE_MISMATCH'
  | 'RECURSION_BASE_CASE'
  | 'TWO_POINTER_INVARIANT'
  | 'INTEGER_OVERFLOW';

export interface PostMortemAnalysis {
  mistakeCategory: string;
  conceptSummary: string;
  failedInput: string;
  actualOutput: string;
  expectedOutput: string;
  studentFlowTrace: string;
  correctFlowTrace: string;
  keyTakeaway: string;
  relatedConcepts: string[];
}

export interface GeneratePostMortemRequest {
  problemId?: string;
  problemTitle?: string;
  language?: string;
  submittedCode: string;
  verdict?: string;
  failedInput?: string;
  actualOutput?: string;
  expectedOutput?: string;
  errorMessage?: string;
}

export interface CreateRevisionNoteRequest {
  problemId?: string;
  submissionId?: string;
  problemTitle: string;
  problemSlug?: string;
  language?: string;
  mistakeCategory: string;
  conceptSummary: string;
  failedInput?: string;
  actualOutput?: string;
  expectedOutput?: string;
  studentFlowTrace: string;
  correctFlowTrace: string;
  keyTakeaway: string;
  submittedCode?: string;
  userReflections?: string;
  isBookmarked?: boolean;
}

export interface UpdateRevisionNoteRequest {
  userReflections?: string;
  masteryStatus?: MasteryStatus;
  isBookmarked?: boolean;
}

export interface RevisionNote {
  id: string;
  userId: string;
  problemId?: string;
  submissionId?: string;
  problemTitle: string;
  problemSlug?: string;
  language: string;
  mistakeCategory: MistakeCategory;
  conceptSummary: string;
  failedInput?: string;
  actualOutput?: string;
  expectedOutput?: string;
  studentFlowTrace: string;
  correctFlowTrace: string;
  keyTakeaway: string;
  submittedCode?: string;
  userReflections?: string;
  masteryStatus: MasteryStatus;
  isBookmarked: boolean;
  reviewCount: number;
  lastReviewedAt?: string;
  nextReviewAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RevisionNotesSummary {
  totalNotes: number;
  toReviseCount: number;
  revisedCount: number;
  masteredCount: number;
  bookmarkedCount: number;
  dueTodayCount: number;
  categoryBreakdown: Record<string, number>;
}
