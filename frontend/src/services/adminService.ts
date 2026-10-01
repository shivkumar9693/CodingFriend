import { apiClient } from './apiClient';
import {
  AdminProblemDetail,
  AdminTestCase,
  CreateProblemRequest,
  CreateTestCaseRequest,
  UpdateProblemRequest,
  UpdateTestCaseRequest,
} from '../types/admin';

export const adminService = {
  // Problem Administration
  async createProblem(data: CreateProblemRequest): Promise<AdminProblemDetail> {
    return apiClient.post<AdminProblemDetail>('/api/problems', data);
  },

  async updateProblem(id: string, data: UpdateProblemRequest): Promise<AdminProblemDetail> {
    return apiClient.put<AdminProblemDetail>(`/api/problems/${id}`, data);
  },

  async deleteProblem(id: string): Promise<void> {
    return apiClient.delete<void>(`/api/problems/${id}`);
  },

  // Test Case Administration
  async getTestCases(problemId: string): Promise<AdminTestCase[]> {
    return apiClient.get<AdminTestCase[]>(`/api/admin/problems/${problemId}/test-cases`);
  },

  async getTestCaseById(problemId: string, testCaseId: string): Promise<AdminTestCase> {
    return apiClient.get<AdminTestCase>(`/api/admin/problems/${problemId}/test-cases/${testCaseId}`);
  },

  async createTestCase(problemId: string, data: CreateTestCaseRequest): Promise<AdminTestCase> {
    return apiClient.post<AdminTestCase>(`/api/admin/problems/${problemId}/test-cases`, data);
  },

  async updateTestCase(
    problemId: string,
    testCaseId: string,
    data: UpdateTestCaseRequest
  ): Promise<AdminTestCase> {
    return apiClient.put<AdminTestCase>(`/api/admin/problems/${problemId}/test-cases/${testCaseId}`, data);
  },

  async deleteTestCase(problemId: string, testCaseId: string): Promise<void> {
    return apiClient.delete<void>(`/api/admin/problems/${problemId}/test-cases/${testCaseId}`);
  },
};
