import { apiClient } from './apiClient';
import { PageResponse, ProblemDetail, ProblemFilterParams, ProblemSummary } from '../types/problem';

// In-Memory Cache with TTL for ultra-fast LeetCode-like page transitions
const problemDetailCache = new Map<string, { data: ProblemDetail; timestamp: number }>();
const problemListCache = new Map<string, { data: PageResponse<ProblemSummary>; timestamp: number }>();
const CACHE_TTL_MS = 3 * 60 * 1000; // 3 minutes fresh cache

export const problemService = {
  async getProblems(params: ProblemFilterParams = {}, forceRefresh = false): Promise<PageResponse<ProblemSummary>> {
    const searchParams = new URLSearchParams();

    if (params.difficulty) {
      searchParams.set('difficulty', params.difficulty);
    }
    if (params.tag) {
      searchParams.set('tag', params.tag);
    }
    if (params.page !== undefined) {
      searchParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      searchParams.set('size', params.size.toString());
    }

    const queryStr = searchParams.toString();
    const endpoint = `/api/problems${queryStr ? `?${queryStr}` : ''}`;
    const cacheKey = endpoint;

    const cached = problemListCache.get(cacheKey);
    if (!forceRefresh && cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
      return cached.data;
    }

    const response = await apiClient.get<PageResponse<ProblemSummary>>(endpoint);
    problemListCache.set(cacheKey, { data: response, timestamp: Date.now() });
    return response;
  },

  async getProblemByIdOrSlug(idOrSlug: string, forceRefresh = false): Promise<ProblemDetail> {
    const key = idOrSlug.trim();
    const cached = problemDetailCache.get(key);

    if (!forceRefresh && cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
      return cached.data;
    }

    const response = await apiClient.get<ProblemDetail>(`/api/problems/${encodeURIComponent(idOrSlug)}`);
    problemDetailCache.set(key, { data: response, timestamp: Date.now() });
    // Also index by slug and ID if available
    if (response.slug) problemDetailCache.set(response.slug, { data: response, timestamp: Date.now() });
    if (response.id) problemDetailCache.set(response.id, { data: response, timestamp: Date.now() });

    return response;
  },

  /**
   * Hover / Background Prefetching to eliminate load spinners when user clicks a problem
   */
  prefetchProblem(idOrSlug: string): void {
    const key = idOrSlug.trim();
    const cached = problemDetailCache.get(key);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
      return; // Already fresh in memory
    }

    apiClient
      .get<ProblemDetail>(`/api/problems/${encodeURIComponent(idOrSlug)}`)
      .then((data) => {
        problemDetailCache.set(key, { data, timestamp: Date.now() });
        if (data.slug) problemDetailCache.set(data.slug, { data, timestamp: Date.now() });
        if (data.id) problemDetailCache.set(data.id, { data, timestamp: Date.now() });
      })
      .catch(() => {
        // Silent fail for prefetch
      });
  },

  clearCache(): void {
    problemDetailCache.clear();
    problemListCache.clear();
  },
};
