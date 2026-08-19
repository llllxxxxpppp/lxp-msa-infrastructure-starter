interface MaterialIconProps {
  /** Material Symbols 아이콘 이름 (예: "mail", "lock", "arrow_forward"). */
  name: string;
  className?: string;
}

/** `<span class="material-symbols-outlined">` 래퍼. globals.css의 font-variation-settings를 사용한다. */
export function MaterialIcon({ name, className = "" }: MaterialIconProps) {
  return <span className={`material-symbols-outlined ${className}`}>{name}</span>;
}
