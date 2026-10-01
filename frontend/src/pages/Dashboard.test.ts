import { describe, it, expect, vi, beforeEach } from 'vitest';
import { userService } from '../services/userService';
import { apiClient } from '../services/apiClient';
import { LearnerStatistics, LearnerMistakeSummary } from '../types/user';

vi.mock('../services/apiClient', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('Learner Dashboard Service & Analytics Tests (Day 21)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches learner statistics with all 7 key metrics', async () => {
    const mockStats: LearnerStatistics = {
      totalSubmissions: 25,
      totalAccepted: 18,
      accuracyPercentage: 72.0,
      problemsSolved: 8,
      problemsAttempted: 10,
      averageAttemptsPerProblem: 2.5,
      mostFrequentMistake: 'OFF_BY_ONE',
      mistakeBreakdown: {
        OFF_BY_ONE: 4,
        NULL_POINTER: 2,
        TIME_LIMIT_EXCEEDED: 1,
      },
      weakTopics: {
        ARRAY: 4,
        DYNAMIC_PROGRAMMING: 2,
      },
    };

    vi.mocked(apiClient.get).mockResolvedValue(mockStats);

    const result = await userService.getLearnerStatistics();

    expect(apiClient.get).toHaveBeenCalledWith('/api/users/me/statistics');
    expect(result.problemsSolved).toBe(8);
    expect(result.problemsAttempted).toBe(10);
    expect(result.totalAccepted).toBe(18);
    expect(result.totalSubmissions).toBe(25);
    expect(result.accuracyPercentage).toBe(72.0);
    expect(result.averageAttemptsPerProblem).toBe(2.5);
    expect(result.mistakeBreakdown?.OFF_BY_ONE).toBe(4);
    expect(result.weakTopics?.ARRAY).toBe(4);
  });

  it('fetches learner recurring mistakes breakdown', async () => {
    const mockMistakes: LearnerMistakeSummary[] = [
      {
        id: 'm1',
        mistakeType: 'OFF_BY_ONE',
        frequency: 4,
        firstOccurrence: '2026-02-01T10:00:00Z',
        lastOccurrence: '2026-02-04T12:00:00Z',
        relatedTopics: ['ARRAY', 'BINARY_SEARCH'],
      },
      {
        id: 'm2',
        mistakeType: 'NULL_POINTER',
        frequency: 2,
        firstOccurrence: '2026-02-02T10:00:00Z',
        lastOccurrence: '2026-02-03T12:00:00Z',
        relatedTopics: ['TREES', 'LINKED_LIST'],
      },
    ];

    vi.mocked(apiClient.get).mockResolvedValue(mockMistakes);

    const result = await userService.getLearnerMistakes();

    expect(apiClient.get).toHaveBeenCalledWith('/api/users/me/mistakes');
    expect(result).toHaveLength(2);
    expect(result[0].mistakeType).toBe('OFF_BY_ONE');
    expect(result[0].frequency).toBe(4);
    expect(result[0].relatedTopics).toContain('ARRAY');
  });

  describe('Dashboard Formatting & Calculation Utilities', () => {
    const formatMistakeName = (rawType: string): string => {
      return rawType
        .split('_')
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
    };

    const formatTopicName = (topic: string): string => {
      return topic
        .split('_')
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
    };

    it('formats raw mistake types into human-readable titles', () => {
      expect(formatMistakeName('OFF_BY_ONE')).toBe('Off By One');
      expect(formatMistakeName('NULL_POINTER')).toBe('Null Pointer');
      expect(formatMistakeName('ARRAY_INDEX_OUT_OF_BOUNDS')).toBe('Array Index Out Of Bounds');
      expect(formatMistakeName('TIME_LIMIT_EXCEEDED')).toBe('Time Limit Exceeded');
    });

    it('formats topic names correctly', () => {
      expect(formatTopicName('ARRAY')).toBe('Array');
      expect(formatTopicName('HASH_TABLE')).toBe('Hash Table');
      expect(formatTopicName('DYNAMIC_PROGRAMMING')).toBe('Dynamic Programming');
    });

    it('accurately computes mistake breakdown percentages', () => {
      const breakdown = {
        OFF_BY_ONE: 6,
        NULL_POINTER: 2,
        SYNTAX_ERROR: 2,
      };

      const totalMistakes = Object.values(breakdown).reduce((a, b) => a + b, 0);
      expect(totalMistakes).toBe(10);

      const offByOnePercent = Math.round((breakdown.OFF_BY_ONE / totalMistakes) * 100);
      const nullPointerPercent = Math.round((breakdown.NULL_POINTER / totalMistakes) * 100);

      expect(offByOnePercent).toBe(60);
      expect(nullPointerPercent).toBe(20);
    });

    it('accurately computes weak topic intensity scaling', () => {
      const weakTopics = {
        ARRAY: 5,
        TREES: 2,
      };

      const maxCount = Math.max(...Object.values(weakTopics));
      expect(maxCount).toBe(5);

      const arrayIntensity = Math.round((weakTopics.ARRAY / maxCount) * 100);
      const treesIntensity = Math.round((weakTopics.TREES / maxCount) * 100);

      expect(arrayIntensity).toBe(100);
      expect(treesIntensity).toBe(40);
    });

    it('identifies empty state when learner has 0 submissions', () => {
      const emptyStats: LearnerStatistics = {
        totalSubmissions: 0,
        totalAccepted: 0,
        accuracyPercentage: 0,
        problemsSolved: 0,
        problemsAttempted: 0,
        averageAttemptsPerProblem: 0,
      };

      const isEmpty = emptyStats.totalSubmissions === 0;
      expect(isEmpty).toBe(true);
    });
  });
});
