import { apiFetch } from "@/lib/api-client";
import { getRefreshToken } from "@/lib/token-storage";
import type { LoginRequest, TokenResponse } from "./types";

/** POST /api/auth/login — auth-service AuthController#login */
export function login(request: LoginRequest): Promise<TokenResponse> {
  return apiFetch<TokenResponse>("/api/auth/login", {
    method: "POST",
    body: request,
  });
}

/** POST /api/auth/logout — X-Refresh-Token 헤더로 현재 세션의 refresh token을 폐기한다. */
export function logout(): Promise<void> {
  const refreshToken = getRefreshToken();
  return apiFetch<void>("/api/auth/logout", {
    method: "POST",
    headers: refreshToken ? { "X-Refresh-Token": refreshToken } : undefined,
  });
}
