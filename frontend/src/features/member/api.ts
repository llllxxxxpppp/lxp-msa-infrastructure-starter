import { apiFetch } from "@/lib/api-client";
import type {
  ChangePasswordRequest,
  SignupRequest,
  UpdateInstructorProfileRequest,
  UserResponse,
} from "./types";

/**
 * POST /api/members/signup — member-service MemberController#signup.
 * 게이트웨이 공개 경로(PUBLIC_PATH_PATTERNS)에 등록돼 있어 토큰 없이 호출 가능하다.
 */
export function signup(request: SignupRequest): Promise<UserResponse> {
  return apiFetch<UserResponse>("/api/members/signup", {
    method: "POST",
    body: request,
  });
}

/**
 * PATCH /api/members/me/password — MemberSelfController#changePassword.
 * X-User-Id는 게이트웨이가 검증된 토큰에서 뽑아 주입하므로 클라이언트는 신경 쓸 필요 없다.
 */
export function changePassword(request: ChangePasswordRequest): Promise<void> {
  return apiFetch<void>("/api/members/me/password", {
    method: "PATCH",
    body: request,
  });
}

/** PATCH /api/members/me/instructor-profile — MemberSelfController#updateInstructorProfile (강사 전용) */
export function updateInstructorProfile(
  request: UpdateInstructorProfileRequest,
): Promise<UserResponse> {
  return apiFetch<UserResponse>("/api/members/me/instructor-profile", {
    method: "PATCH",
    body: request,
  });
}

/** DELETE /api/members/me — MemberSelfController#withdraw */
export function withdraw(): Promise<void> {
  return apiFetch<void>("/api/members/me", { method: "DELETE" });
}
