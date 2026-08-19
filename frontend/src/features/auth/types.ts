/** auth-service의 DTO와 1:1로 맞춘 타입 (auth-service/.../controller/dto). */

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}
