import { apiClient } from './apiClient';
import {
  CreateRevisionNoteRequest,
  GeneratePostMortemRequest,
  PostMortemAnalysis,
  RevisionNote,
  RevisionNotesSummary,
  UpdateRevisionNoteRequest,
} from '../types/notes';

export interface GetNotesParams {
  status?: string;
  bookmarked?: boolean;
  search?: string;
}

export const revisionNotesService = {
  async generatePostMortem(req: GeneratePostMortemRequest): Promise<PostMortemAnalysis> {
    return apiClient.post<PostMortemAnalysis>('/api/learning/notes/analyze', req);
  },

  async createNote(req: CreateRevisionNoteRequest): Promise<RevisionNote> {
    return apiClient.post<RevisionNote>('/api/learning/notes', req);
  },

  async getUserNotes(params: GetNotesParams = {}): Promise<RevisionNote[]> {
    const searchParams = new URLSearchParams();
    if (params.status) searchParams.append('status', params.status);
    if (params.bookmarked !== undefined) searchParams.append('bookmarked', params.bookmarked.toString());
    if (params.search) searchParams.append('search', params.search);

    const queryString = searchParams.toString();
    const endpoint = queryString ? `/api/learning/notes?${queryString}` : '/api/learning/notes';
    return apiClient.get<RevisionNote[]>(endpoint);
  },

  async getSummary(): Promise<RevisionNotesSummary> {
    return apiClient.get<RevisionNotesSummary>('/api/learning/notes/summary');
  },

  async getNoteById(id: string): Promise<RevisionNote> {
    return apiClient.get<RevisionNote>(`/api/learning/notes/${id}`);
  },

  async updateNote(id: string, req: UpdateRevisionNoteRequest): Promise<RevisionNote> {
    return apiClient.put<RevisionNote>(`/api/learning/notes/${id}`, req);
  },

  async markReviewed(id: string): Promise<RevisionNote> {
    return apiClient.post<RevisionNote>(`/api/learning/notes/${id}/review`, {});
  },

  async deleteNote(id: string): Promise<void> {
    return apiClient.delete<void>(`/api/learning/notes/${id}`);
  },
};
