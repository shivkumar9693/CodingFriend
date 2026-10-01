import { API_BASE_URL } from '../config';

export interface ApiError {
  status: number;
  message: string;
  validationErrors?: Array<{ field: string; message: string }>;
}

class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl.replace(/\/+$/, '');
  }

  private getAccessToken(): string | null {
    return localStorage.getItem('access_token');
  }

  private getRefreshToken(): string | null {
    return localStorage.getItem('refresh_token');
  }

  public setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('refresh_token', refreshToken);
  }

  public clearTokens(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user_data');
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    isRetry = false
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
    const token = this.getAccessToken();

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      });

      if (response.status === 401 && !isRetry && !endpoint.includes('/auth/')) {
        const refreshed = await this.tryRefreshToken();
        if (refreshed) {
          return this.request<T>(endpoint, options, true);
        } else {
          this.clearTokens();
          window.dispatchEvent(new CustomEvent('auth:unauthorized'));
        }
      }

      if (response.status === 204) {
        return null as T;
      }

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        const error: ApiError = {
          status: response.status,
          message: data?.message || data?.error || 'An unexpected error occurred',
          validationErrors: data?.validationErrors || data?.errors || undefined,
        };
        throw error;
      }

      if (data && typeof data === 'object' && 'data' in data && 'success' in data && data.success === true) {
        return data.data as T;
      }

      return data as T;
    } catch (err: unknown) {
      if ((err as ApiError)?.status !== undefined) {
        throw err;
      }
      const networkError: ApiError = {
        status: 0,
        message:
          err instanceof Error
            ? err.message
            : 'Unable to connect to the server. Please ensure the backend is running.',
      };
      throw networkError;
    }
  }

  private async tryRefreshToken(): Promise<boolean> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) return false;

    try {
      const res = await fetch(`${this.baseUrl}/api/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({ refreshToken }),
      });

      if (!res.ok) return false;

      const data = await res.json();
      const tokenPayload = (data && typeof data === 'object' && 'data' in data && data.data) ? data.data : data;
      if (tokenPayload?.accessToken && tokenPayload?.refreshToken) {
        this.setTokens(tokenPayload.accessToken, tokenPayload.refreshToken);
        return true;
      }
      return false;
    } catch {
      return false;
    }
  }

  public get<T>(endpoint: string, headers?: Record<string, string>): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET', headers });
  }

  public post<T>(endpoint: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  public put<T>(endpoint: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  public delete<T>(endpoint: string, headers?: Record<string, string>): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE', headers });
  }
}

export const apiClient = new ApiClient(API_BASE_URL);
