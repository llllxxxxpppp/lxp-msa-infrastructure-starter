interface ProgressBarProps {
  /** 0~100 사이 진행률. */
  value: number;
  className?: string;
}

/** DESIGN.md Progress Indicators: 활성 진행은 Trust Blue, 트랙은 Deep Navy 10% 투명도. */
export function ProgressBar({ value, className = "" }: ProgressBarProps) {
  const clamped = Math.min(100, Math.max(0, value));
  return (
    <div
      className={`bg-primary/10 h-2 w-full overflow-hidden rounded-full ${className}`}
      role="progressbar"
      aria-valuenow={clamped}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <div
        className="bg-secondary h-full rounded-full transition-all"
        style={{ width: `${clamped}%` }}
      />
    </div>
  );
}
