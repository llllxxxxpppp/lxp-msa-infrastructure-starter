/**
 * member-service의 DTO와 1:1로 맞춘 타입.
 * (member-service/.../application/dto/{request/SignupRequest,response/UserResponseDTO,
 *  request/ChangePasswordRequest,request/UpdateInstructorProfileRequest}.java)
 */

export interface SignupRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  id: number;
  email: string;
  /** member-service의 MemberRole enum 값 (예: MEMBER, INSTRUCTOR, ADMIN). */
  role: string;
}

/** PATCH /api/members/me/password — MemberSelfController#changePassword */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/** PATCH /api/members/me/instructor-profile — MemberSelfController#updateInstructorProfile */
export interface UpdateInstructorProfileRequest {
  name: string;
  profileImageUrl?: string;
  introduction?: string;
}
