"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getAccessToken } from "@/lib/token-storage";
import { decodeAccessToken, getRoles } from "@/lib/jwt";

interface RoleGuardProps {
  /** 이 중 하나라도 포함하면 통과. 예: ["ROLE_ADMIN"] */
  allowedRoles: string[];
  children: React.ReactNode;
}

/**
 * AuthGuard(로그인 여부)에 역할 검사를 더한 가드. policy-explorer 같은 관리자 전용 라우트에 쓴다.
 *
 * ⚠️ 이 역할 판정은 accessToken payload를 클라이언트에서 디코드한 값(lib/jwt.ts)에 기반한
 * UI 편의 목적일 뿐이다. 실제 인가는 게이트웨이(JwtAuthenticationFilter)와 각 서비스가
 * 항상 다시 검증하므로, 여기서 통과시키더라도 권한 없는 API 호출은 서버에서 401/403으로 막힌다.
 */
export function RoleGuard({ allowedRoles, children }: RoleGuardProps) {
  const router = useRouter();
  const [isAllowed] = useState(() => {
    const token = getAccessToken();
    if (!token) return false;
    const roles = getRoles(decodeAccessToken(token));
    return allowedRoles.some((role) => roles.includes(role));
  });

  useEffect(() => {
    if (!isAllowed) {
      router.replace("/courses");
    }
  }, [isAllowed, router]);

  if (!isAllowed) return null;
  return <>{children}</>;
}
