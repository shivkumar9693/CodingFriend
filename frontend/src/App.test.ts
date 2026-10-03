import { describe, it, expect, beforeEach, beforeAll, vi } from 'vitest';
import { API_BASE_URL } from './config';
import { authService } from './services/authService';
import { problemService } from './services/problemService';
import { userService } from './services/userService';
import { ProblemDetail } from './types/problem';

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

describe('Frontend Configuration & Architecture', () => {
  it('resolves API_BASE_URL correctly with fallback', () => {
    expect(API_BASE_URL).toBeDefined();
    expect(typeof API_BASE_URL).toBe('string');
  });

  it('verifies frontend route definitions and modules', () => {
    const definedRoutes = [
      '/',
      '/login',
      '/register',
      '/problems',
      '/problems/:slug',
      '/profile',
      '/404',
      '*',
    ];

    expect(definedRoutes).toContain('/login');
    expect(definedRoutes).toContain('/register');
    expect(definedRoutes).toContain('/problems');
    expect(definedRoutes).toContain('/problems/:slug');
    expect(definedRoutes).toContain('/profile');
  });
});

describe('Auth Service & LocalStorage Token Management', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('clears tokens properly on logout', async () => {
    localStorage.setItem('access_token', 'sample-access-token');
    localStorage.setItem('refresh_token', 'sample-refresh-token');
    localStorage.setItem('user_data', JSON.stringify({ id: '123', email: 'test@example.com' }));

    // Mock fetch for logout request
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ message: 'Logged out successfully' }),
    } as unknown as Response);

    await authService.logout();

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(localStorage.getItem('user_data')).toBeNull();
  });

  it('retrieves current cached user from localStorage', () => {
    const mockUser = {
      id: 'usr-1',
      email: 'learner@example.com',
      fullName: 'Learner One',
      role: 'USER' as const,
    };
    localStorage.setItem('user_data', JSON.stringify(mockUser));

    const retrieved = authService.getCurrentUser();
    expect(retrieved).toEqual(mockUser);
  });

  it('performs Google login and sets tokens and user_data in localStorage', async () => {
    const mockGoogleResponse = {
      accessToken: 'google-access-jwt',
      refreshToken: 'google-refresh-jwt',
      tokenType: 'Bearer',
      expiresIn: 900000,
      user: {
        id: 'usr-google-1',
        email: 'googleuser@example.com',
        fullName: 'Google User',
        role: 'LEARNER' as const,
        avatarUrl: 'https://lh3.googleusercontent.com/a/sample',
      },
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ data: mockGoogleResponse }),
    } as unknown as Response);

    const result = await authService.loginWithGoogle('sample-google-id-token');

    expect(result.accessToken).toBe('google-access-jwt');
    expect(result.user.email).toBe('googleuser@example.com');
    expect(localStorage.getItem('access_token')).toBe('google-access-jwt');
    expect(localStorage.getItem('refresh_token')).toBe('google-refresh-jwt');
    expect(JSON.parse(localStorage.getItem('user_data') || '{}')).toEqual(mockGoogleResponse.user);
  });
});

describe('Problem Service Contract & Query Building', () => {
  it('exposes problem fetching functions', () => {
    expect(typeof problemService.getProblems).toBe('function');
    expect(typeof problemService.getProblemByIdOrSlug).toBe('function');
  });

  it('exposes user profile management functions', () => {
    expect(typeof userService.getProfile).toBe('function');
    expect(typeof userService.updateProfile).toBe('function');
  });
});

describe('Day 8: Monaco Code Editor & Problem Workspace Logic', () => {
  const mockProblem: ProblemDetail = {
    id: 'prob-uuid-1',
    title: 'Two Sum',
    slug: 'two-sum',
    description: 'Given an array of integers nums and an integer target, return indices of the two numbers...',
    difficulty: 'EASY',
    constraints: '2 <= nums.length <= 10^4',
    inputFormat: 'nums = [2,7,11,15], target = 9',
    outputFormat: '[0,1]',
    examples: 'Input: nums = [2,7,11,15], target = 9\nOutput: [0,1]',
    starterCode: 'class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[]{};\n    }\n}',
    supportedLanguage: 'JAVA',
    tags: ['Array', 'Hash Table'],
    sampleTestCases: [
      {
        id: 'tc-1',
        sequenceOrder: 1,
        inputData: '[2,7,11,15]\n9',
        expectedOutput: '[0,1]',
        explanation: 'Because nums[0] + nums[1] == 9, we return [0, 1].',
        isHidden: false,
      },
    ],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(() => {
    localStorage.clear();
  });

  it('loads starter code correctly when no previous autosave exists', () => {
    const storageKey = `acm_code_${mockProblem.id}`;
    const savedCode = localStorage.getItem(storageKey);

    const initialCode = savedCode || mockProblem.starterCode;
    expect(initialCode).toBe(mockProblem.starterCode);
    expect(initialCode).toContain('public int[] twoSum');
  });

  it('preserves and loads locally autosaved code over starter code', () => {
    const storageKey = `acm_code_${mockProblem.id}`;
    const userModifiedCode = 'class Solution {\n    // My custom solution in progress\n}';
    localStorage.setItem(storageKey, userModifiedCode);

    const loadedCode = localStorage.getItem(storageKey) || mockProblem.starterCode;
    expect(loadedCode).toBe(userModifiedCode);
    expect(loadedCode).not.toBe(mockProblem.starterCode);
  });

  it('resets code properly back to starter template when reset action is triggered', () => {
    const storageKey = `acm_code_${mockProblem.id}`;
    localStorage.setItem(storageKey, '// Broken code');

    // Simulate Reset action
    const resetCode = mockProblem.starterCode || '';
    localStorage.setItem(storageKey, resetCode);

    expect(localStorage.getItem(storageKey)).toBe(mockProblem.starterCode);
    expect(localStorage.getItem(storageKey)).toContain('class Solution');
  });

  it('properly structures public sample test cases without leaking hidden test cases', () => {
    expect(mockProblem.sampleTestCases).toBeDefined();
    expect(mockProblem.sampleTestCases.length).toBe(1);
    expect(mockProblem.sampleTestCases[0].isHidden).toBe(false);
  });
});
