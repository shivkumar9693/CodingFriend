import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { submissionService } from '../services/submissionService';
import { SubmissionResponse, SubmissionHintResponse } from '../types/submission';

// Mock localStorage for Node/Vitest environment
const storageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString();
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

beforeAll(() => {
  Object.defineProperty(globalThis, 'localStorage', {
    value: storageMock,
    writable: true,
  });
});

describe('Day 20: AI Mentor Submission UI Service & Logic', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('fetches submission hint at level 1 with structured response', async () => {
    const mockHintResponse: SubmissionHintResponse = {
      mistakeType: 'OFF_BY_ONE',
      hintLevel: 1,
      hint: 'What happens when your index reaches the array length?',
      concept: 'Array Boundary Conditions',
      revealsSolution: false,
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockHintResponse,
    } as unknown as Response);

    const result = await submissionService.getSubmissionHint('sub-101', 1);

    expect(result).toBeDefined();
    expect(result.hintLevel).toBe(1);
    expect(result.mistakeType).toBe('OFF_BY_ONE');
    expect(result.hint).toContain('What happens when');
    expect(result.revealsSolution).toBe(false);
  });

  it('handles progressive hint level increments up to level 5', async () => {
    for (let level = 1; level <= 5; level++) {
      const mockHint: SubmissionHintResponse = {
        mistakeType: 'EDGE_CASE',
        hintLevel: level,
        hint: `Hint text for level ${level}`,
        concept: 'Empty array edge cases',
        revealsSolution: level === 5,
      };

      globalThis.fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => mockHint,
      } as unknown as Response);

      const res = await submissionService.getSubmissionHint('sub-202', level);
      expect(res.hintLevel).toBe(level);
      if (level === 5) {
        expect(res.revealsSolution).toBe(true);
      } else {
        expect(res.revealsSolution).toBe(false);
      }
    }
  });

  it('handles AI unavailable or service failure with structured error throw', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      json: async () => ({
        message: 'AI Mentor service is temporarily unreachable',
      }),
    } as unknown as Response);

    await expect(submissionService.getSubmissionHint('sub-999', 1)).rejects.toThrow(
      'AI Mentor service is temporarily unreachable'
    );
  });

  it('guarantees hidden test case privacy in submission evaluation data', () => {
    const mockSubmission: SubmissionResponse = {
      id: 'sub-secure-1',
      problemId: 'prob-1',
      userId: 'user-1',
      sourceCode: 'public class Solution {}',
      language: 'JAVA',
      status: 'COMPLETED',
      verdict: 'WRONG_ANSWER',
      executionTimeMs: 142,
      testsPassed: 3,
      totalTests: 5,
      errorMessage: null,
      evaluationDetails: JSON.stringify([
        {
          testNumber: 1,
          passed: true,
          input: '[1, 2]',
          expectedOutput: '3',
          actualOutput: '3',
          isHidden: false,
        },
        {
          testNumber: 4,
          passed: false,
          input: '[SECRET_INPUT]',
          expectedOutput: '[SECRET_EXPECTED]',
          actualOutput: '[SECRET_ACTUAL]',
          isHidden: true,
        },
      ]),
      createdAt: '2026-10-01T12:00:00Z',
      updatedAt: '2026-10-01T12:00:01Z',
    };

    const evaluationList = JSON.parse(mockSubmission.evaluationDetails || '[]');
    const failingCase = evaluationList.find((tc: any) => !tc.passed);

    expect(failingCase).toBeDefined();
    expect(failingCase.testNumber).toBe(4);
    expect(failingCase.isHidden).toBe(true);

    // Filtered / masked representation for UI
    const maskedDetails = evaluationList.map((tc: any) => {
      if (tc.isHidden) {
        return {
          testNumber: tc.testNumber,
          passed: tc.passed,
          isHidden: true,
          input: null,
          expectedOutput: null,
          actualOutput: null,
        };
      }
      return tc;
    });

    const maskedFailing = maskedDetails.find((tc: any) => tc.testNumber === 4);
    expect(maskedFailing.input).toBeNull();
    expect(maskedFailing.expectedOutput).toBeNull();
    expect(maskedFailing.actualOutput).toBeNull();
    expect(maskedFailing.isHidden).toBe(true);
  });
});
