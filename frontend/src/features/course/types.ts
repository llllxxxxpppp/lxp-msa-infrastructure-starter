/**
 * course-service의 응답 DTO와 1:1로 맞춘 타입.
 * (course-service/.../application/dto/response/CourseSummaryResponse.java,
 *  CoursePageResponse.java, CourseDetailResponse.java)
 */

export interface CourseSummary {
  courseId: number;
  instructorId: number;
  title: string;
  status: string;
  thumbnailUrl: string | null;
  category: string;
  difficulty: string;
  durationMinutes: number;
}

export interface CoursePage {
  courses: CourseSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/** course-service CourseController#getCourseDetail (LectureResponse) */
export interface Lecture {
  lectureId: number;
  title: string;
  status: string;
  contentType: string;
  sortOrder: number;
}

/** course-service CourseController#getCourseDetail (MissionResponse) */
export interface Mission {
  missionId: number;
  title: string;
  status: string;
  sortOrder: number;
}

export interface CourseDetail {
  courseId: number;
  instructorId: number;
  title: string;
  status: string;
  description: string;
  thumbnailUrl: string | null;
  category: string;
  difficulty: string;
  durationMinutes: number;
  lectures: Lecture[];
  missions: Mission[];
}

/**
 * course-service Category enum(com.lcs.course.domain.model.vo.Category)의 한글 라벨.
 * design/course/course-list 좌측 필터 사이드바와 동일한 그룹·순서.
 */
export const CATEGORY_GROUPS: { group: string; categories: { value: string; label: string }[] }[] =
  [
    {
      group: "Development",
      categories: [
        { value: "BACKEND", label: "백엔드 개발" },
        { value: "FRONTEND", label: "프론트엔드 개발" },
        { value: "MOBILE", label: "모바일 개발" },
        { value: "DEVOPS", label: "데브옵스·인프라" },
        { value: "SECURITY", label: "보안" },
      ],
    },
    {
      group: "Data/AI",
      categories: [
        { value: "DATA_ANALYSIS", label: "데이터 분석" },
        { value: "DATA_ENGINEERING", label: "데이터 엔지니어링" },
        { value: "AI_ML", label: "AI·머신러닝" },
      ],
    },
    {
      group: "Planning/Design",
      categories: [
        { value: "PRODUCT", label: "프로덕트" },
        { value: "DESIGN", label: "디자인·UX" },
      ],
    },
  ];

export const CATEGORY_LABELS: Record<string, string> = Object.fromEntries(
  CATEGORY_GROUPS.flatMap((g) => g.categories).map((c) => [c.value, c.label]),
);

/** course-service Difficulty enum(com.lcs.course.domain.model.vo.Difficulty)의 한글 라벨. */
export const DIFFICULTY_OPTIONS: { value: string; label: string }[] = [
  { value: "BEGINNER", label: "입문" },
  { value: "PRACTICAL", label: "실전" },
  { value: "ADVANCED", label: "심화" },
];

export const DIFFICULTY_LABELS: Record<string, string> = Object.fromEntries(
  DIFFICULTY_OPTIONS.map((d) => [d.value, d.label]),
);
