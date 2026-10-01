import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { LoginRequest, RegisterRequest, User } from '../types/auth';
import { authService } from '../services/authService';
import { userService } from '../services/userService';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<User>;
  register: (data: RegisterRequest) => Promise<User>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => authService.getCurrentUser());
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const refreshProfile = useCallback(async () => {
    const token = localStorage.getItem('access_token');
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    try {
      const profile = await userService.getProfile();
      setUser({
        id: profile.id,
        email: profile.email,
        fullName: profile.fullName,
        bio: profile.bio,
        githubUsername: profile.githubUsername,
        role: profile.role,
        createdAt: profile.createdAt,
        updatedAt: profile.updatedAt,
      });
    } catch {
      // If token is invalid/expired and refresh failed, user is null
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshProfile();

    const handleUnauthorized = () => {
      setUser(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [refreshProfile]);

  const login = async (credentials: LoginRequest): Promise<User> => {
    const response = await authService.login(credentials);
    setUser(response.user);
    return response.user;
  };

  const register = async (data: RegisterRequest): Promise<User> => {
    const response = await authService.register(data);
    setUser(response.user);
    return response.user;
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
