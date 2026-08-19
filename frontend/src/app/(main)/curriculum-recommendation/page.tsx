"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { CurriculumChat } from "@/components/curriculum/CurriculumChat";
import { Button } from "@/components/ui/Button";
import { MaterialIcon } from "@/components/ui/MaterialIcon";
import { curriculumChatClient } from "@/features/curriculum/api";
import type { ChatStatus } from "@/features/curriculum/types";

/** design/curriculum-recommand 화면. */
export default function CurriculumRecommendationPage() {
  const router = useRouter();
  const [status, setStatus] = useState<ChatStatus>("interviewing");

  return (
    <div className="gap-stack-lg mx-auto flex max-w-3xl flex-col">
      <div className="gap-stack-sm flex items-center">
        <button
          type="button"
          onClick={() => router.push("/courses")}
          className="text-body-sm text-slate-text hover:text-secondary flex items-center gap-1"
        >
          <MaterialIcon name="arrow_back" className="text-[18px]" />
          강좌 목록으로
        </button>
      </div>

      <h1 className="text-headline-lg text-primary flex items-center gap-2">
        <MaterialIcon name="auto_awesome" className="text-secondary" />
        커리큘럼 추천
      </h1>

      <CurriculumChat client={curriculumChatClient} onStatusChange={setStatus} />

      {status === "completed" && (
        <Button onClick={() => router.push("/courses")}>강좌 목록에서 시작하기</Button>
      )}
    </div>
  );
}
