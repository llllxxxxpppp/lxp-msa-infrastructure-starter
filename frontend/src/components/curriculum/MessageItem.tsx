import { CurriculumCard } from "./CurriculumCard";
import type { ChatMessage } from "@/features/curriculum/hooks";

interface MessageItemProps {
  message: ChatMessage;
}

export function MessageItem({ message }: MessageItemProps) {
  if (message.role === "user") {
    return (
      <li className="flex justify-end">
        <div className="bg-surface-container border-outline-variant max-w-[80%] rounded-2xl rounded-tr-sm border p-4 text-sm shadow-sm">
          {message.text}
        </div>
      </li>
    );
  }

  // 커리큘럼이 실린 턴은 카드와 마무리 문장을 한 말풍선에 담고 폭을 넓게 쓴다.
  const plan = message.curriculum;

  return (
    <li className={`flex flex-col ${plan ? "max-w-full" : "max-w-[85%]"}`}>
      <div
        className={`bg-surface-container-lowest border-outline-variant rounded-2xl rounded-tl-sm border shadow-sm ${
          plan ? "p-6" : "p-4 text-sm leading-relaxed"
        }`}
      >
        {plan && <CurriculumCard plan={plan} confirmed={message.status === "completed"} />}
        {message.text && (
          <p className={plan ? "text-primary text-sm font-medium" : undefined}>{message.text}</p>
        )}
      </div>
    </li>
  );
}
