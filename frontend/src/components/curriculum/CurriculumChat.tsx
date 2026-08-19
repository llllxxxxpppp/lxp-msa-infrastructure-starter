"use client";

import { useEffect } from "react";
import { MessageInput } from "./MessageInput";
import { MessageList } from "./MessageList";
import { useCurriculumChat } from "@/features/curriculum/hooks";
import type { ChatClient, ChatStatus } from "@/features/curriculum/types";

interface CurriculumChatProps {
  /**
   * 봇과 통신하는 방법. 렌더마다 같은 인스턴스를 넘겨야 대화가 이어진다.
   * 이 prop 하나가 목과 실제 봇을 가르는 지점이다.
   */
  client: ChatClient;
  /** 확정 뒤에 CTA를 붙이는 등 페이지가 상태를 알아야 할 때 쓴다. */
  onStatusChange?: (status: ChatStatus) => void;
}

/**
 * 커리큘럼 추천 채팅.
 *
 * 제목과 뒤로가기는 페이지가 갖고 있으므로 여기서는 대화와 입력창만 그린다.
 */
export function CurriculumChat({ client, onStatusChange }: CurriculumChatProps) {
  const { messages, status, isSending, error, send, retry } = useCurriculumChat(client);

  useEffect(() => {
    onStatusChange?.(status);
  }, [status, onStatusChange]);

  return (
    <div className="flex w-full flex-col">
      <MessageList messages={messages} isSending={isSending} error={error} onRetry={retry} />
      <MessageInput onSend={send} disabled={isSending} />
    </div>
  );
}
