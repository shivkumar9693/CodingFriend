import { apiClient } from './apiClient';
import {
  SocraBuddyDiagnosisRequest,
  SocraBuddyDiagnosisResponse,
  SocraBuddyHintRequest,
  SocraBuddyHintResponse,
  InlineAssistRequest,
  InlineAssistResponse,
} from '../types/mentor';

export const mentorService = {
  async getHint(data: SocraBuddyHintRequest): Promise<SocraBuddyHintResponse> {
    return apiClient.post<SocraBuddyHintResponse>('/api/mentor/hints', data);
  },

  async diagnoseFailure(data: SocraBuddyDiagnosisRequest): Promise<SocraBuddyDiagnosisResponse> {
    return apiClient.post<SocraBuddyDiagnosisResponse>('/api/mentor/diagnose', data);
  },

  async getInlineAssist(data: InlineAssistRequest): Promise<InlineAssistResponse> {
    return apiClient.post<InlineAssistResponse>('/api/mentor/inline-assist', data);
  },
};
