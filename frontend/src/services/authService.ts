import { apiClient } from './apiClient';
import { AuthResponse, LoginRequest, RegisterRequest, TokenRefreshResponse, User } from '../types/auth';

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const raw = await apiClient.post<AuthResponse>('/api/auth/login', credentials);
    const response: AuthResponse = (raw as unknown as { data: AuthResponse }).data || raw;
    apiClient.setTokens(response.accessToken, response.refreshToken);
    localStorage.setItem('user_data', JSON.stringify(response.user));
    return response;
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const raw = await apiClient.post<AuthResponse>('/api/auth/register', data);
    const response: AuthResponse = (raw as unknown as { data: AuthResponse }).data || raw;
    apiClient.setTokens(response.accessToken, response.refreshToken);
    localStorage.setItem('user_data', JSON.stringify(response.user));
    return response;
  },

  async logout(): Promise<void> {
    const refreshToken = localStorage.getItem('refresh_token');
    try {
      if (refreshToken) {
        await apiClient.post('/api/auth/logout', { refreshToken });
      }
    } finally {
      apiClient.clearTokens();
    }
  },

  async refreshToken(refreshToken: string): Promise<TokenRefreshResponse> {
    return apiClient.post<TokenRefreshResponse>('/api/auth/refresh', { refreshToken });
  },

  getCurrentUser(): User | null {
    const userStr = localStorage.getItem('user_data');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr) as User;
    } catch {
      return null;
    }
  },
};
