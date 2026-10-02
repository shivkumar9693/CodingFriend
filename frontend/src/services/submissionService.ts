import { apiClient } from './apiClient';
import { CreateSubmissionRequest, SubmissionHintResponse, SubmissionResponse, SubmissionSummaryResponse } from '../types/submission';
import { PageResponse } from '../types/problem';

export interface GetSubmissionsParams {
  problemId?: string;
  userId?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export const submissionService = {
  async createSubmission(data: CreateSubmissionRequest): Promise<SubmissionResponse> {
    return apiClient.post<SubmissionResponse>('/api/submissions', data);
  },

  async runCode(data: CreateSubmissionRequest): Promise<SubmissionResponse> {
    return apiClient.post<SubmissionResponse>('/api/submissions/run', data);
  },

  async getSubmissionById(id: string): Promise<SubmissionResponse> {
    return apiClient.get<SubmissionResponse>(`/api/submissions/${id}`);
  },

  async getSubmissionHint(submissionId: string, level: number): Promise<SubmissionHintResponse> {
    return apiClient.post<SubmissionHintResponse>(`/api/submissions/${submissionId}/hints`, { level });
  },

  async getSubmissions(params: GetSubmissionsParams = {}): Promise<PageResponse<SubmissionSummaryResponse>> {
    const searchParams = new URLSearchParams();
    if (params.problemId) searchParams.append('problemId', params.problemId);
    if (params.userId) searchParams.append('userId', params.userId);
    if (params.page !== undefined) searchParams.append('page', params.page.toString());
    if (params.size !== undefined) searchParams.append('size', params.size.toString());
    if (params.sortBy) searchParams.append('sortBy', params.sortBy);
    if (params.sortDirection) searchParams.append('sortDirection', params.sortDirection);

    const queryString = searchParams.toString();
    const endpoint = queryString ? `/api/submissions?${queryString}` : '/api/submissions';
    return apiClient.get<PageResponse<SubmissionSummaryResponse>>(endpoint);
  },
};

