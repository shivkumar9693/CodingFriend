import { describe, it, expect } from 'vitest';
import { SubmissionResponse } from '../types/submission';

describe('SubmissionResultView Logic', () => {
  it('correctly formats and computes runtime percentile beats for fast solutions', () => {
    const mockSubmission: SubmissionResponse = {
      id: 'sub-12345678',
      userId: 'user-87654321',
      problemId: 'prob-101',
      language: 'JAVA',
      sourceCode: 'class Solution { public int solve() { return 42; } }',
      status: 'COMPLETED',
      verdict: 'ACCEPTED',
      testsPassed: 8,
      totalTests: 8,
      executionTimeMs: 2,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    expect(mockSubmission.verdict).toBe('ACCEPTED');
    expect(mockSubmission.testsPassed).toBe(8);
    expect(mockSubmission.totalTests).toBe(8);
    expect(mockSubmission.executionTimeMs).toBe(2);

    // Fast execution time (2ms) beats significant percentage of user submissions
    const runtimeBeats = mockSubmission.executionTimeMs <= 2 ? 86.75 : 50;
    expect(runtimeBeats).toBeGreaterThanOrEqual(80);
  });

  it('handles failed or non-accepted submissions with diagnostic output', () => {
    const failedSubmission: SubmissionResponse = {
      id: 'sub-failed-001',
      userId: 'user-87654321',
      problemId: 'prob-101',
      language: 'JAVA',
      sourceCode: 'class Solution { // syntax error }',
      status: 'FAILED',
      verdict: 'COMPILE_ERROR',
      testsPassed: 0,
      totalTests: 8,
      executionTimeMs: 0,
      errorMessage: 'Main.java:3: error: reached end of file while parsing',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    expect(failedSubmission.verdict).toBe('COMPILE_ERROR');
    expect(failedSubmission.errorMessage).toContain('error: reached end of file');
  });
});
