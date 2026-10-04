import { API_BASE_URL } from '../config';
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

  async streamHint(
    data: SocraBuddyHintRequest,
    onToken: (token: string) => void,
    onComplete: (hint: SocraBuddyHintResponse) => void,
    onError?: (err: any) => void
  ): Promise<void> {
    const token = typeof localStorage !== 'undefined' ? localStorage.getItem('access_token') : null;
    const url = `${API_BASE_URL}/api/mentor/stream-hint`;

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        const fallback = await mentorService.getHint(data);
        onComplete(fallback);
        return;
      }

      if (!response.body) {
        const fallback = await mentorService.getHint(data);
        onComplete(fallback);
        return;
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';
      let completedData: SocraBuddyHintResponse | null = null;

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split(/\r?\n\r?\n/);
        buffer = lines.pop() || '';

        for (const block of lines) {
          if (!block.trim()) continue;
          let eventType = 'message';
          let eventData = '';

          const blockLines = block.split(/\r?\n/);
          for (const line of blockLines) {
            if (line.startsWith('event:')) {
              eventType = line.substring(6).trim();
            } else if (line.startsWith('data:')) {
              eventData = line.substring(5).trim();
            }
          }

          if (eventType === 'token' && eventData) {
            try {
              const parsed = JSON.parse(eventData);
              if (parsed.token) {
                onToken(parsed.token);
              }
            } catch {
              onToken(eventData);
            }
          } else if (eventType === 'complete' && eventData) {
            try {
              completedData = JSON.parse(eventData);
            } catch {
              // ignore parse error
            }
          }
        }
      }

      if (completedData) {
        onComplete(completedData);
      } else {
        const fallback = await mentorService.getHint(data);
        onComplete(fallback);
      }
    } catch (err) {
      if (onError) {
        onError(err);
      } else {
        throw err;
      }
    }
  },

  async diagnoseFailure(data: SocraBuddyDiagnosisRequest): Promise<SocraBuddyDiagnosisResponse> {
    return apiClient.post<SocraBuddyDiagnosisResponse>('/api/mentor/diagnose', data);
  },

  async getInlineAssist(data: InlineAssistRequest): Promise<InlineAssistResponse> {
    return apiClient.post<InlineAssistResponse>('/api/mentor/inline-assist', data);
  },
};
