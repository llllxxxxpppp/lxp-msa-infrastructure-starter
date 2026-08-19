"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { CurriculumChat } from "@/components/curriculum/CurriculumChat";
import { Button } from "@/components/ui/Button";
import { MaterialIcon } from "@/components/ui/MaterialIcon";
import { createMockChatClient } from "@/features/curriculum/mockClient";
import type { ChatStatus } from "@/features/curriculum/types";

/**
 * design/curriculum-recommand 화면.
 *
 * MOCK: 커리큘럼 추천봇(`curriculum-service`)이 게이트웨이에 연결돼 있지 않아 목 클라이언트로
 * 동작한다. 봇 라우트가 생기면 `createMockChatClient()` 자리에 실제 ChatClient 구현을 넣으면
 * 되고, 컴포넌트는 손대지 않는다.
 */
export default function CurriculumRecommendationPage() {
  const router = useRouter();
  const [status, setStatus] = useState<ChatStatus>("interviewing");

  // 클라이언트는 한 번만 만든다. 렌더마다 새로 만들면 대화가 첫 턴으로 돌아간다.
  const [client] = useState(createMockChatClient);

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

      <CurriculumChat client={client} onStatusChange={setStatus} />

      {status === "completed" && (
        <Button onClick={() => router.push("/courses")}>강좌 목록에서 시작하기</Button>
      )}
    </div>
  );
}
