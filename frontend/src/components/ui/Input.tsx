import { InputHTMLAttributes, ReactNode } from "react";
import { MaterialIcon } from "./MaterialIcon";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  /** 좌측에 표시할 Material Symbols 아이콘 이름 (design/login처럼 mail/lock 등). */
  icon?: string;
  /** 우측에 표시할 임의 요소 (예: 비밀번호 표시/숨김 토글 버튼). */
  trailing?: ReactNode;
}

export function Input({ label, id, icon, trailing, className = "", ...props }: InputProps) {
  return (
    <div className="gap-base flex flex-col">
      {label && (
        <label htmlFor={id} className="text-label-md text-primary">
          {label}
        </label>
      )}
      <div className="relative">
        {icon && (
          <MaterialIcon
            name={icon}
            className="text-slate-text pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-[20px]"
          />
        )}
        <input
          id={id}
          className={`border-outline-variant bg-surface-container-lowest text-body-md text-on-surface placeholder:text-slate-text/70 focus:border-secondary w-full rounded border py-2 transition-all duration-200 focus:shadow-[0_0_0_2px_rgba(4,83,205,0.2)] focus:outline-none ${icon ? "pl-10" : "pl-4"} ${trailing ? "pr-10" : "pr-4"} ${className}`}
          {...props}
        />
        {trailing && (
          <div className="text-slate-text absolute top-1/2 right-3 -translate-y-1/2">
            {trailing}
          </div>
        )}
      </div>
    </div>
  );
}
