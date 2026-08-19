interface AvatarProps {
  /** 아바타에 표시할 이름/이메일 — 첫 글자를 이니셜로 뽑는다. 실제 프로필 이미지 API가 없어 이니셜만 지원한다. */
  label: string;
  className?: string;
}

export function Avatar({ label, className = "" }: AvatarProps) {
  const initial = label.trim().charAt(0).toUpperCase() || "?";
  return (
    <div
      className={`bg-primary-container text-label-md text-on-primary-container flex h-10 w-10 items-center justify-center rounded-full ${className}`}
      aria-hidden="true"
    >
      {initial}
    </div>
  );
}
