import type { CurriculumStep } from "@/features/curriculum/types";

interface CurriculumStepItemProps {
  step: CurriculumStep;
  /** 확정된 카드에서는 강좌명과 소요 시간만 한 줄로 보여준다. */
  confirmed: boolean;
}

export function CurriculumStepItem({ step, confirmed }: CurriculumStepItemProps) {
  if (confirmed) {
    return (
      <li className="border-outline-variant bg-surface-container-lowest flex items-center border-b px-4 py-3 last:border-b-0">
        <span className="border-outline text-on-surface-variant bg-surface mr-3 inline-flex w-10 shrink-0 items-center justify-center rounded border px-2 py-1 text-[10px] font-medium">
          {step.stage}
        </span>
        <p className="text-on-surface text-sm font-medium">
          {step.title} · {step.duration_minutes}분
        </p>
      </li>
    );
  }

  return (
    <li className="border-outline-variant bg-surface-container-lowest flex items-start border-b p-5 last:border-b-0">
      <span className="border-outline text-on-surface-variant bg-surface mr-4 inline-flex shrink-0 items-center justify-center rounded-md border px-3 py-1 text-xs font-medium">
        {step.stage}
      </span>
      <div>
        <h4 className="text-primary mb-1 text-base font-semibold">{step.title}</h4>
        <p className="text-on-surface-variant mb-2 text-xs">{step.duration_minutes}분</p>
        <p className="text-on-surface text-sm">{step.reason}</p>
      </div>
    </li>
  );
}
