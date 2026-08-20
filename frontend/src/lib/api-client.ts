import { env } from "./env";
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from "./token-storage";
import { ApiError, type ApiErrorResponse } from "@/types/api";

/**
 * Gateway(`http://localhost:8080`) 전용 fetch 래퍼.
 *
 * 인증 흐름 (gateway/GATEWAY_MIGRATION_PLAN.md "A. 프론트엔드 인터셉터" 방식):
 * 1. 요청마다 Authorization 헤더에 access token을 자동으로 붙인다.
 * 2. 401 응답을 받으면 `POST /api/auth/refresh`를 `X-Refresh-Token` 헤더와 함께 호출해 재발급을 시도한다.
 * 3. 재발급 성공 시 응답 바디(`{ accessToken }`, `NewAccessTokenDTO`)의 새 토큰으로 원래 요청을 1회 재시도한다.
 *    (게이트웨이 CORS 설정은 `New-Access-Token` 응답 헤더도 노출해두었지만, 실제 auth-service 구현은
 *    헤더가 아니라 JSON 바디로만 반환하므로 바디 기준으로 맞춘다.)
 * 4. 재발급도 실패하면 토큰을 지우고 에러를 그대로 던진다 (호출부에서 로그인 페이지로 이동 처리).
 *
 * 동시에 여러 요청이 401을 받아도 재발급 요청은 한 번만 나가도록 in-flight promise를 공유한다.
 */

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  /** 재발급 재시도 이후에도 이 옵션이 true면 다시 재시도하지 않는다 (무한 루프 방지). */
  _retried?: boolean;
};

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;

  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const res = await fetch(`${env.apiBaseUrl}/api/auth/refresh`, {
          method: "POST",
          headers: { "X-Refresh-Token": refreshToken },
        });
        if (!res.ok) {
          clearTokens();
          return null;
        }
        const { accessToken: newAccessToken } = (await res.json()) as {
          accessToken: string;
        };
        if (!newAccessToken) {
          clearTokens();
          return null;
        }
        setTokens(newAccessToken);
        return newAccessToken;
      } finally {
        // 다음 401 사이클에서 다시 재발급을 시도할 수 있도록 초기화한다.
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
}

async function parseErrorBody(res: Response): Promise<ApiErrorResponse | undefined> {
  try {
    return (await res.json()) as ApiErrorResponse;
  } catch {
    return undefined;
  }
}

/** 인증과 토큰 재발급을 적용하고 성공한 원본 Response를 반환한다. */
export async function apiFetchResponse(
  path: string,
  options: RequestOptions = {},
): Promise<Response> {
  const { body, headers, _retried, ...rest } = options;
  const accessToken = getAccessToken();
  // FormData(파일 업로드)는 브라우저가 boundary 포함 Content-Type을 직접 설정해야 하므로
  // 여기서 지정하지 않고, JSON.stringify도 건너뛴다(policy-explorer-service 업로드 등에서 사용).
  const isFormData = body instanceof FormData;

  const res = await fetch(`${env.apiBaseUrl}${path}`, {
    ...rest,
    headers: {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...headers,
    },
    body: isFormData ? body : body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 && !_retried) {
    const newAccessToken = await refreshAccessToken();
    if (newAccessToken) {
      return apiFetchResponse(path, { ...options, _retried: true });
    }
  }

  if (!res.ok) {
    const errorBody = await parseErrorBody(res);
    // message: Java 서비스(course/member/subscription/auth), detail: FastAPI(policy-explorer-service).
    throw new ApiError(
      res.status,
      errorBody?.message ?? errorBody?.detail ?? res.statusText,
      errorBody,
    );
  }

  return res;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const res = await apiFetchResponse(path, options);

  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}
