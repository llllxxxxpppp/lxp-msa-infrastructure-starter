import { useEffect, useRef } from "react";
import { ErrorNotice } from "./ErrorNotice";
import { MessageItem } from "./MessageItem";
import { TypingIndicator } from "./TypingIndicator";
import type { ChatMessage } from "@/features/curriculum/hooks";

interface MessageListProps {
  messages: ChatMessage[];
  isSending: boolean;
  error: string | null;
  onRetry: () => void;
}

export function MessageList({ messages, isSending, error, onRetry }: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  // 새 메시지·로딩·오류가 붙을 때마다 맨 아래로 따라간다.
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: "end" });
  }, [messages, isSending, error]);

  return (
    <>
      <ol className="flex flex-col space-y-6">
        {messages.map((message) => (
          <MessageItem key={message.id} message={message} />
        ))}
        {isSending && <TypingIndicator />}
        {error && <ErrorNotice message={error} onRetry={onRetry} disabled={isSending} />}
      </ol>
      <div ref={bottomRef} />
    </>
  );
}
