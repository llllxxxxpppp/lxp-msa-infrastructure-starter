"use client";

import { useCallback, useState } from "react";
import { setTokens, clearTokens } from "@/lib/token-storage";
import { ApiError } from "@/types/api";
import * as authApi from "./api";
import type { LoginRequest } from "./types";

interface UseLoginResult {
  login: (request: LoginRequest) => Promise<boolean>;
  isLoading: boolean;
  error: string | null;
}

/** 로그인 폼에서 사용하는 훅. 성공 시 토큰을 저장하고 true를 반환한다. */
export function useLogin(): UseLoginResult {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const login = useCallback(async (request: LoginRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const { accessToken, refreshToken } = await authApi.login(request);
      setTokens(accessToken, refreshToken);
      return true;
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "로그인에 실패했습니다.");
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { login, isLoading, error };
}

/** 로그아웃: 서버에 refresh token 폐기를 요청하고, 결과와 무관하게 로컬 토큰은 지운다. */
export function useLogout(): () => Promise<void> {
  return useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      clearTokens();
    }
  }, []);
}
