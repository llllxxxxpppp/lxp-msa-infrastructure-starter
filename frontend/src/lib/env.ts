/**
 * 환경 변수를 한 곳에서 검증하고 export한다.
 *
 * `NEXT_PUBLIC_*` 값은 빌드 타임에 번들에 박히기 때문에, 백엔드의 JWT_SECRET처럼
 * 모듈 로드 시점에 throw하면 `.env.local` 없이 `next build`만 돌려도(CI, 첫 클론 직후 등)
 * 정적 프리렌더링이 그 자리에서 실패해버린다. 그래서 값이 없으면 로컬 gateway 기본값으로
 * 폴백하고 콘솔 경고만 남긴다 — 실제로 다른 주소를 써야 하는 환경에서만 문제가 드러난다.
 */
const DEFAULT_API_BASE_URL = "http://localhost:8080";

function readPublicEnv(name: string, value: string | undefined, fallback: string): string {
  if (!value) {
    console.warn(
      `[env] ${name} 환경 변수가 설정되지 않아 기본값(${fallback})을 사용합니다. frontend/.env.local.example을 참고해 .env.local을 만드세요.`,
    );
    return fallback;
  }
  return value;
}

export const env = {
  /** Gateway(Spring Cloud Gateway) 진입점. 예: http://localhost:8080 */
  apiBaseUrl: readPublicEnv(
    "NEXT_PUBLIC_API_BASE_URL",
    process.env.NEXT_PUBLIC_API_BASE_URL,
    DEFAULT_API_BASE_URL,
  ),
};
