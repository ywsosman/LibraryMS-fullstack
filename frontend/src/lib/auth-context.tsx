'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { UserResponse, MemberResponse, LoginRequest, RegisterRequest } from './types';
import { authApi, usersApi, membersApi, setAccessToken, setRefreshToken, getRefreshToken } from './api';

interface AuthContextType {
  user: UserResponse | null;
  member: MemberResponse | null;
  isLoading: boolean;
  isAdmin: boolean;
  isAuthenticated: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [member, setMember] = useState<MemberResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const fetchProfile = useCallback(async () => {
    try {
      const u = await usersApi.getMe();
      setUser(u);
      if (u.memberId) {
        try {
          const m = await membersApi.getMy();
          setMember(m);
        } catch {
          setMember(null);
        }
      } else {
        setMember(null);
      }
    } catch {
      setUser(null);
      setMember(null);
    }
  }, []);

  useEffect(() => {
    const initAuth = async () => {
      const refresh = getRefreshToken();
      if (!refresh) {
        setIsLoading(false);
        return;
      }
      const token = await authApi.refresh();
      if (token) {
        await fetchProfile();
      }
      setIsLoading(false);
    };

    initAuth();
  }, [fetchProfile]);

  const login = async (data: LoginRequest) => {
    setIsLoading(true);
    try {
      const res = await authApi.login(data);
      setAccessToken(res.accessToken);
      setRefreshToken(res.refreshToken);
      await fetchProfile();
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: RegisterRequest) => {
    setIsLoading(true);
    try {
      const res = await authApi.register(data);
      setAccessToken(res.accessToken);
      setRefreshToken(res.refreshToken);
      await fetchProfile();
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    const refresh = getRefreshToken();
    if (refresh) {
      try {
        await authApi.logout(refresh);
      } catch {
        // silent
      }
    }
    setAccessToken(null);
    setRefreshToken(null);
    setUser(null);
    setMember(null);
  };

  const isAdmin = Boolean(user?.roles?.includes('ROLE_ADMIN'));
  const isAuthenticated = Boolean(user);

  return (
    <AuthContext.Provider
      value={{
        user,
        member,
        isLoading,
        isAdmin,
        isAuthenticated,
        login,
        register,
        logout,
        refreshProfile: fetchProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
