"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { MaterialIcon } from "@/components/ui/MaterialIcon";

/**
 * design/curriculum-recommand 정적 목업.
 *
 * ⚠️ 실제 AI 커리큘럼 추천 백엔드가 리포에 없다. 아래 3단계 시나리오는 디자인 export의 대화
 * 내용을 그대로 하드코딩한 스크립트이며, 버튼 클릭으로 다음 단계로만 진행한다(실 API 호출 없음).
 */

const CURRICULUM_STEPS = [
  {
    title: "웹과 REST API 기초",
    meta: "5시간 · 백엔드 개발",
    desc: "본격적으로 들어가기 전에 기본 개념을 정리하는 단계입니다.",
  },
  {
    title: "FastAPI 실전 프로젝트",
    meta: "10시간 · 백엔드 개발",
    desc: "배운 내용을 실제로 만들어 보며 손에 익히는 단계입니다.",
  },
  {
    title: "확장 가능한 백엔드 아키텍처",
    meta: "12시간 · 백엔드 개발",
    desc: "앞 단계를 확장해 혼자서도 문제를 풀 수 있게 하는 단계입니다.",
  },
];

const SHORTENED_STEPS = [
  {
    title: "웹과 REST API 기초",
    meta: "5시간 · 백엔드 개발",
    desc: "이미 아시는 부분은 건너뛰고 필요한 절만 골라 들으셔도 됩니다.",
  },
  {
    title: "FastAPI 실전 프로젝트",
    meta: "10시간 · 백엔드 개발",
    desc: "전체를 다 듣기보다 프로젝트 파트 위주로 보시면 시간을 줄일 수 있습니다.",
  },
  {
    title: "확장 가능한 백엔드 아키텍처",
    meta: "12시간 · 백엔드 개발",
    desc: "앞 두 단계를 마친 뒤에 시작하셔도 늦지 않습니다.",
  },
];

type Step = "intro" | "proposal" | "shortened" | "confirmed";

export default function CurriculumRecommendationPage() {
  const router = useRouter();
  const [step, setStep] = useState<Step>("intro");
  const [role, setRole] = useState("");

  return (
    <div className="gap-stack-lg mx-auto flex max-w-2xl flex-col">
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

      <AssistantBubble>
        안녕하세요. 몇 가지 여쭤보고 입문·실전·심화 3단계 커리큘럼을 제안해 드릴게요. 먼저 어떤
        직무를 맡고 계신가요?
      </AssistantBubble>

      {step === "intro" && (
        <form
          className="flex gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            if (role.trim()) setStep("proposal");
          }}
        >
          <input
            value={role}
            onChange={(e) => setRole(e.target.value)}
            placeholder="예: 백엔드 개발"
            className="border-outline-variant bg-surface-container-lowest text-body-sm text-on-surface focus:border-secondary focus:ring-secondary flex-1 rounded-xl border px-4 py-3 focus:ring-2 focus:outline-none"
          />
          <Button type="submit">전송</Button>
        </form>
      )}

      {(step === "proposal" || step === "shortened" || step === "confirmed") && (
        <>
          <UserBubble>{role || "백엔드 개발"}</UserBubble>

          <AssistantBubble>
            {step === "shortened"
              ? "같은 분야에서 더 짧은 대안이 없어 강의는 그대로 두고, 단계마다 시간을 줄이는 방법을 적었습니다."
              : "백엔드 개발 분야에서 기초를 다지고 실제로 만들어 본 뒤 확장까지 이어지는 3단계로 구성했습니다."}
          </AssistantBubble>

          <div className="gap-stack-sm flex flex-col">
            {(step === "shortened" ? SHORTENED_STEPS : CURRICULUM_STEPS).map((item, index) => (
              <Card key={item.title} className="gap-stack-md p-stack-md flex">
                <div className="bg-secondary text-label-sm text-on-secondary flex h-8 w-8 shrink-0 items-center justify-center rounded-full">
                  {index + 1}
                </div>
                <div>
                  <p className="text-label-md text-on-surface">{item.title}</p>
                  <p className="text-label-sm text-on-surface-variant mb-2">{item.meta}</p>
                  <p className="text-body-sm text-on-surface">{item.desc}</p>
                </div>
              </Card>
            ))}
          </div>

          {step !== "confirmed" && (
            <>
              <AssistantBubble>
                이 커리큘럼이 괜찮으신가요? 바꾸고 싶은 부분이 있다면 말씀해 주세요.
              </AssistantBubble>
              <div className="gap-stack-sm flex">
                <Button onClick={() => setStep("confirmed")}>괜찮아요, 확정할게요</Button>
                {step === "proposal" && (
                  <Button variant="secondary" onClick={() => setStep("shortened")}>
                    더 짧게 조정해주세요
                  </Button>
                )}
              </div>
            </>
          )}

          {step === "confirmed" && (
            <Card className="p-stack-lg">
              <p className="mb-stack-md text-label-md text-primary">
                확정된 학습 로드맵 · 총 27시간
              </p>
              <ul className="gap-stack-sm flex flex-col">
                {(step === "confirmed" ? SHORTENED_STEPS : CURRICULUM_STEPS).map((item) => (
                  <li key={item.title} className="text-body-sm text-on-surface">
                    {item.title} · {item.meta.split(" · ")[0]}
                  </li>
                ))}
              </ul>
              <p className="mt-stack-md text-body-sm text-primary">
                좋습니다. 이 커리큘럼을 최종 학습 로드맵으로 확정하겠습니다.
              </p>
              <Button className="mt-stack-md" onClick={() => router.push("/courses")}>
                강좌 목록에서 시작하기
              </Button>
            </Card>
          )}
        </>
      )}
    </div>
  );
}

function AssistantBubble({ children }: { children: React.ReactNode }) {
  return (
    <div className="bg-surface-container-lowest p-stack-md text-body-sm text-on-surface max-w-[85%] rounded-xl rounded-tl-none shadow-sm">
      {children}
    </div>
  );
}

function UserBubble({ children }: { children: React.ReactNode }) {
  return (
    <div className="bg-secondary p-stack-md text-body-sm text-on-secondary ml-auto max-w-[85%] rounded-xl rounded-tr-none">
      {children}
    </div>
  );
}
