import { CurriculumStepItem } from "./CurriculumStepItem";
import type { CurriculumPlan } from "@/features/curriculum/types";

interface CurriculumCardProps {
  plan: CurriculumPlan;
  /** 확정된 로드맵인지. 확정 뒤에는 훑어보기만 하면 되므로 사유와 카테고리를 뺀다. */
  confirmed: boolean;
}

/** "5시간" 같은 표기에서 숫자만 뽑는다. 봇이 시간 단위 문자열로만 내려준다. */
function hoursOf(duration: string): number {
  const parsed = Number.parseInt(duration, 10);
  return Number.isNaN(parsed) ? 0 : parsed;
}

export function CurriculumCard({ plan, confirmed }: CurriculumCardProps) {
  const totalHours = plan.steps.reduce((sum, step) => sum + hoursOf(step.duration), 0);

  if (confirmed) {
    return (
      <div className="border-outline-variant mb-6 overflow-hidden rounded-xl border">
        <div className="border-outline-variant bg-surface-container-low border-b p-4">
          <p className="text-primary text-sm font-medium">
            확정된 학습 로드맵 · 총 {totalHours}시간
          </p>
        </div>
        <ol>
          {plan.steps.map((step) => (
            <CurriculumStepItem key={step.course_id} step={step} confirmed />
          ))}
        </ol>
      </div>
    );
  }

  return (
    <>
      <p className="text-on-surface-variant mb-6 text-sm">{plan.summary}</p>
      <ol className="border-outline-variant mb-6 overflow-hidden rounded-xl border">
        {plan.steps.map((step) => (
          <CurriculumStepItem key={step.course_id} step={step} confirmed={false} />
        ))}
      </ol>
    </>
  );
}
