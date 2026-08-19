"use client";

import { useCallback, useEffect, useRef, useState } from "react";
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
 * 봇의 POST /api/curriculum/chat 은 message 가 필수라 사용자가 먼저 말을 걸어야 한다.
 * 빈 화면으로 시작하면 무엇을 입력해야 할지 알 수 없으므로 첫 인사는 프론트가 심는다.
 */
export const GREETING =
  "안녕하세요. 몇 가지 여쭤보고 입문·실전·심화 3단계 커리큘럼을 제안해 드릴게요. 먼저 어떤 직무를 맡고 계신가요?";

export interface UseCurriculumChat {
  messages: ChatMessage[];
  status: ChatStatus;
  isReady: boolean;
  isInitializing: boolean;
  isSending: boolean;
  isWaiting: boolean;
  /** 초기화 또는 마지막 전송이 실패했을 때의 안내 문구. 성공하면 비워진다. */
  error: string | null;
  errorActionLabel: string;
  send: (text: string) => void;
  /** 실패한 초기화 또는 마지막 메시지 전송을 다시 시도한다. */
  retry: () => void;
}

function createId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
}

/** client는 렌더마다 같은 인스턴스여야 세션 초기화 effect가 반복되지 않는다. */
export function useCurriculumChat(client: ChatClient): UseCurriculumChat {
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    { id: createId(), role: "bot", text: GREETING, curriculum: null, status: null },
  ]);
  const [status, setStatus] = useState<ChatStatus>("interviewing");
  const [isReady, setIsReady] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [isWaiting, setIsWaiting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorActionLabel, setErrorActionLabel] = useState("다시 시도");

  // 실패한 메시지를 재전송하려면 원문을 들고 있어야 한다.
  const lastSentRef = useRef<string | null>(null);
  // 개발 모드의 effect 재실행에서도 세션 초기화 요청을 한 번만 보낸다.
  const initializationRef = useRef<Promise<void> | null>(null);
  const initializedRef = useRef(false);
  // 전송 중 여부는 다음 렌더를 기다리지 않고 즉시 막아야 해서 ref 로도 잠근다.
  const sendingRef = useRef(false);
  const activeRequestRef = useRef<AbortController | null>(null);

  const initialize = useCallback((): Promise<void> => {
    if (initializationRef.current) {
      return initializationRef.current;
    }

    initializedRef.current = false;
    setIsReady(false);
    setIsInitializing(true);
    setError(null);
    const initialization = (async () => {
      try {
        await Promise.resolve().then(() => client.reset());
        initializedRef.current = true;
        setIsReady(true);
      } catch (cause) {
        initializationRef.current = null;
        setError(cause instanceof Error ? cause.message : "새 추천 대화를 시작하지 못했습니다.");
        setErrorActionLabel("다시 시도");
      } finally {
        setIsInitializing(false);
      }
    })();
    initializationRef.current = initialization;
    return initialization;
  }, [client]);

  useEffect(() => {
    void initialize();
  }, [initialize]);

  useEffect(() => {
    return () => activeRequestRef.current?.abort();
  }, []);

  const request = useCallback(
    async (text: string) => {
      if (sendingRef.current) {
        return;
      }
      sendingRef.current = true;
      setIsSending(true);
      setIsWaiting(true);
      setError(null);
      const botMessageId = createId();
      const controller = new AbortController();
      activeRequestRef.current = controller;
      let botMessageAdded = false;

      try {
        for await (const event of client.stream({ message: text }, controller.signal)) {
          if (event.type === "metadata") {
            setStatus(event.data.status);
            setIsWaiting(false);
            botMessageAdded = true;
            setMessages((prev) => [
              ...prev,
              {
                id: botMessageId,
                role: "bot",
                text: "",
                curriculum: event.data.curriculum,
                status: event.data.status,
              },
            ]);
          } else if (event.type === "delta") {
            if (!botMessageAdded) {
              throw new Error("스트리밍 응답의 이벤트 순서가 올바르지 않습니다.");
            }
            setMessages((prev) =>
              prev.map((message) =>
                message.id === botMessageId
                  ? { ...message, text: message.text + event.content }
                  : message,
              ),
            );
          }
        }
        lastSentRef.current = null;
      } catch (cause) {
        if (controller.signal.aborted) {
          return;
        }
        if (botMessageAdded) {
          setMessages((prev) => prev.filter((message) => message.id !== botMessageId));
        }
        // 보낸 말은 화면에 남겨 둔다. 다시 보내기를 누르면 그대로 재전송한다.
        lastSentRef.current = text;
        setError(cause instanceof Error ? cause.message : "봇 응답을 받지 못했습니다.");
        setErrorActionLabel("다시 보내기");
      } finally {
        sendingRef.current = false;
        if (activeRequestRef.current === controller) {
          activeRequestRef.current = null;
        }
        if (!controller.signal.aborted) {
          setIsSending(false);
          setIsWaiting(false);
        }
      }
    },
    [client],
  );

  const send = useCallback(
    (text: string) => {
      const trimmed = text.trim();
      if (!trimmed || !initializedRef.current || sendingRef.current) {
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
      return;
    }
    void initialize();
  }, [initialize, request]);

  return {
    messages,
    status,
    isReady,
    isInitializing,
    isSending,
    isWaiting,
    error,
    errorActionLabel,
    send,
    retry,
  };
}
