"use client";

import { useCallback, useRef, useState } from "react";
import type { ChatClient, ChatStatus, CurriculumPlan } from "./types";

/*
 * 화면에 그릴 메시지 한 줄. 봇 응답과 1:1 이 아니라 사용자가 보낸 말도 같은 배열에 담는다.
 * 봇 턴에 커리큘럼이 실려 오면 그 메시지에 붙여 카드로 렌더링한다.
 */
export interface ChatMessage {
  id: string;
  role: "user" | "bot";
  text: string;
  curriculum: CurriculumPlan | null;
  /** 이 턴의 봇 상태. 커리큘럼 카드를 검토용으로 그릴지 확정용으로 그릴지 가른다. */
  status: ChatStatus | null;
}

/*
 * 봇의 POST /chat 은 message 가 필수라 사용자가 먼저 말을 걸어야 한다.
 * 빈 화면으로 시작하면 무엇을 입력해야 할지 알 수 없으므로 첫 인사는 프론트가 심는다.
 */
export const GREETING =
  "안녕하세요. 몇 가지 여쭤보고 입문·실전·심화 3단계 커리큘럼을 제안해 드릴게요. 먼저 어떤 직무를 맡고 계신가요?";

export interface UseCurriculumChat {
  messages: ChatMessage[];
  status: ChatStatus;
  isSending: boolean;
  /** 마지막 전송이 실패했을 때의 안내 문구. 성공하면 비워진다. */
  error: string | null;
  send: (text: string) => void;
  /** 실패한 마지막 메시지를 그대로 다시 보낸다. */
  retry: () => void;
}

function createId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
}

/**
 * client 는 렌더마다 같은 인스턴스여야 한다. 목 클라이언트는 대화 상태를 자기 안에
 * 들고 있어서, 렌더마다 새로 만들면 매번 첫 턴으로 돌아간다.
 */
export function useCurriculumChat(client: ChatClient): UseCurriculumChat {
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    { id: createId(), role: "bot", text: GREETING, curriculum: null, status: null },
  ]);
  const [status, setStatus] = useState<ChatStatus>("interviewing");
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 세션당 한 번만 만들어 대화 내내 고정한다. 지연 초기화라 렌더마다 새로 생기지 않는다.
  const [threadId] = useState(createId);

  // 실패한 메시지를 재전송하려면 원문을 들고 있어야 한다.
  const lastSentRef = useRef<string | null>(null);
  // 전송 중 여부는 다음 렌더를 기다리지 않고 즉시 막아야 해서 ref 로도 잠근다.
  const sendingRef = useRef(false);

  const request = useCallback(
    async (text: string) => {
      if (sendingRef.current) {
        return;
      }
      sendingRef.current = true;
      setIsSending(true);
      setError(null);

      try {
        const response = await client.send({
          thread_id: threadId,
          message: text,
        });
        setStatus(response.status);
        setMessages((prev) => [
          ...prev,
          {
            id: createId(),
            role: "bot",
            text: response.message,
            curriculum: response.curriculum,
            status: response.status,
          },
        ]);
        lastSentRef.current = null;
      } catch (cause) {
        // 보낸 말은 화면에 남겨 둔다. 다시 보내기를 누르면 그대로 재전송한다.
        lastSentRef.current = text;
        setError(cause instanceof Error ? cause.message : "봇 응답을 받지 못했습니다.");
      } finally {
        sendingRef.current = false;
        setIsSending(false);
      }
    },
    [client, threadId],
  );

  const send = useCallback(
    (text: string) => {
      const trimmed = text.trim();
      if (!trimmed || sendingRef.current) {
        return;
      }
      setMessages((prev) => [
        ...prev,
        { id: createId(), role: "user", text: trimmed, curriculum: null, status: null },
      ]);
      void request(trimmed);
    },
    [request],
  );

  const retry = useCallback(() => {
    const pending = lastSentRef.current;
    if (pending) {
      void request(pending);
    }
  }, [request]);

  return { messages, status, isSending, error, send, retry };
}
