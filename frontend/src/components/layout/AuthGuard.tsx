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
  // 마운트 시 1회만 평가한다(지연 초기화) — effect 안에서 setState하지 않도록 상태 계산과
  // 리다이렉트(부수효과)를 분리한다.
  const [isAuthed] = useState(() => typeof window !== "undefined" && !!getAccessToken());

  useEffect(() => {
    if (!isAuthed) {
      router.replace("/login");
    }
  }, [isAuthed, router]);

  if (!isAuthed) return null;
  return <>{children}</>;
}
