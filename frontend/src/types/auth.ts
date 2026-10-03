export type UserRole = 'USER' | 'ADMIN' | 'LEARNER';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  bio?: string | null;
  githubUsername?: string | null;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  linkedinUrl?: string | null;
  contestRating?: number;
  duelRating?: number;
  currentStreak?: number;
  maxStreak?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  role?: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ForgotPasswordResponse {
  message: string;
  resetToken?: string | null;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

