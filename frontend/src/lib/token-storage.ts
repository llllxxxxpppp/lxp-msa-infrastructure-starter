/**
 * Access/Refresh 토큰 저장 유틸.
 *
 * - Access Token: 메모리(모듈 스코프 변수)에 우선 보관해 XSS 노출 표면을 줄이고,
 *   새로고침 대비로 localStorage에도 보조 저장한다.
 * - Refresh Token: localStorage에 저장한다. 게이트웨이는 이를 `X-Refresh-Token` 헤더로
 *   전달받아 재발급을 처리한다 (gateway/GATEWAY_MIGRATION_PLAN.md의 "A. 프론트엔드 인터셉터" 방식).
 *
 * 추후 보안 강화가 필요해지면 이 파일만 교체해 httpOnly 쿠키 기반(BFF) 방식으로
 * 전환할 수 있도록, 다른 코드는 이 모듈이 노출하는 함수만 사용한다.
 */

const ACCESS_TOKEN_KEY = "lxp:accessToken";
const REFRESH_TOKEN_KEY = "lxp:refreshToken";

let accessTokenMemo: string | null = null;

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

export function getAccessToken(): string | null {
  if (accessTokenMemo) return accessTokenMemo;
  if (!isBrowser()) return null;
  accessTokenMemo = window.localStorage.getItem(ACCESS_TOKEN_KEY);
  return accessTokenMemo;
}

export function getRefreshToken(): string | null {
  if (!isBrowser()) return null;
  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setTokens(accessToken: string, refreshToken?: string): void {
  accessTokenMemo = accessToken;
  if (!isBrowser()) return;
  window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  if (refreshToken) {
    window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

export function clearTokens(): void {
  accessTokenMemo = null;
  if (!isBrowser()) return;
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}
