"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getAccessToken } from "@/lib/token-storage";

/**
 * 로그인 필요한 라우트 그룹((main))을 감싸는 클라이언트 가드.
 *
 * 이 프로젝트는 access/refresh 토큰을 localStorage에 저장하는 "클라이언트 인터셉터" 방식을
 * 쓰기 때문에(README 참고), Edge에서 도는 Next.js `middleware.ts`는 그 값을 읽을 수 없다.
 * 그래서 라우트 가드를 서버 미들웨어가 아니라 이 클라이언트 컴포넌트에서 처리한다.
 * (httpOnly 쿠키 기반 BFF로 전환하면 이 컴포넌트 대신 middleware.ts로 옮길 수 있다.)
 */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  // 서버/클라이언트 최초 렌더 결과를 동일하게 유지하기 위해 null로 시작하고,
  // mount 이후 effect에서만 토큰을 읽어 인증 여부를 확정한다.
  const [isAuthed, setIsAuthed] = useState<boolean | null>(null);

  useEffect(() => {
    const authed = !!getAccessToken();
    if (!authed) {
      router.replace("/login");
      return;
    }
    setIsAuthed(true);
  }, [router]);

  if (!isAuthed) return null;
  return <>{children}</>;
}
