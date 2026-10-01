import { apiClient } from './apiClient';
import { PageResponse, ProblemDetail, ProblemFilterParams, ProblemSummary } from '../types/problem';

export const problemService = {
  async getProblems(params: ProblemFilterParams = {}): Promise<PageResponse<ProblemSummary>> {
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

    return apiClient.get<PageResponse<ProblemSummary>>(endpoint);
  },

  async getProblemByIdOrSlug(idOrSlug: string): Promise<ProblemDetail> {
    return apiClient.get<ProblemDetail>(`/api/problems/${encodeURIComponent(idOrSlug)}`);
  },
};
