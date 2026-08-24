"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import * as courseApi from "@/features/course/api";
import { CATEGORY_LABELS, DIFFICULTY_LABELS, type CourseDetail } from "@/features/course/types";
import { ApiError } from "@/types/api";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Button } from "@/components/ui/Button";
import { MaterialIcon } from "@/components/ui/MaterialIcon";
import { CourseChatWidget } from "@/components/chat/CourseChatWidget";

export default function CourseDetailPage() {
  const params = useParams<{ courseId: string }>();
  const router = useRouter();
  const [course, setCourse] = useState<CourseDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // params.courseId는 렌더 시점에 이미 확정된 값이라 effect 밖에서 동기적으로 계산한다.
  const courseId = useMemo(() => Number(params.courseId), [params.courseId]);
  const isInvalidId = !Number.isFinite(courseId);

  useEffect(() => {
    if (isInvalidId) return;

    // eslint-disable-next-line react-hooks/set-state-in-effect -- 데이터 패칭 effect의 표준 패턴
    setIsLoading(true);
    setError(null);
    courseApi
      .getCourseDetail(courseId)
      .then(setCourse)
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : "강좌 정보를 불러오지 못했습니다."),
      )
      .finally(() => setIsLoading(false));
  }, [courseId, isInvalidId]);

  if (isInvalidId) return <p className="text-body-sm text-error-red">잘못된 강좌 ID입니다.</p>;
  if (isLoading) return <p className="text-body-sm text-slate-text">불러오는 중...</p>;
  if (error) return <p className="text-body-sm text-error-red">{error}</p>;
  if (!course) return null;

  // 강의(Lecture)와 미션(Mission)을 sortOrder로 합쳐 하나의 커리큘럼 목록으로 보여준다.
  const curriculumItems = [
    ...course.lectures.map((lecture) => ({
      key: `lecture-${lecture.lectureId}`,
      title: lecture.title,
      status: lecture.status,
      sortOrder: lecture.sortOrder,
      icon: "play_circle",
      kind: "강의",
    })),
    ...course.missions.map((mission) => ({
      key: `mission-${mission.missionId}`,
      title: mission.title,
      status: mission.status,
      sortOrder: mission.sortOrder,
      icon: "assignment",
      kind: "미션",
    })),
  ].sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <div className="gap-stack-lg flex flex-col">
      <button
        type="button"
        onClick={() => router.push("/courses")}
        className="text-body-sm text-slate-text hover:text-secondary flex w-fit items-center gap-1"
      >
        <MaterialIcon name="arrow_back" className="text-[18px]" />
        목록으로
      </button>

      <div>
        <div className="mb-stack-sm flex flex-wrap gap-2">
          <Chip tone="primary">{CATEGORY_LABELS[course.category] ?? course.category}</Chip>
          <Chip>{DIFFICULTY_LABELS[course.difficulty] ?? course.difficulty}</Chip>
          <Chip>{course.durationMinutes}분</Chip>
        </div>
        <h1 className="text-headline-lg-mobile text-primary md:text-headline-lg">{course.title}</h1>
        <p className="mt-stack-sm text-body-md text-slate-text">{course.description}</p>
      </div>

      {/* <Button
        variant="secondary"
        className="w-fit"
        onClick={() => router.push("/curriculum-recommendation")}
      >
        <MaterialIcon name="auto_awesome" className="text-[18px]" />
        View Recommended Curriculum Path
      </Button> */}

      <Card className="p-stack-md">
        <h2 className="mb-stack-md text-headline-sm text-primary">커리큘럼</h2>
        {curriculumItems.length === 0 ? (
          <p className="text-body-sm text-slate-text">등록된 강의/미션이 없습니다.</p>
        ) : (
          <ul className="divide-outline-variant flex flex-col divide-y">
            {curriculumItems.map((item) => (
              <li key={item.key} className="gap-stack-sm py-stack-sm flex items-center">
                <MaterialIcon name={item.icon} className="text-secondary" />
                <div className="flex-1">
                  <p className="text-body-sm text-on-surface">{item.title}</p>
                  <p className="text-label-sm text-outline">{item.kind}</p>
                </div>
                <Chip tone={item.status === "PUBLIC" ? "success" : "neutral"}>{item.status}</Chip>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <CourseChatWidget courseId={course.courseId} courseTitle={course.title} />
    </div>
  );
}
