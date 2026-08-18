"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import * as courseApi from "@/features/course/api";
import {
  CATEGORY_GROUPS,
  CATEGORY_LABELS,
  DIFFICULTY_LABELS,
  DIFFICULTY_OPTIONS,
  type CourseSummary,
} from "@/features/course/types";
import { ApiError } from "@/types/api";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { MaterialIcon } from "@/components/ui/MaterialIcon";

type SortOption = "recommended" | "title" | "duration";

// 시드 데이터의 thumbnailUrl은 실제로 존재하지 않는 더미 CDN 주소라 항상 이 플레이스홀더로 대체된다.
const THUMBNAIL_FALLBACK =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='225' viewBox='0 0 400 225'%3E%3Crect width='400' height='225' fill='%23e5e9eb'/%3E%3Ctext x='50%25' y='50%25' fill='%2374777d' font-family='sans-serif' font-size='16' text-anchor='middle' dominant-baseline='middle'%3ELXP Course%3C/text%3E%3C/svg%3E";

// 카테고리/난이도 파라미터가 서버에 없어(CourseService.getCourses는 keyword만 지원) 넉넉히 받아 클라이언트에서 거른다.
const FETCH_SIZE = 50;

export default function CoursesPage() {
  const router = useRouter();
  const [courses, setCourses] = useState<CourseSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [selectedCategories, setSelectedCategories] = useState<Set<string>>(new Set());
  const [selectedDifficulty, setSelectedDifficulty] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<SortOption>("recommended");

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedKeyword(keyword), 300);
    return () => clearTimeout(timer);
  }, [keyword]);

  useEffect(() => {
    // 검색어가 바뀔 때마다 새 요청을 보내면서 로딩 상태를 다시 보여준다 — 데이터 패칭 effect의
    // 표준 패턴이라 set-state-in-effect 규칙은 이 두 줄에 한해 비활성화한다.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setError(null);
    courseApi
      .getCourses({ keyword: debouncedKeyword, size: FETCH_SIZE })
      .then((res) => setCourses(res.courses))
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : "강좌 목록을 불러오지 못했습니다."),
      )
      .finally(() => setIsLoading(false));
  }, [debouncedKeyword]);

  const visibleCourses = useMemo(() => {
    let result = courses;
    if (selectedCategories.size > 0) {
      result = result.filter((c) => selectedCategories.has(c.category));
    }
    if (selectedDifficulty) {
      result = result.filter((c) => c.difficulty === selectedDifficulty);
    }
    if (sortBy === "title") {
      result = [...result].sort((a, b) => a.title.localeCompare(b.title));
    } else if (sortBy === "duration") {
      result = [...result].sort((a, b) => a.durationMinutes - b.durationMinutes);
    }
    return result;
  }, [courses, selectedCategories, selectedDifficulty, sortBy]);

  function toggleCategory(value: string) {
    setSelectedCategories((prev) => {
      const next = new Set(prev);
      if (next.has(value)) next.delete(value);
      else next.add(value);
      return next;
    });
  }

  return (
    <div className="gap-gutter grid grid-cols-1 md:grid-cols-12">
      {/* 좌측 필터 사이드바 */}
      <aside className="gap-stack-lg hidden flex-col md:col-span-3 md:flex">
        <Card className="p-stack-md">
          <h3 className="mb-stack-md text-label-md text-primary">카테고리</h3>
          <div className="gap-stack-sm flex flex-col">
            {CATEGORY_GROUPS.map((group) => (
              <div key={group.group}>
                <p className="text-label-sm text-outline mb-1">{group.group}</p>
                <div className="flex flex-col gap-1">
                  {group.categories.map((category) => (
                    <label
                      key={category.value}
                      className="hover:bg-surface-container flex cursor-pointer items-center gap-2 rounded p-1 transition-colors"
                    >
                      <input
                        type="checkbox"
                        className="border-outline-variant text-secondary h-4 w-4 rounded-sm"
                        checked={selectedCategories.has(category.value)}
                        onChange={() => toggleCategory(category.value)}
                      />
                      <span className="text-body-sm text-on-surface-variant">{category.label}</span>
                    </label>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card className="p-stack-md">
          <h3 className="mb-stack-md text-label-md text-primary">Skill Level</h3>
          <div className="gap-stack-sm flex flex-col">
            <label className="hover:bg-surface-container flex cursor-pointer items-center gap-2 rounded p-1 transition-colors">
              <input
                type="radio"
                name="level"
                className="border-outline-variant text-secondary h-4 w-4"
                checked={selectedDifficulty === null}
                onChange={() => setSelectedDifficulty(null)}
              />
              <span className="text-body-sm text-on-surface-variant">전체</span>
            </label>
            {DIFFICULTY_OPTIONS.map((option) => (
              <label
                key={option.value}
                className="hover:bg-surface-container flex cursor-pointer items-center gap-2 rounded p-1 transition-colors"
              >
                <input
                  type="radio"
                  name="level"
                  className="border-outline-variant text-secondary h-4 w-4"
                  checked={selectedDifficulty === option.value}
                  onChange={() => setSelectedDifficulty(option.value)}
                />
                <span className="text-body-sm text-on-surface-variant">{option.label}</span>
              </label>
            ))}
          </div>
        </Card>
      </aside>

      {/* 강좌 그리드 */}
      <section className="col-span-1 md:col-span-9">
        <div className="mb-stack-md relative">
          <MaterialIcon
            name="search"
            className="text-outline pointer-events-none absolute top-1/2 left-3 -translate-y-1/2"
          />
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Search courses..."
            className="border-outline-variant bg-surface-container-lowest text-body-sm text-on-surface focus:border-secondary focus:ring-secondary h-10 w-full rounded-lg border pr-4 pl-10 focus:ring-1 focus:outline-none"
          />
        </div>

        <div className="mb-stack-lg flex items-center justify-between">
          <h1 className="text-headline-lg-mobile text-primary md:text-headline-lg">
            강좌 목록
          </h1>
          {/* <div className="gap-stack-sm flex items-center">
            <span className="text-body-sm text-on-surface-variant">Sort by:</span>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as SortOption)}
              className="border-outline-variant bg-surface-container-lowest text-body-sm text-on-surface focus:border-secondary focus:ring-secondary rounded-lg border py-1 pr-8 pl-2 focus:ring-1 focus:outline-none"
            >
              <option value="recommended">추천순</option>
              <option value="title">제목순</option>
              <option value="duration">학습시간순</option>
            </select>
          </div> */}
        </div>

        {isLoading && <p className="text-body-sm text-slate-text">불러오는 중...</p>}
        {error && <p className="text-body-sm text-error-red">{error}</p>}
        {!isLoading && !error && visibleCourses.length === 0 && (
          <p className="text-body-sm text-slate-text">조건에 맞는 강좌가 없습니다.</p>
        )}

        <div className="gap-gutter grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3">
          {visibleCourses.map((course) => (
            <Card
              key={course.courseId}
              className="group flex cursor-pointer flex-col gap-2 transition-shadow hover:shadow-[0_2px_12px_rgba(0,0,0,0.12)]"
              onClick={() => router.push(`/courses/${course.courseId}`)}
            >
              {/* eslint-disable-next-line @next/next/no-img-element -- 시드 썸네일 URL이 임의 외부 호스트라 next/image remotePatterns 설정 대상이 아님 */}
              <img
                src={course.thumbnailUrl ?? THUMBNAIL_FALLBACK}
                alt=""
                className="aspect-video w-full rounded-t-lg object-cover"
                onError={(e) => {
                  e.currentTarget.onerror = null;
                  e.currentTarget.src = THUMBNAIL_FALLBACK;
                }}
              />
              <div className="p-stack-md flex flex-col gap-2 pt-0">
                <h3 className="text-headline-sm text-primary group-hover:text-secondary line-clamp-2 transition-colors">
                  {course.title}
                </h3>
                <div className="flex flex-wrap gap-2">
                  <Chip tone="primary">{CATEGORY_LABELS[course.category] ?? course.category}</Chip>
                  <Chip>{DIFFICULTY_LABELS[course.difficulty] ?? course.difficulty}</Chip>
                </div>
                <span className="text-body-sm text-slate-text">{course.durationMinutes}분</span>
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* 커리큘럼 추천 FAB — design/course/course-list의 auto_awesome FAB */}
      <button
        type="button"
        onClick={() => router.push("/curriculum-recommendation")}
        className="bg-secondary text-label-md text-on-secondary hover:bg-secondary-container fixed right-6 bottom-6 flex items-center gap-2 rounded-full px-5 py-3 shadow-[0_12px_24px_rgba(0,0,0,0.12)] transition-colors"
      >
        <MaterialIcon name="auto_awesome" />
        커리큘럼 추천
      </button>
    </div>
  );
}
