import { HTMLAttributes } from "react";

/**
 * Level 1 elevation(흰 표면) 카드. DESIGN.md Elevation & Depth 참고.
 * 패딩은 의도적으로 넣지 않는다(썸네일이 카드 가장자리까지 차야 하는 강좌 카드처럼 케이스가
 * 갈리므로) — 필요하면 사용하는 쪽에서 `p-stack-md`/`p-stack-lg`를 직접 준다.
 */
export function Card({ className = "", ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`border-outline-variant bg-surface-container-lowest rounded-lg border shadow-[0_2px_12px_rgba(0,0,0,0.08)] ${className}`}
      {...props}
    />
  );
}
