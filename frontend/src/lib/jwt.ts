/**
 * accessToken의 JWT payload를 base64url 디코드만 해서 읽는다.
 *
 * ⚠️ 서명 검증을 하지 않는다 — 오직 화면 표시(이메일/역할 보여주기)용이다.
 * 실제 인가는 항상 게이트웨이(JwtAuthenticationFilter)와 각 서비스가 담당하며,
 * 이 함수의 결과를 인가 판단(예: "관리자니까 버튼을 활성화")에 쓰더라도 서버가
 * 다시 막아준다는 전제하에만 UI 편의용으로 사용한다.
 *
 * 페이로드 형태는 auth-service JwtTokenProvider#createAccessToken 기준:
 * { sub: email, userId, roles: "ROLE_MEMBER,ROLE_INSTRUCTOR" 형태의 콤마 구분 문자열, iat, exp }
 */
export interface AccessTokenPayload {
  sub?: string;
  userId?: number;
  roles?: string;
  iat?: number;
  exp?: number;
}

export function decodeAccessToken(token: string): AccessTokenPayload | null {
  const parts = token.split(".");
  if (parts.length !== 3) return null;

  try {
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join(""),
    );
    return JSON.parse(json) as AccessTokenPayload;
  } catch {
    return null;
  }
}

export function getRoles(payload: AccessTokenPayload | null): string[] {
  if (!payload?.roles) return [];
  return payload.roles
    .split(",")
    .map((role) => role.trim())
    .filter(Boolean);
}
