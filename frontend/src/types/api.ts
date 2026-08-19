/**
 * 백엔드 공통 에러 응답 형식.
 *
 * course/member/subscription/auth 4개 서비스 모두 `record ErrorResponse(String message)`
 * 형태만 사용한다 (status/code/timestamp 필드 없음, HTTP 상태 코드는 응답 status로만 전달).
 * 서비스마다 개별 정의라 스펙이 아직 하나로 확정되진 않았으니, 나중에 공통 모듈로 통일되면
 * 이 타입도 같이 넓혀간다.
 */
export interface ApiErrorResponse {
  message: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly body?: ApiErrorResponse;

  constructor(status: number, message: string, body?: ApiErrorResponse) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}
