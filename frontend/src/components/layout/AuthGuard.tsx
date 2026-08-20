"use client";

import { useEffect, useSyncExternalStore } from "react";
import { useRouter } from "next/navigation";
import { getAccessToken } from "@/lib/token-storage";

// token-storage는 변경을 구독할 수단이 없으므로 subscribe는 아무것도 하지 않는다 —
// useSyncExternalStore는 마운트 시 getSnapshot을 다시 읽어 서버 스냅샷과 동기화해 준다.
function subscribeToNothing() {
  return () => {};
}

function getIsAuthedSnapshot(): boolean {
  return !!getAccessToken();
}

function getServerIsAuthedSnapshot(): boolean {
  return false;
}

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
  // 서버 렌더와 클라이언트 최초 렌더 결과를 동일하게(false) 유지하기 위해
  // localStorage 기반 인증 여부를 useSyncExternalStore로 읽는다.
  const isAuthed = useSyncExternalStore(subscribeToNothing, getIsAuthedSnapshot, getServerIsAuthedSnapshot);

  useEffect(() => {
    // isAuthed(동기화된 스냅샷)이 아니라 실제 토큰을 다시 읽는다 — 서버 스냅샷(false)과
    // 아직 동기화되기 전인 마운트 시점에는 isAuthed가 일시적으로 false일 수 있어,
    // 그 값을 그대로 리다이렉트 조건으로 쓰면 로그인된 사용자도 잘못 튕겨나갈 수 있다.
    if (!getAccessToken()) {
      router.replace("/login");
    }
  }, [router]);

  if (!isAuthed) return null;
  return <>{children}</>;
}
