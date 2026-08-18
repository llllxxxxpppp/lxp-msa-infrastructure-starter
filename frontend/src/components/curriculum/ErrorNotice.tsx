import { Button } from "@/components/ui/Button";

interface ErrorNoticeProps {
  message: string;
  onRetry: () => void;
  disabled: boolean;
}

/** 디자인 export에 오류 상태가 없어 error 토큰과 공용 Button으로 구성했다. */
export function ErrorNotice({ message, onRetry, disabled }: ErrorNoticeProps) {
  return (
    <li className="border-error-container bg-error-container flex flex-wrap items-center justify-between gap-3 rounded-xl border p-4">
      <p className="text-on-error-container text-sm" role="alert">
        {message} 다시 보내시겠어요?
      </p>
      <Button variant="secondary" onClick={onRetry} disabled={disabled}>
        다시 보내기
      </Button>
    </li>
  );
}
