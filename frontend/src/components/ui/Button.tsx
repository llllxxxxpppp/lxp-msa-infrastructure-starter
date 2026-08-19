import { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "secondary" | "ghost" | "danger";

/**
 * DESIGN.md의 버튼 규칙(Trust Blue 배경/보더/텍스트) + 실제 code.html이 쓰는 rounded-lg 반경.
 * danger는 DESIGN.md 명세엔 없지만 탈퇴/구독취소 같은 파괴적 액션에 필요해 error 색상으로 추가했다.
 */
const VARIANT_CLASSES: Record<Variant, string> = {
  primary: "bg-secondary text-on-secondary hover:bg-secondary-container disabled:opacity-50",
  secondary:
    "bg-transparent text-secondary border border-secondary hover:bg-secondary/5 disabled:opacity-50",
  ghost: "bg-transparent text-primary hover:bg-surface-container disabled:opacity-50",
  danger: "bg-error text-on-error hover:opacity-90 disabled:opacity-50",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
}

export function Button({ variant = "primary", className = "", ...props }: ButtonProps) {
  return (
    <button
      className={`text-label-md inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 shadow-sm transition-colors duration-200 disabled:cursor-not-allowed disabled:shadow-none ${VARIANT_CLASSES[variant]} ${className}`}
      {...props}
    />
  );
}
