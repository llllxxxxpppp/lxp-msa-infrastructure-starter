import { HTMLAttributes } from "react";

type Tone = "neutral" | "primary" | "success" | "warning" | "error";

/** 강좌 카테고리/난이도, 상태 태그 등에 쓰는 pill 모양 라벨. DESIGN.md Chips 참고. */
const TONE_CLASSES: Record<Tone, string> = {
  neutral: "bg-surface-container text-on-surface-variant",
  primary: "bg-primary-fixed text-on-primary-fixed",
  success: "bg-success-green/15 text-success-green",
  warning: "bg-warning-amber/15 text-warning-amber",
  error: "bg-error-red/15 text-error-red",
};

interface ChipProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: Tone;
}

export function Chip({ tone = "neutral", className = "", ...props }: ChipProps) {
  return (
    <span
      className={`text-label-sm inline-block rounded-full px-3 py-1 ${TONE_CLASSES[tone]} ${className}`}
      {...props}
    />
  );
}
