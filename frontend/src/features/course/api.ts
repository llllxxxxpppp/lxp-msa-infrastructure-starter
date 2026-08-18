import { apiFetch } from "@/lib/api-client";
import type { CourseDetail, CoursePage } from "./types";

export interface GetCoursesParams {
  keyword?: string;
  page?: number;
  size?: number;
}

/**
 * GET /api/courses — course-service CourseController#getCourses. 로그인 필요(토큰만 있으면 role 무관).
 * 서버는 keyword 검색만 지원하고 카테고리/난이도 파라미터가 없다 — 그 필터는 호출부에서 클라이언트로 처리한다.
 */
export function getCourses(params: GetCoursesParams = {}): Promise<CoursePage> {
  const query = new URLSearchParams();
  if (params.keyword) query.set("keyword", params.keyword);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 10));

  return apiFetch<CoursePage>(`/api/courses?${query.toString()}`);
}

/** GET /api/courses/{id}/detail — course-service CourseController#getCourseDetail */
export function getCourseDetail(courseId: number): Promise<CourseDetail> {
  return apiFetch<CourseDetail>(`/api/courses/${courseId}/detail`);
}
